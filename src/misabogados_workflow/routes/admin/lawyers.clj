(ns misabogados-workflow.routes.admin.lawyers
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE] :as c]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.db.core :as db :refer [oid]]
            [monger.operators :refer :all]
            [misabogados-workflow.util :as util]
            [monger.collection :as mc]
            [monger.joda-time]
            [clojure.walk :as walk]
            [clj-time.local :as l]
            [config.core :refer [env]]
            [misabogados-workflow.util :as u]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.access-control :as ac]
            [buddy.auth.accessrules :refer [restrict]]))


(defn file-path [filename]
  (if (:production env)
    (str (:uploads-path env) "/lawyer/" filename)
    (str (.getPath (io/resource "public")) "/uploads/lawyer/" filename)))

(defn uploads-url [filename]
  (if (:production env)
    (str (:uploads-url env) "/lawyer/" filename)
    (str "/uploads/lawyer/" filename)))

(defn save-file
  "Move file temporary file."
  [filename new-path]
  (let [file (io/file (file-path new-path))]
    (prn filename)
    (io/make-parents file)
    (io/copy (io/file filename) file)))

(defn create-filename [id tmp]
  (let [extension (re-find #"\.\w+$" tmp)]
    (str id extension)))

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
  "Retuns requested lawyer by id."
  [id]
  (response (mc/find-one-as-map @db/db "lawyers" {:_id (oid id)}))
  )

(defn update-lawyer
  "Update lawyer by id."
  [id {params :params}]
  (let [tmp-filename (get-in params [:lawyer :profile_picture :tmp-filename])
        filename (create-filename id tmp-filename)
        lawyer (if tmp-filename
                   (assoc (:lawyer params) :image (uploads-url filename))
                   (:lawyer params))]
    (when tmp-filename
      (save-file (str "/tmp/" tmp-filename) filename))
    (mc/update-by-id @db/db "lawyers" (oid id) {$set lawyer})
    (response {:id id :status "updated"}))
  )

(defn create-lawyer
  "Create new lawyer"
  [{params :params}]
  (let [id (:_id (mc/insert-and-return @db/db "lawyers" (:lawyer params)))
        tmp-filename (get-in params [:lawyer :profile_picture :tmp-filename])
        filename (create-filename id tmp-filename)
        lawyer (if tmp-filename
                   (assoc (:lawyer params) :image (uploads-url filename))
                   (:lawyer params))]
    (when tmp-filename
        (save-file (str "/tmp/" tmp-filename) filename)
        (mc/update-by-id @db/db "lawyers" (oid id) {$set lawyer}))
    (response {:id id :status "created"}))
  )

(defn delete-lawyer
  "Delete lawyer by id"
  [id]
  (mc/remove-by-id @db/db "lawyers" (oid id))
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
       (response {:filename (str filename extension)}))
      (response {:error "not an image file"}))))

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
                                                           :on-error access-error-handler}))
  (POST "/admin/lawyers/file" [] (restrict (fn [r] (upload-file r))
                                           {:handler ac/admin-access
                                            :on-error access-error-handler})))
