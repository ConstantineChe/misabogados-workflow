(ns misabogados-workflow.routes.admin.lawyers
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE] :as c]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.db.core :as db :refer [oid]]
            [monger.operators :refer :all]
            [misabogados-workflow.util :as util]
            [monger.collection :as mc]
            [monger.joda-time]
            [clojure.walk :as walk]
            [clj-time.local :as l]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.access-control :as ac]
            [buddy.auth.accessrules :refer [restrict]]))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-lawyers
  "Returns all lawyers from db."
  [request]
  (response (mc/find-maps @db/db "lawyers")))

(defn get-lawyer
  "Retuns requested categorie by id."
  [id]
  (response (mc/find-one-as-map @db/db "lawyers" {:_id (oid id)}))
  )

(defn update-lawyer
  "Update lawyer by id."
  [id {params :params}]
  (mc/update-by-id @db/db "lawyers" (oid id) {$set (:lawyer params)})
  (response {:id id :status "updated"})
  )

(defn create-lawyer
  "Create new lawyer"
  [{params :params}]
  (let [id (:_id (mc/insert-and-return @db/db "lawyers" (:lawyer params)))]
    (response {:id id :status "created"}))
  )

(defn delete-lawyer
  "Delete lawyer by id"
  [id]
  (mc/remove-by-id @db/db "lawyers" (oid id))
  (response {:id id :status "deleted"})
  )



(defroutes lawyers-admin
  (GET "/admin/lawyers" [] (restrict get-lawyers
                                        {:handler ac/admin-access
                                         :on-error access-error-handler}))
  (GET "/admin/lawyers/:id" [id :as request] (restrict (fn [r] (get-lawyer id))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler}))
  (PUT "/admin/lawyers/:id" [id :as request] (restrict (fn [r] (update-lawyer id request))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler}))
  (POST "/admin/lawyers" [] (restrict create-lawyer
                                        {:handler ac/admin-access
                                         :on-error access-error-handler}))
  (DELETE "/admin/lawyers/:id" [id :as request] (restrict (fn [r] (delete-lawyer id))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler})))
