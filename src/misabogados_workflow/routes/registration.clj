(ns misabogados-workflow.routes.registration
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.db.core :as db]
            [misabogados-workflow.util :as util]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [encrypt]]))

(def permitted #{:name :email})

(defn signup-page [r]
  (layout/render "signup.html" {:title "Registrar"}))



(defn signup [request]
  (let [session (:session request)]
      (db/create-user (into {:verification-code (-> request :params :email util/generate-hash)
                             :password (-> request :params :password encrypt)
                             :role :client}
                          (filter #(permitted  (key %))
                                  (:params request))))
      (-> (response {:identity (-> request :params :email)
                     :role (-> request :params :role)})
          (assoc :session (assoc session :identity (-> request :params :email keyword))))))

;; TODO add validation!!
;; TODO add messages

(defn verify-email [code request]
  (if-let [user (db/find-user-by-code code)]
    (if-not (:verified user)
      (do
        (db/update-user (:email user) {:verified true})
        (let [updated-session (assoc (:session request) :identity (keyword (:email user)))]
          (-> (redirect "/")
              (assoc :session updated-session))))
      (redirect "/"))
    (redirect "/" (assoc :flash (assoc-in request [:params :errors]
                                          "Invalid verification code")))))


(defroutes registration-routes
  (GET "/signup" [] signup-page)
  (POST "/signup" [] signup)
  (GET "/verify/:code" [code :as request] (verify-email code request))
)
