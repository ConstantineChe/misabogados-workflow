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

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-users [request] (response (doall (db/get-users))))

(defn update-user [{:keys [params]}]
  (try
    (db/update-user (:id params) (:data params))
    (response {:success true})))

(defn get-user [{:keys [params]}] (response (db/get-user (:id params))))

(defroutes users-routes
  (GET "/users" [] (restrict get-users
                             {:handler {:or [ac/admin-access ac/lawyer-access]}
                              :on-error access-error-handler}))
  (GET "/user" [] get-user)
  (PUT "/user" [] update-user))
