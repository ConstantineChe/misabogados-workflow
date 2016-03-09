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

(defn update-user [id {:keys [params]}]
  (db/update-user id (clojure.walk/keywordize-keys (:data params)))
  (clojure.pprint/pprint (clojure.walk/keywordize-keys (:data params)))
  (response {:req params :id id})  )

(defn get-user [{:keys [params]}] (response (db/get-user (:id params))))

(defroutes users-routes
  (GET "/users" [] (restrict get-users
                             {:handler {:or [ac/admin-access]}
                              :on-error access-error-handler}))
  (PUT "/users/:id" [id :as request] (restrict (fn [request] (update-user id request))
                                               {:handler {:or [ac/admin-access]}
                                                :on-error access-error-handler})))
