(ns misabogados-workflow.routes.session
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect]]
            [misabogados-workflow.db.core :as db]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [encrypt check]]
            [misabogados-workflow.layout.core :as layout]))

(defn login-page [request]
  (if-not (authenticated? request)
    (layout/login)
    (-> (redirect "/")
        (assoc :flash (assoc-in request
                       [:params :errors] "Already logged in")))))

(defn login [request]
  (let [user (db/get-user (:email (:params request)))
        password (:password (:params request))
        session (:session request)]
    (if (check password (:password user))
        (let [updated-session (assoc session :identity (keyword (:email user)))]
          (-> (redirect "/")
              (assoc :session updated-session))))))

(defn logout [request]
  (-> (redirect "/")
      (assoc :session {})))




(defroutes session-routes
  (GET "/login" [] login-page)
  (POST "/login" [] login)
  (GET "/logout" [] logout))
