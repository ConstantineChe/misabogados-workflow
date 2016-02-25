(ns misabogados-workflow.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [config.core :refer [env]])
    (:import org.bson.types.ObjectId))


(defonce db (atom nil))

(defn connect! []
  ;; Tries to get the Mongo URI from the environment variable
  (reset! db (-> (:database-url env) mg/connect-via-uri :db)))

(defn disconnect! []
  (when-let [conn @db]
    (mg/disconnect conn)
    (reset! db nil)))

(defn oid [id] (ObjectId. id))

(defn create-user [user]
  (mc/insert @db "users" user))

(defn update-user [email data]
  (mc/update @db "users" {:email email}
             {$set data}))

(defn get-user [email]
  (mc/find-one-as-map @db "users" {:email email}))

(defn get-users []
  (mc/find-maps @db "users"))

(defn get-lead [id]
  (mc/find-one-as-map @db "leads" {:_id (oid id)}))

(defn get-leads []
  (mc/find-maps @db "leads"))

(defn update-lead [id fields]

  (mc/update-by-id @db "leads" (oid id) {$set fields}))

(defn create-lead [fields]
  (mc/insert  @db "leads" fields))

(defn find-user-by-code [code]
  (mc/find-one-as-map @db "users" {:verification-code code}))

(defn create-payment [fields]
  (mc/insert @db "payments" fields))

(defn get-payments
  ([]
   (mc/find-maps @db "payments"))
  ([lawyer]
   (mc/find-maps @db "payments" {:lawyer lawyer})))
