(ns misabogados-workflow.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST ajax-request]]
            [misabogados-workflow.access-controll :as ac])
  (:import goog.History))

(def user (r/atom {}))

(defn handler [response]
  (.log js/console response))

(defn https? []
  (= "https:" (.-protocol js/location)))


(def csrf-token (r/atom nil))

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
                                     :headers {:X-CSRF-Token  @csrf-token}
                                     :handler (fn [response] (.log js/console (str response))
                                                (if (= (get response "status") "ok")
                                                  (do (session/put! :user {:identity (get response "identity")
                                                                           :role (get response "role")})
                                                      (ac/reset-access!)
                                                      (aset js/window "location" "#/dashboard"))
                                                  (reset! error (get response "error")))
                                                nil)
                                     :error-handler (fn [response]
                                                      (.log js/console response)
                                                      (reset! error "Invalid anti-forgery token")
                                                      nil)} ))))

(defn logout! []
  (GET (str js/context "/logout") {:handler (fn [response] (session/put! :user {}) (ac/reset-access!) nil)}))

(defn logged-in? [] (not (empty? (session/get :user))))



(defn update-csrf-token []
  (GET (str js/context "/csrf-token") {:handler #(reset! csrf-token (get % "token"))}))


(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav {:key title}
   [:a.navbar-brand
    {:class (when (= page (session/get :page)) "active")

     :href uri
     :on-click #(reset! collapsed? true)}
    title]])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "misabogados-workflow"]
        (conj [:ul.nav.navbar-nav
           (if-not (logged-in?)
             [nav-link "#/login" "Login" :login collapsed?]
             [:ul.nav.navbar-nav>a.navbar-brand
              {:on-click #(logout!)} "Logout"])]
              (doall (map (fn [item]  (apply nav-link (conj item collapsed?))) (:nav-links @ac/components)))
              )]])))

(defn debug []
  (let [request (r/atom nil)
        _ (GET (str js/context "/request") {:handler (fn [resp]
                                                       (do (.log js/console resp)
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
        _ (update-csrf-token)]
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

(defn dashboard []
  (let [users (r/atom {})
        error (r/atom nil)
        _ (GET (str js/context "/users")
               {:handler (fn [response]
                           (reset! users response) nil)
                :error-handler (fn [response] (reset! error (get "error" response)) nil)})]
    (fn []
      [:div.container [:legend "dashboard"]
           [:p "Role: " (:role (session/get :user))]
           (if-not (nil? @error) [:p.error (str @error)])
           (if-let [users (seq @users)]
             (for [user users]
               [:p (str user)]))])))

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
   :dashboard #'dashboard})

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
                   nil)})
  (ac/reset-access!))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (get-session!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
