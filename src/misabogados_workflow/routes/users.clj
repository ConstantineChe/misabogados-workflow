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


(def admin-access
  {:handler (fn [request] (if (= :admin (-> request :session :role))
                           success))
   :on-error (fn [request value] {:status 403
                                 :header {}
                                 :body {:error "not autherized"
                                        :role (-> request :session :role)}})})


(defroutes users-routes
  (GET "/users" [] (restrict (response {:status 200 :body (str (db/get-users))}) admin-access))
  (GET "/user/:id" [] (fn [request] (response {:status 200 :body (str (-> request :params :id) (-> request :session :role))}))))
