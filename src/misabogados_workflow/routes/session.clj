(ns misabogados-workflow.routes.session
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as db]
            [buddy.auth :refer [authenticated?]]
            [buddy.hashers :refer [encrypt check]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn login-page [request]
  (if-not (authenticated? request)
    (layout/render "login.html" (merge {:title "Ingresar"}
                                       (if-let [messages (-> request :flash :messages)]
                                        {:messages messages})
                                       (-> request :flash :params)))
    (-> (redirect "/")
        (assoc :flash (assoc-in request
                       [:params :errors] "Ya se ha ingresado")))))

(defn login [request]
  (let [user (db/get-user (:email (:params request)))
        password (:password (:params request))
        session (:session request)]
    (let [success? (check password (:password user))
          updated-session (into session {:identity (keyword (:email user))
                                         :role (keyword (:role user))})]
      (if (= "application/transit+json; charset=UTF-8" (:content-type request))
        (if success?
          (-> (content-type (ok {:identity (keyword (:email user))
                                 :status "ok"
                                 :role (keyword (:role user))}) "application/json")
              (assoc :session updated-session))
          (content-type (ok {:status "fail"
                             :error "Email o contraseña incorrecta"})
                        "application/json"))
        (if success?
          (-> (redirect "/")
              (assoc :session updated-session))
          (-> (redirect "/login")
              (assoc-in [:flash :params] (:params request))
              (assoc-in [:flash :messages :errors :error]
                        "incorrect username or password")))))))

(defn logout [request]
  (if (= "application/transit+json; charset=UTF-8" (:content-type request))
    (-> (content-type (ok {:status :success}) "application/json") (assoc :session {}))
    (-> (redirect "/") (assoc :session {}))))


(defroutes session-routes
  (GET "/login" [] login-page)
  (POST "/login" [] login)
  (GET "/logout" [] logout)
  (GET "/csrf-token" [] (content-type (ok {:token *anti-forgery-token*}) "application/json"))
  (GET "/request" request (str request))
  (GET "/session" request (content-type (ok {:identity (:identity request) :role (-> request :session :role)}) "application/json")))
