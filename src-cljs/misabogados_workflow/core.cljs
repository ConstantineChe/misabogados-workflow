(ns misabogados-workflow.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST ajax-request]])
  (:import goog.History))

(def user (r/atom {}))

(defn handler [response]
  (.log js/console (str response)))


(defn login! [email password error] 
  (cond
   (empty? email)
   (reset! error "Please enter email")
   (empty? password)
   (reset! error "Please enter password")
   :else
   (do
    (reset! error nil)
    (GET (str js/context "/login1") {:handler handler} ))))

(defn logged-in? [] (not (empty? @user)))




(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav>a.navbar-brand
   {:class (when (= page (session/get :page)) "active")
    :href uri
    :on-click #(reset! collapsed? true)}
   title])

(defn not-logged-in-menu []
  [nav-link "#/login" "Login" :login])


(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "misabogados-workflow"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]
         (if-not (logged-in?)
           (not-logged-in-menu))]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of misabogados-workflow... work in progress"]]])

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        error (r/atom nil)]
    (fn []
      [:div.login-form
       (if-let [error @error] [:p.error error])
       [:input {:type :email
                :value @email
                :on-change #(reset! email (-> %  .-target .-value))
                }]
       [:input {:type :password
                :value @password
                :on-change #(reset! password (-> %  .-target .-value))
                }]
       [:button {:on-click #(login! @email @password error)} "Login"]])))

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
   :login #'login-page})

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

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
