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

(defn get-leads [role identity]
  (cond (= :admin role)
        (mc/aggregate @db "leads"
                      [;;{"$match" {:matches {"$exists" true}}}
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
                       ;;{"$limit" 20}
                       {"$sort" {:_id -1}}
                       ])
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

(defn get-payment-request-by-webpay-payment-code [code]
  (mc/find-one-as-map @db "payment_requests" {:payment_log {$elemMatch {"data.TBK_ORDEN_COMPRA" code}}}))





(def charmap [{:or "í" :rp "i"}
              {:or "é" :rp "e"}
              {:or "á" :rp "a"}
              {:or "ó" :rp "o"}
              {:or "ú" :rp "u"}
              {:or "ü" :rp "u"}
              {:or "ñ" :rp "n"}])


(defn update-cats [rq] (let [categories (mc/find-maps @db "categories")]
                       (dorun (for [category categories
                                    :let [slug (apply str (filter #(re-matches #"[a-z],\-" (str %))
                                                                  (reduce (fn [slg chr] (clojure.string/replace slg (re-pattern (:or chr)) (:rp chr)))
                                                                          (clojure.string/lower-case (clojure.string/replace (:slug category) #"\s+" "-"))
                                                                          charmap)))]]
                                (mc/update @db "categories" {:_id (:_id category)} {$set {:slug slug}})
                                ))))
