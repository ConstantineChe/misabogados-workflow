(ns misabogados-workflow.routes.users
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as db]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.layout.core :as layout]
            [buddy.auth.accessrules :refer [restrict success error]]))

(defn role-access [request role] (if (= role (-> request :session :role))
                                   true
                                   (error "no")))

(defn admin-access [request] (role-access request :admin))

(defn lawyer-access [request] (role-access request :lawyer))

(defroutes users-routes
  (GET "/users" [] (restrict (fn [request] (response (doall (db/get-users))))
                             {:handler {:or [admin-access lawyer-access]}
                              :on-error (fn [request value] {:status 403
                                                             :header {}
                                                            :body {:error "not autherized"
                                                                   :value value
                                                                    :role (-> request :session :role)}})}))
  (GET "/user/:id" [] (fn [request] (response {:status 200 :body (str (-> request :params :id) (-> request :session :role))}))))
