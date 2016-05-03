(ns misabogados-workflow.routes.registration
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :as layout]
            [hiccup.form :as form]
            [misabogados-workflow.email :as email]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.db.core :as db]
            [monger.collection :as mc]
            [monger.operators :refer :all]
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
          (assoc-in [:flash :messages :errors :error] "La contraseña y su confirmación no coinciden")
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
              (assoc :session updated-session)
              (assoc-in [:flash :messages :success :verified] "Your email was verified."))))
      (redirect "/"))
    (-> (redirect "/")
        (assoc-in [:flash :messages :errors :invalid-code] "Código de virificación invalido"))))

(defn forgot-password-page [request]
  (if (authenticated? request)
    (redirect "/")
    (layout/render "forgot-password.html" (merge {:title "Forgot password"}
                                                (if-let [messages (-> request :flash :messages)]
                                                  {:messages messages})))))

(defn forgot-password [request]
  (let [params (:params request)
        user (db/get-user (:email params))
        code (-> params :email util/generate-hash)]
    (if user
      (do (db/update-user (str (:_id user)) {:reset-code code})
          (future (email/reset-password-email user (str (util/base-path request) "/reset-password/" code)))
          (-> (redirect "/")
              (assoc-in [:flash :messages :success :reset-password-email]
                        (str "Email with passwd reset link sent to " (:email params)))))
      (-> (redirect "/forgot-passowrd")
          (assoc-in (assoc-in [:flash :messages :errors :no-user]
                              (str "User with email " (:email user) " doesn't exists.")))))))

(defn reset-password-page [code request]
  (let [user (mc/find-one-as-map @db/db "users" {:reset-code code})]
    (if-not user
      (-> (redirect "/")
          (assoc-in [:flash :messages :errors :invalid-code]
                       "Invalid passowrd reset code."))
      (layout/render "reset-password.html" (merge {:title "Reset password"
                                                   :code code}
                                                  (if-let [messages (-> request :flash :messages)]
                                                  {:messages messages}))))))

(defn reset-passowrd [request]
  (let [params (:params request)
        code (:code params)
        user (mc/find-one-as-map @db/db "users" {:reset-code code})
        session (:session request)]
    (if-not user
      (-> (redirect "/")
          (assoc-in [:flash :messages :errors :invalid-code]
                      "Invalid passowrd reset code."))
      (if (= (:new-password params) (:confirm-password params))
        (do (db/update-user (str (:_id user)) {:password (encrypt (:new-password params))})
            (-> (redirect "/")
                (assoc :session (assoc session :identity (-> user :email keyword)))
                (assoc-in [:flash :messages :success :password-reset]
                                     "New password was set")))
        (-> (redirect (str "/reset-password/" code))
            (assoc-in [:flash :messages :errors :error] "La contraseña y su confirmación no coinciden"))))))

(defroutes registration-routes
  (GET "/signup" [] signup-page)
  (POST "/signup" [] signup)
  (GET "/forgot-password" [] forgot-password-page)
  (POST "/forgot-password" [] forgot-password)
  (GET "/reset-password/:code" [code :as request] (reset-password-page code request))
  (POST "/reset-password" [] reset-passowrd)
  (GET "/verify/:code" [code :as request] (verify-email code request))
  (GET "/reset/:code" [code :as request] (reset-password-page code request))
)
