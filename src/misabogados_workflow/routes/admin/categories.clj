(ns misabogados-workflow.routes.admin.categories
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

(defn get-categories
  "Returns all categories from db."
  [request]
  (response (mc/find-maps @db/db "categories")))

(defn get-category
  "Retuns requested categorie by id."
  [id]
  (response (mc/find-one-as-map @db/db "categories" {:_id (oid id)}))
  )

(defn update-category
  "Update category by id."
  [id {params :params}]
  (mc/update-by-id @db/db "categories" (oid id) {$set (:category params)})
  (response {:id id :status "updated"})
  )

(defn create-category
  "Create new category"
  [{params :params}]
  (let [id (:_id (mc/insert-and-return @db/db "categories" (:category params)))]
    (response {:id id :status "created"}))
  )

(defn delete-category
  "Delete category by id"
  [id]
  (mc/remove-by-id @db/db "categories" (oid id))
  (response {:id id :status "deleted"})
  )



(defroutes categories-admin
  (GET "/admin/categories" [] (restrict get-categories
                                        {:handler ac/admin-access
                                         :on-error access-error-handler}))
  (GET "/admin/categories/:id" [id :as request] (restrict (fn [r] (get-category id))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler}))
  (PUT "/admin/categories/:id" [id :as request] (restrict (fn [r] (update-category id request))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler}))
  (POST "/admin/categories" [] (restrict create-category
                                        {:handler ac/admin-access
                                         :on-error access-error-handler}))
  (DELETE "/admin/categories/:id" [id :as request] (restrict (fn [r] (delete-category id))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler})))
