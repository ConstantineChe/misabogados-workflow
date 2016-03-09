(ns misabogados-workflow.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [config.core :refer [env]])
    (:import org.bson.types.ObjectId))


(defonce db (atom nil))

(defn connect! []
  ;; Tries to get the Mongo URI from the environment variable
  (reset! db (-> (:database-url env)
                 mg/connect-via-uri :db)))

(defn disconnect! []
  (when-let [conn @db]
    (mg/disconnect conn)
    (reset! db nil)))

(defn oid [id] (ObjectId. id))

(defn create-user [user]
  (mc/insert @db "users" user))

(defn update-user [id data]
  (mc/update @db "users" {:_id (oid id)}
             {$set data}))

(defn get-user [email]
  (mc/find-one-as-map @db "users" {:email email}))

(defn get-users []
  (mc/find-maps @db "users"))

(defn get-lead [id]
  (mc/find-one-as-map @db "leads" {:_id (oid id)}))

(defn get-leads [role identity]
  (cond (= :admin role)
        (mc/find-maps @db "leads")
        (= :operator role)
        (mc/find-maps @db "leads" {:step {$nin ["archive"]}})
        (= :lawyer role)
        {}
        (= :client role)
        {}))

(defn update-lead [id fields]

  (mc/update-by-id @db "leads" (oid id) {$set fields}))

(defn create-lead [fields]
  (mc/insert  @db "leads" fields))

(defn find-user-by-code [code]
  (mc/find-one-as-map @db "users" {:verification-code code}))

(defn create-payment-request [fields]
  (mc/insert @db "payment_requests" fields))

(defn get-payment-requests
  ([]
   (mc/find-maps @db "payment_requests"))
  ([lawyer]
   (mc/find-maps @db "payment_requests" {:lawyer lawyer})))

(defn get-payment-request-by-code [code]
  (mc/find-one-as-map @db "payment_requests" {:code code}))
