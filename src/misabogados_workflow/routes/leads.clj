(ns misabogados-workflow.routes.leads
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
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
            [misabogados-workflow.settings :refer [settings]]
            [misabogados-workflow.flow :as flow]
            [misabogados-workflow.flow-definition :refer [steps]]
            [misabogados-workflow.util :refer [wrap-datetime]])
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

(defn create-lead-ajax [request]
  (clojure.pprint/pprint request)
  (let [params (-> request :params :lead)
        params (objectify-ids params)
        lead (mc/insert-and-return @db/db "leads" (assoc params :date_created (new java.util.Date)))]
    (actions/do-lead-actions (:actions params) lead)
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
        identity (:identity request)
        {:keys [sort-field sort-dir per-page page filters]} (:params request)
        leads (db/get-leads role identity
                            (Integer. per-page)
                            (Integer. page)
                            {sort-field (Integer. sort-dir)}
                            filters)]
    (response {:status "ok" :leads-count (db/get-leads-count role identity filters) :leads leads})))

(defn get-options []
  (response
   {:region_code (into [["" ""]] (map #((juxt :name :code) %) (:regions @settings)))
    :lead_type_code (into [["" ""]] (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_types")))
    :lead_source_code (into [["" ""]] (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_sources")))
    :category_id (map #((juxt :name :_id) %) (mc/find-maps @db/db "categories"))
    :client_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db/db "clients"))
    :matches {:lawyer_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db/db "lawyers"))}}))

(defn get-lead-actions [id request]
  (let [lead (db/get-lead id)
        step (if (:step lead) (keyword (:step lead)) :pitch)
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
