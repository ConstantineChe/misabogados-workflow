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
            [misabogados-workflow.flow :refer [get-rendered-form dataset PManual PAutomatic]]
            [misabogados-workflow.flow-definition :refer [steps]])
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]
           org.bson.types.ObjectId))

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

(defn update-lead-ajax [id request]
  (let [id (oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)
        lead (objectify-ids (:lead params))]
    (prn "params request" (:params request))
    (prn "params upd " params)
    (prn lead)
    (if (true? allowed?)
      (do (mc/update-by-id @db/db "leads" id {$set
                                              (assoc lead
                                                  :date_updated (new java.util.Date))})
          (actions/do-lead-actions (:actions params) (db/get-lead (str id)))
          (response {:lead {:update id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))

(defn do-action [id action {:keys [params]}]
  (let [lead (assoc  (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])) :step action)
        step ((keyword action) steps)]
    (db/update-lead id lead)
    (cond (satisfies? PManual step)
          (redirect "/#dashboard")
          (satisfies? PAutomatic step)
          (do
            (.do-action step lead)
            (db/update-lead id {:step (:action step)})
            (redirect "/#dashboard")))))

(defn create-lead [{:keys [params]}]
  (db/create-lead (assoc  (:lead (transform-keys ->snake_case_keyword params)) :step :check))
  (redirect "/#dashboard"))

(defn edit-lead [id]
  (let [lead (db/get-lead id)]
    (layout/blank-page "Form"
                       (layout/render-form "Edit lead"
                                           ["PUT" (str "/lead/" id)]
                                           (list (.create-form (get-step "archive")  {:lead lead} :admin)
                                                            [:button.btn.btn-secondary "Save"])))))

(defn new-lead [params]
  (layout/blank-page "Form"
                     (layout/render-form "New lead"
                                         ["POST" "/leads"]
                                         (list (get-rendered-form [:lead :user :basic-info]  {:lead {}})
                                               [:button.btn.btn-secondary "Save"]))))

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

(defroutes leads-routes
  (GET "/lead/:id/edit" {{id :id} :params} (edit-lead id))
  (GET "/leads/create" [] new-lead)
  (PUT "/lead/:id" [id :as request] (update-lead-ajax id request))
  (GET "/lead/:id/action/:action" {{id :id action :action} :params {role :role} :session}
       (if (contains? steps (keyword action))
         (layout/render-form action ["PUT" (str "/lead/" id)]
                             (.create-form (get-step action)
                                           {:lead (db/get-lead id)}
                                           role))))
  (PUT "/lead/:id/action/:action" [id action :as request]
       (if (contains? steps (keyword action)) (do-action id action request)))
  (POST "/lead" [] create-lead-ajax)
  (POST "/leads" [] create-lead)
  (GET "/leads" [] get-leads)
  (GET "/lead/:id" [id :as request] (response (db/get-lead id)))
  (GET "/leads/options" [] (get-options)))
