(ns misabogados-workflow.routes.leads
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.model :refer [->Lead map->User map->BasicInfo]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.routes.lead-actions :as actions]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [misabogados-workflow.db.core :as db :refer [oid]]
            [monger.operators :refer :all]
            [misabogados-workflow.util :as util]
            [monger.collection :as mc]
            [monger.joda-time]
            [clojure.walk :as walk]
            [clj-time.local :as l]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.flow :as flow]
            [misabogados-workflow.flow-definition :refer [steps]])
  (:import org.bson.types.ObjectId))

(defn get-step [action]
  ((keyword action) steps))

;; (defn update-lead [id {:keys [params]}]
;;   (db/update-lead id (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])))
;;   (redirect "/#dashboard"))

(defn allowed-to-edit [id request]
  (if (or (contains? #{:admin :operator :finance} (-> request :session :role))

          )
    true
    {:message "Not allowed"}))

(defn id-fields [lead]
  (into [:client_id :category_id]
        (map (fn [x] [:matches x :lawyer_id]) (range (count (:matches lead))))))

(defn objectify-ids [lead]
  (reduce #(let [key  (if (sequential? %2) %2 [%2])
                 value (get-in %1 key)]
             (if value (assoc-in %1 key (ObjectId. value)) %1)) lead (id-fields lead)))

(defn wrap-datetime [params]
  (walk/postwalk
   #(if (and (string? %)
             (re-matches #"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}).+" %))
      (try (l/to-local-date-time %)
           (catch Exception e  %)) %)
   params))

(defn create-lead-ajax [request]
  (let [params (assoc (-> request :params :lead) :matches [(-> request :params :lead :matches)])
        params (assoc-in params [:matches 0 :meetings] [(get-in params [:matches 0 :meetings])])
        params (objectify-ids params)
        lead (mc/insert-and-return @db/db "leads" (assoc params :date_created (new java.util.Date)))]
    (response {:staus "ok" :id (:_id lead)})))

(defn update-lead-data [lead id]
  (let [lead (objectify-ids lead)]
    (mc/update-by-id @db/db "leads" id {$set (assoc lead :date_updated (new java.util.Date))})))

(defn update-lead-ajax [id request]
  (let [id (oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if allowed?
      (do (update-lead-data (:lead params) id)
          (actions/do-lead-actions (:actions params) (db/get-lead (str id)))
          (response {:lead {:update id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))

(defn do-action [id action {:keys [params]}]
  (let [id (oid id)
        lead (assoc  (dissoc (:lead params) :_id) :step action)
        step ((keyword action) steps)]
      (case (:type step)
            :manual (do (update-lead-data lead id)
                        (response {:status "ok" :id id :step action}))
            :auto (do (update-lead-data (assoc lead :step (:endpoint step)) id)
                      ((:auto-action step) (assoc lead :_id id))
                      (response {:status "ok" :id id :step (:endpoint step)})))))


(defn get-leads [request]
  (let [role (-> request :session :role)
        identity (:identity request)]
    (response {:status "ok" :leads (doall (db/get-leads role identity))})))

(defn get-options []
  (response
   {:lead_type_code (into [["" ""]] (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_types")))
    :lead_source_code (into [["" ""]] (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_sources")))
    :category_id (map #((juxt :name :_id) %) (mc/find-maps @db/db "categories"))
    :client_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db/db "clients"))
    :matches {:lawyer_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db/db "lawyers"))}}))

(defn get-lead-actions [id request]
  (let [lead (db/get-lead id)
        step (if (:step lead) (keyword (:step lead)) :check)
        actions (filter #((:roles %) (-> request :session :role)) (:actions (step steps)))]
    (response {:id id :actions actions})))


(defroutes leads-routes
  (PUT "/lead/:id" [id :as request] (update-lead-ajax id request))
  (GET "/lead/:id/actions" [id :as request] (get-lead-actions id request))
  (PUT "/lead/:id/action/:action" [id action :as request]
         (if ((keyword action) steps) (do-action id action request)))
  (POST "/lead" [] create-lead-ajax)
  (GET "/leads" [] get-leads)
  (GET "/lead/:id" [id :as request] (response (db/get-lead id)))
  (GET "/leads/options" [] (get-options)))
