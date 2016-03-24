(ns misabogados-workflow.routes.payment-requests
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.layout :refer [render]]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as dbcore :refer [db oid]]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.middleware :as mw]
            [misabogados-workflow.util :as util]
            [misabogados-workflow.email :as email]
            [buddy.auth.accessrules :refer [restrict]]))

(defn get-current-user [request]
  (dbcore/get-user (:identity request)))

(defn get-current-user-id [request]
  (:_id (get-current-user request)))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn allowed-to-edit [id request]
  (if (or (= :admin (-> request :session :role))
          (= :finance (-> request :session :role))
          (= (:lawyer (mc/find-one-as-map @db "payment_requests" {:_id id}))
             (get-current-user-id request)))
    true
    {:message "Not allowed"}))

(defn get-payment-requests [request]
  (let [payment-requests (apply merge (map (fn [payment-request]
                          {(str (:_id payment-request)) (dissoc payment-request :_id)})
                                           (cond (= :lawyer (-> request :session :role))
                                                 (dbcore/get-payment-requests (get-current-user-id request))
                                                 (or (= :admin (-> request :session :role))
                                                     (= :finance (-> request :session :role)))
                                                 (map #(update-in % [:lawyer_data 0]
                                                                  (fn [c] (into {} (filter
                                                                                     (fn [f] (not (contains? #{:_id :password :verification-code}
                                                                                                            (key f))))
                                                                                     c))))
                                                      (mc/aggregate @db "payment_requests"
                                                                        [{"$lookup" {:from "users"
                                                                                     :localField :lawyer
                                                                                     :foreignField :_id
                                                                                     :as :lawyer_data}}
                                                                         ])))))]
    (response {:payment-requests payment-requests :status "ok" :role (-> request :session :role)})))



(defn get-payment-request [id request]
  (response {:payment-request (mc/find-one-as-map @db "payment-requests" {:_id (oid id)})
             :status "ok" :role (-> request :session :role)}))

(defn create-payment-request [request]
  (let [params (:params request)
        current-user (get-current-user request)
        payment-request (assoc (:params request)
                                          :lawyer (:_id current-user)
                                          :code (util/generate-hash params)
                                          :date_created (new java.util.Date))]
    (dbcore/create-payment-request payment-request)
    (future (email/payment-request-email (:client_email params) {:lawyer-name (:name current-user)
                                                                 :payment-request payment-request
                                                                 :base-url (get (:headers request) "host")}))
    (response {:payment-request {:create "new"}
               :status "ok"
               :role (-> request :session :role)
               :params params})))

(defn update-payment-request [id request]
  (let [id (oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if (true? allowed?)
      (do (mc/update-by-id @db "payment_requests" id {$set
                                                         (assoc (dissoc params :lawyer)
                                                                :date_updated (new java.util.Date)
                                                                :code (util/generate-hash (:params request)))})
          (let [current-user (get-current-user request)
                payment-request (mc/find-one-as-map @db "payment_requests" {:_id id})]
            (println (str "-----" payment-request))
            (future (email/payment-request-email (:client_email payment-request) {:lawyer-name (:name current-user)
                                                                                  :payment-request payment-request
                                                                                  :base-url (get (:headers request) "host")})))
          (response {:payment-request {:update id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))

(defn remove-payment-request [id request]
  (let [id (oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if (true? allowed?)
      (do (mc/remove-by-id @db "payment_requests" id)
          (response {:payment-request {:delete id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))



(defroutes payment-requests-routes
  (GET "/payment-requests" [] (restrict get-payment-requests
                                {:handler {:or [ac/admin-access ac/operator-access ac/lawyer-access ac/finance-access]}
                                 :on-error access-error-handler}))
  (GET "/payment-requests/:id" [id :as request]
       (restrict (fn [request] (get-payment-request id request))
                 {:handler {:or [ac/admin-access ac/operator-access ac/lawyer-access ac/finance-access]}
                  :on-error access-error-handler}))
  (POST "/payment-requests" [] (restrict (fn [request] (create-payment-request request))
                                 {:handler {:or [ac/admin-access ac/lawyer-access ac/finance-access]}
                                  :on-error access-error-handler}))
  (PUT "/payment-requests/:id" [id :as request]
       (restrict (fn [request] (update-payment-request id request))
                 {:handler {:or [ac/admin-access ac/lawyer-access ac/finance-access]}
                  :on-error access-error-handler}))
  (DELETE "/payment-requests/:id" [id :as request]
          (restrict (fn [request] (remove-payment-request id request))
                    {:handler {:or [ac/admin-access ac/lawyer-access ac/finance-access]}
                     :on-error access-error-handler})))
