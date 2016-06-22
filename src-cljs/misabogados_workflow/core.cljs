(ns misabogados-workflow.core
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [clojure.string :as str]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.admin :refer [admin]]
            [misabogados-workflow.ajax :refer [GET POST csrf-token update-csrf-token!]]
            [misabogados-workflow.dashboard :refer [dashboard]]
            [misabogados-workflow.payments :refer [payments]]
            [misabogados-workflow.lead :as lead]
            [misabogados-workflow.admin.categories :as categories]
            [misabogados-workflow.admin.lawyers :as lawyers]
            [misabogados-workflow.utils :as u :refer [get-session!]]
            [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljsjs.jquery]
            [cljsjs.bootstrap]
            [misabogados-workflow.websockets :as ws])
  (:import goog.History))

(defn https? []
  (= "https:" (.-protocol js/location)))

(defonce logged-in? (r/atom nil))

(defonce messages (r/atom []))

(defn signup! [signup-form]
  (POST (str js/context "/signup") {:params signup-form
                                    :handler #(do (session/put! :user {:identity (get % "identity")
                                                                       :role (get % "role")})
                                                  (ac/reset-access!)
                                                  (aset js/window "location" "#dashboard")
                                                  (update-csrf-token!))
                                    :error-handler #(js/alert (str "error: " %))}))

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
                                                  (do (get-session!
                                                       (fn [response]
                                                         (reset! logged-in? (nil? (get response "identity")))
                                                         (if-not @logged-in?
                                                           (session/put! :user {:identity (get response "identity" )
                                                                                :role (get response "role")}))
                                                         (session/put! :own-profile (get response "own-profile"))
                                                         (session/put! :filters {:payment-requests {:own-client true
                                                                                                    :misabogados-client true
                                                                                                    :status-pending true
                                                                                                    :status-in-process true
                                                                                                    :status-paid true
                                                                                                    :status-failed true}})
                                                         (ac/reset-access!)
                                                         (update-csrf-token!)
                                                         (case (session/get-in [:user :role])
                                                           "lawyer" (u/redirect "#payments")
                                                           nil (u/redirect "#login")
                                                           (u/redirect "#dashboard"))
                                                         nil)))
                                                  (reset! error (get response "error")))
                                                nil)
                                     :error-handler (fn [response]
                                                      (reset! error "Error")
                                                      nil)} ))))

(defn logout! []
  (GET (str js/context "/logout") {:handler (fn [response] (session/put! :user {})
                                              (ac/reset-access!)
                                              (update-csrf-token!)
                                              (u/redirect "#login")
                                              nil)}))


(defn nav-link
  ([uri title page]
   [:li {:key title
         :class (when (= page (session/get :page)) "active")}
    [:a
     {:href uri
      :on-click #(->
                  (js/jQuery "#navbar-hamburger:visible")
                  (.click))}
     title]])
  ([uri title]
   [:li>a
    {:href uri
      :on-click #(->
                  (js/jQuery "#navbar-hamburger:visible")
                  (.click))} title]))

(defn navbar []
  (fn []
    [:nav.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:button#navbar-hamburger.navbar-toggle.collapsed {:data-toggle "collapse" :data-target "#navbar-body" :aria-expanded "false"}
        [:span.sr-only "Toggle navigation"] [:span.icon-bar][:span.icon-bar][:span.icon-bar]]
;       [:a.navbar-brand {:href "#/"}  "Misabogados Workflow"]
]

      [:div#navbar-body.navbar-collapse.collapse

       (into [:ul.nav.navbar-nav]
             (doall (map (fn [item]  (apply nav-link item)) (:nav-links @ac/components)))
             )
       (into [:ul.nav.navbar-nav.navbar-right]
             (doall (map (fn [item]  (apply nav-link item)) (:nav-links-right @ac/components))))]]
     [:div.modal.fade {:role :dialog :id "session-timeout"}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  [:p "Session timed out"]]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]]]]]]))


(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of misabogados-workflow... work in progress"]]])

(defn validate-form [form validation]
  b/valid? form
  :email [v/email v/required]
  :name v/required
  :password v/required
  :password_confirmation v/required)

(defn signup-page []
  (let [signup-form (r/atom {})
        validation (r/atom {})]
    (fn []
;      (if (not https?) (reset! warnings "Not using ssl"))
      [:div.container
       [:div.signup-form
        [:div.form-horizontal
         [:legend "Sign-up"]
         [:div.form-group
          [:label.control-label {:for :email} "Email"]
          [:input#email.form-control {:type :email
                                      :value (:email @signup-form)
                                      :on-change #(reset! signup-form (assoc @signup-form :email (-> %  .-target .-value)))
                                      }]]
         [:div.form-group
          [:label.control-label {:for :name} "Name"]
          [:input#name.form-control {:type :text
                                     :value (:name @signup-form)
                                     :on-change #(reset! signup-form (assoc @signup-form :name (-> %  .-target .-value)))
                                     }]]
         [:div.form-group
          [:label.control-label {:for :pwd} "Password"]
          [:input#pwd.form-control {:type :password
                                    :value (:password @signup-form)
                                    :on-change #(reset! signup-form (assoc @signup-form :password (-> %  .-target .-value)))
                                    }]]
         [:div.form-group
          [:label.control-label {:for :pwd_conf} "Confirm password"]
          [:input#conf.form-control {:type :password
                                     :value (:password_confirmation @signup-form)
                                     :on-change #(reset! signup-form (assoc @signup-form :password_confirmation (-> %  .-target .-value)))
                                     }]]
         [:div.form-group [:button {:on-click #(if () (signup! @signup-form))} "Sign-up"]]
          ]]])))

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        error (r/atom nil)
        warnings (r/atom nil)
        _ (update-csrf-token!)]
    (fn []
      (if @logged-in? (aset js/window "location" "#dashboard"))
      (if (not https?) (reset! warnings "Not using ssl"))
      [:div.container
       [:div.form-horizontal
        [:legend "Login"]
         (if-let [error @error] [:p.error error])
         (if-let [warning @warnings] [:p.warning warning])
        [:div.form-group
         [:label.control-label {:for :email} "Email"]
         [:input#email.form-control {:type :email
                   :value @email
                   :on-change #(reset! email (-> %  .-target .-value))
                   }]]
        [:div.form-group
         [:label.control-label {:for :pwd} "Password"]
         [:input#pwd.form-control {:type :password
                   :value @password
                   :on-change #(reset! password (-> %  .-target .-value))
                   }]]
        [:div.form-group [:button {:on-click #(login! @email @password error)} "Login"]]]]
      )))

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to misabogados-workflow"]
    [:p "This is a home page"]]])

(def pages
  (merge lead/pages
         categories/pages
         lawyers/pages
         {:home #'home-page
          :about #'about-page
          :login #'login-page
          :signup #'signup-page
                                        ;   :debug #'debug
          :dashboard #'dashboard
          :payments #'payments
          :admin #'admin}))

(defn component-did-mount-hooks []
)

(defn page []
  (r/create-class {:component-did-mount #(component-did-mount-hooks)
                   :reagent-render (fn [] [(pages (session/get :page))])}))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/login" []
  (session/put! :page :login))

(comment (secretary/defroute "/debug" []
    (session/put! :page :debug)))

(secretary/defroute "/dashboard" []
  (session/put! :page :dashboard))

(secretary/defroute "/payments" []
  (session/put! :page :payments))

(secretary/defroute "/signup" []
  (session/put! :page :signup))

(secretary/defroute "/logout" []
  (do (logout!)
      (session/put! :page :home)))

(secretary/defroute "/admin" []
  (session/put! :page :admin))

;; -------------------------
;; Websockets



(defn process-messages [{:keys [message code] :as msg}]
  (case code
    :timeout (when-not (empty? (session/get :user))
               (u/show-modal "session-timeout")
               (session/put! :user {})
               (ac/reset-access!))
    :count nil
    (prn "unknown code " code "message " msg)))

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


(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (ws/make-websocket! (str (if (https?) "wss://" "ws://") (.-host js/location) "/ws") process-messages)
  (get-session!
   (fn [response]
     (reset! logged-in? (get response "identity"))
     (session/put! :user {:identity (get response "identity" )
                          :role (get response "role")})
     (session/put! :own-profile (get response "own-profile"))
     (session/put! :filters {:payment-requests {:own-client true
                                                :misabogados-client true
                                                :status-pending true
                                                :status-in-process true
                                                :status-paid true
                                                :status-failed true}})
     (ac/reset-access!)
     (update-csrf-token!)
     (if-not (get response "role")
       (u/redirect "#login"))
     nil))
  (hook-browser-navigation!)
  (mount-components))
