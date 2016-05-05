(ns misabogados-workflow.routes.admin.categories
  (:require [buddy.auth.accessrules :refer [restrict]]
            [clj-time.local :as l]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [compojure.core :refer [defroutes GET PUT POST DELETE] :as c]
            [config.core :refer [env]]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.db.core :as db :refer [oid]]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.util :as u]
            [misabogados-workflow.util :as util]
            [monger.collection :as mc]
            [monger.joda-time]
            [monger.operators :refer :all]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :refer [redirect response]]))

(def files (atom #{}))

(defn file-path [filename]
  (if (:production env)
    (str (:uploads-path env) "/category/" filename)
    (str "/uploads/category/" filename)))

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
  "Retuns requested category by id."
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

(defn upload-file
  "upload image file"
  [request]
  (let [file-params (get-in request [:multipart-params "file"])
        filename (u/generate-hash (:filename file-params))
        extension (re-find #"\.\w+$" (:filename file-params))]
    (if (#{".jpg" "jpeg" ".png" ".img"} extension)
      (do (io/copy (:tempfile file-params)
                (io/file (str "/tmp/" filename extension)))
       (swap! files conj filename)
       (response {:filename (str filename extension)}))
      (response {:error "not an image file"}))))


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
                                                           :on-error access-error-handler}))
  (POST "/admin/categories/file" [] (restrict (fn [r] (upload-file r))
                                                          {:handler ac/admin-access
                                                           :on-error access-error-handler})))
