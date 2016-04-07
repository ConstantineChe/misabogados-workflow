(ns misabogados-workflow.routes.users
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as db]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.middleware :as mw]
            [buddy.auth.accessrules :refer [restrict]]
            [clojure.walk :refer [keywordize-keys]]))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-users [request] (response (doall (db/get-users))))

(defn update-user [id {:keys [params]}]
  (db/update-user id (clojure.walk/keywordize-keys (:data params)))
  (response {:req params :id id})  )

(defn get-user [{:keys [params]}] (response (db/get-user (:id params))))

(defroutes users-routes
  (GET "/users" [] (restrict get-users
                             {:handler {:or [ac/admin-access]}
                              :on-error access-error-handler}))
  (GET "/users/client/:id" [] #(response (mc/find-one-as-map @db/db "clients"  {:_id (-> % keywordize-keys :params :id db/oid)})))
  (POST "/users/client" [] (restrict #(response (do (mc/insert-and-return @db/db "clients" (-> % :params :data))))
                                            {:handler {:or [ac/admin-access ac/operator-access]}
                                             :on-error access-error-handler}))
  (PUT "/users/client" [] #(do (prn (:params %)) (response (do (mc/update-by-id @db/db "clients"
                                                                              (-> % keywordize-keys :params :id db/oid)
                                                                              {$set (-> % keywordize-keys :params :data)})
                                                               (mc/find-one-as-map @db/db "clients"
                                                                                   {:_id (-> % keywordize-keys :params :id db/oid)})))))
  (PUT "/users/:id" [id :as request] (restrict (fn [request] (update-user id request))
                                               {:handler {:or [ac/admin-access]}
                                                :on-error access-error-handler})))
