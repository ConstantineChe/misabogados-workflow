(ns misabogados-workflow.routes.registration
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :as layout]
            [hiccup.form :as form]
            [misabogados-workflow.email :as email]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.db.core :as db]
            [misabogados-workflow.util :as util]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [encrypt]]))

(def permitted #{:name :email})

(defn signup-page [r]
  (prn (:params r))
  (layout/render "signup.html" (merge {:title "Registrar"}
                                      (if-let [messages (-> r :flash :messages)]
                                        {:messages messages})
                                      (-> r :flash :params))))



(defn signup [request]
  (let [params (:params request)
        session (:session request)]
    (if-not (= (:password params) (:confirm-password params))
      (-> (redirect "/signup")
          (assoc-in [:flash :messages :errors :error] "Password doesn't match with confirmation")
          (assoc-in [:flash :params] params))
      (try (when-let [user (db/create-user (into {:verification-code (-> params :email util/generate-hash)
                                              :password (-> params :password encrypt)
                                              :role :client}
                                             (filter #(permitted  (key %))
                                                     (:params request))))]
             (future (email/verification-email (assoc user :verification-url
                                                      (str (util/base-path request) "/verify/" (:verification-code user)))))
             (if (= "application/transit+json; charset=UTF-8" (:content-type request))
               (-> (response {:identity (-> request :params :email)
                              :role (-> request :params :role)})
                   (assoc :session (assoc session :identity (-> request :params :email keyword))))
               (-> (redirect "/")
                   (assoc :session (assoc session :identity (-> request :params :email keyword))))))

           (catch com.mongodb.DuplicateKeyException e
             (if (= "application/transit+json; charset=UTF-8" (:content-type request))
               (response {:error (str "User with email " (:email params) " already exists.")})
               (-> (redirect "/signup")
                   (assoc-in [:flash :messages :errors :error]
                             (str "User with email " (:email params) " already exists."))
                   (assoc-in [:flash :params] params))))))))

(defn verify-email [code request]
  (if-let [user (db/find-user-by-code code)]
    (if-not (:verified user)
      (do
        (db/update-user (str (:_id user)) {:verified true})
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
