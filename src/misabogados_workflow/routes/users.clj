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
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.middleware :as mw]
            [buddy.auth.accessrules :refer [restrict]]))

(defroutes users-routes
  (GET "/users" [] (restrict (fn [request] (response (doall (db/get-users)))) 
                             {:handler {:or [ac/admin-access ac/lawyer-access]}
                              :on-error mw/on-error-json}))
  (GET "/user/:id" [] (fn [request] (response {:status 200 :body (str (-> request :params :id) (-> request :session :role))}))))
