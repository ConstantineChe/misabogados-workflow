(ns misabogados-workflow.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.utils :as u]
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
  ([uri title page]
   [:li {:key title
         :class (when (= page (session/get :page)) "active")}
    [:a
     {:href uri
      :on-click #(-> 
                  (u/jquery "#navbar-hamburger:visible")
                  (.click))}
     title]])
  ([uri title]
   [:li>a
    {:href uri
      :on-click #(-> 
                  (u/jquery "#navbar-hamburger:visible")
                  (.click))} title]))

(defn navbar []
  
  (fn []
    [:nav.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:button#navbar-hamburger.navbar-toggle.collapsed {:data-toggle "collapse" :data-target "#navbar-body" :aria-expanded "false"}
        [:span.sr-only "Toggle navigation"] [:span.icon-bar][:span.icon-bar][:span.icon-bar]]
       [:a.navbar-brand {:href "#/"} "Misabogados Workflow"]]
      
      [:div#navbar-body.navbar-collapse.collapse
       
       (into [:ul.nav.navbar-nav]
             (doall (map (fn [item]  (apply nav-link item)) (:nav-links @ac/components)))
             )]]]))

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

(secretary/defroute "/logout" []
  (do (logout!)
      (session/put! :page :home)))

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
