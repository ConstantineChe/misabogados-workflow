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
            [misabogados-workflow.flow :refer [get-rendered-form dataset PManual PAutomatic]]
            [misabogados-workflow.flow-definition :refer [steps]])
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]))

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

(defn update-lead-ajax [id request]
  (let [id (oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if (true? allowed?)
      (do (mc/update-by-id @db/db "leads" id {$set
                                           (assoc (dissoc params :lead)
                                                  :date_updated (new java.util.Date)
                                                  :code (util/generate-hash (:params request)))})
          (actions/do-lead-actions (:actions params) (db/get-lead id))
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
  (POST "/leads" [] create-lead)
  (GET "/leads" [] get-leads)
  (GET "/lead/:id" [id :as request] (response (db/get-lead id)))
  (GET "/leads/options" [] (response {:lead_type_code (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_types"))
                                      :lead_source_code (map #((juxt :name :code) %) (mc/find-maps @db/db "lead_sources"))
                                      :category_id (map #((juxt :name :_id) %) (mc/find-maps @db/db "categories"))
                                      :client_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %)
                                                      (mc/find-maps @db/db "clients"))
                                      :matches {:lawyer_id (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %)
                                                                (mc/find-maps @db/db "lawyers"))}})))
