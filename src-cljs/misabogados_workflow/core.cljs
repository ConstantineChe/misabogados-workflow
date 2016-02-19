(ns misabogados-workflow.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.dashboard :refer [dashboard]]
            [misabogados-workflow.payments :refer [payments]]
            [misabogados-workflow.ajax :refer [GET POST csrf-token update-csrf-token!]])
  (:import goog.History))

(defn https? []
  (= "https:" (.-protocol js/location)))

(defn login! [email password error]
  (cond
   (empty? email)
   (reset! error "Please enter email")
   (empty? password)
   (reset! error "Please enter password")
   :else
   (do
    (reset! error nil)
    (POST (str js/context "/login") {:params {:email email
                                              :password password}
                                     :handler (fn [response]
                                                (if (= (get response "status") "ok")
                                                  (do (session/put! :user {:identity (get response "identity")
                                                                           :role (get response "role")})
                                                      (ac/reset-access!)
                                                      (aset js/window "location" "#/dashboard")
                                                      (update-csrf-token!))
                                                  (reset! error (get response "error")))
                                                nil)
                                     :error-handler (fn [response]
                                                      (reset! error "Invalid anti-forgery token")
                                                      nil)} ))))

(defn logout! []
  (GET (str js/context "/logout") {:handler (fn [response] (session/put! :user {})
                                              (ac/reset-access!)
                                              (update-csrf-token!)
                                              nil)}))

(defn logged-in? [] (not (empty? (session/get :user))))

(defn nav-link
  ([uri title page collapsed?]
   [:ul.nav.navbar-nav {:key title}
    [:a.navbar-brand
     {:class (when (= page (session/get :page)) "active")
      :href uri
      :on-click #(reset! collapsed? true)}
     title]])
  ([uri title collapsed?]
   [:ul.nav.navbar-nav>a.navbar-brand
    {:on-click #(do (swap! collapsed? not)
                    (logout!))
               :href uri} title]))

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "misabogados-workflow"]
        (into [:ul.nav.navbar-nav]
              (doall (map (fn [item]  (apply nav-link (conj item collapsed?))) (:nav-links @ac/components)))
              )]])))

(defn debug []
  (let [request (r/atom nil)
        _ (GET (str js/context "/request") {:handler (fn [resp]
                                                       (do
                                                           (reset! request (str resp)))
                                                       nil)})
        _ (ac/reset-access!)]
    (fn []
      [:div.container
       [:legend "Debug"]
       [:h3 "request"] [:p @request]
       [:h3 "ac"] [:p (str (ac/get-access))]
       [:h3 "session"] [:p (str (dissoc @session/state :docs))]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of misabogados-workflow... work in progress"]]])

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        error (r/atom nil)
        warnings (r/atom nil)
        _ (update-csrf-token!)]
    (fn []
      (if (not https?) (reset! warnings "Not using ssl"))
      [:div.login-form
       (if-let [error @error] [:p.error error])
       (if-let [warning @warnings] [:p.warning warning])
       [:input {:type :email
                :value @email
                :on-change #(reset! email (-> %  .-target .-value))
                }]
       [:input {:type :password
                :value @password
                :on-change #(reset! password (-> %  .-target .-value))
                }]
       [:button {:on-click #(login! @email @password error)} "Login"]]
      )))

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to misabogados-workflow"]
    [:p "Time to start building your site!"]
    [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to ClojureScript"]]]
   (when-let [docs (session/get :docs)]
     [:div.row
      [:div.col-md-12
       [:div {:dangerouslySetInnerHTML
              {:__html (md->html docs)}}]]])])

(def pages
  {:home #'home-page
   :about #'about-page
   :login #'login-page
   :debug #'debug
   :dashboard #'dashboard
   :payments #'payments})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/login" []
  (session/put! :page :login))

(secretary/defroute "/debug" []
  (session/put! :page :debug))

(secretary/defroute "/dashboard" []
  (session/put! :page :dashboard))

(secretary/defroute "/payments" []
  (session/put! :page :payments))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn get-session! []
  (GET (str js/context "/session")
       {:handler (fn [response]
                   (if-not (nil? (get response "identity"))
                     (session/put! :user {:identity (get response "identity" )
                                          :role (get response "role")}))
                   (ac/reset-access!)
                   nil)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (get-session!)
  (update-csrf-token!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
