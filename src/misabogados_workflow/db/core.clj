(ns misabogados-workflow.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [config.core :refer [env]]
              [clojure.tools.logging :as log])
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
  (mc/insert-and-return @db "users" user))

(defn update-user [id data]
  (mc/update @db "users" {:_id (oid id)}
             {$set data}))

(defn get-user [email]
  (mc/find-one-as-map @db "users" {:email email}))

(defn get-users []
  (mc/find-maps @db "users"))

(defn get-lead [id]
  (mc/find-one-as-map @db "leads" {:_id (oid id)}))

(defn get-leads [role identity per-page page sort]
  (let [offset (* per-page (dec page))]
    (prn sort)
    (cond (= :admin role)
          (mc/aggregate @db "leads"
                        [

                         {"$unwind" {:path "$matches", :preserveNullAndEmptyArrays true}}
                         {"$lookup" {:from "categories"
                                     :localField :category_id
                                     :foreignField :_id
                                     :as :category}}
                         {"$lookup" {:from "lead_types"
                                     :localField :lead_type_code
                                     :foreignField :code
                                     :as :lead_type}}
                         {"$lookup" {:from "lawyers"
                                     :localField :matches.lawyer_id
                                     :foreignField :_id
                                     :as :lawyer}}
                         {"$lookup" {:from "clients"
                                     :localField :client_id
                                     :foreignField :_id
                                     :as :client}}
;                         {"$match" {:client {$elemMatch {:email {$regex "test" $options "i"}}}}}
                         {"$sort" sort}
                         {"$skip" offset}
                         {"$limit" per-page}
                         ])
          (= :operator role)
          (mc/find-maps @db "leads" {:step {$nin ["archive"]}})
          (= :lawyer role)
          {}
          (= :client role)
          {})))



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

(defn get-payment-request-by-webpay-payment-code [code]
  (mc/find-one-as-map @db "payment_requests" {:payment_log {$elemMatch {"data.TBK_ORDEN_COMPRA" code}}}))
