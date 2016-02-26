(ns misabogados-workflow.routes.payments
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as db]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.middleware :as mw]
            [misabogados-workflow.util :as util]
            [buddy.auth.accessrules :refer [restrict]]))

(defn get-current-user-id [request]
  (:_id (db/get-user (:identity request))))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn allowed-to-edit [id request]
  (if (= (:lawyer (mc/find-one-as-map @db/db "payment_requests" {:_id id}))
         (get-current-user-id request))
    true
    {:message "Not allowed"}))

(defn get-payment-requests [request]
  (let [payment-requests (apply merge (map (fn [payment-request]
                          {(str (:_id payment-request)) (dissoc payment-request :_id)})
                        (db/get-payment-requests (get-current-user-id request))))]
    (response {:payment-requests payment-requests :status "ok" :role (-> request :session :role)})))



(defn get-payment-request [id request]
  (response {:payment-request {:get id} :status "ok" :role (-> request :session :role)}))

(defn create-payment-request [request]
  (db/create-payment-request (assoc (:params request) :lawyer (get-current-user-id request) :code (util/generate-hash (:params request))))
  (response {:payment-request {:create "new"}
             :status "ok"
             :role (-> request :session :role)
             :params (:params request)}))

(defn update-payment-request [id request]
  (let [id (db/oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if (true? allowed?)
      (do (mc/update-by-id @db/db "payment_requests" id {$set (dissoc params :lawyer)})
          (response {:payment-request {:update id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))

(defn remove-payment-request [id request]
  (let [id (db/oid id)
        params (:params request)
        allowed? (allowed-to-edit id request)]
    (if (true? allowed?)
      (do (mc/remove-by-id @db/db "payment_requests" id)
          (response {:payment-request {:delete id} :status "ok" :role (-> request :session :role)}))
      {:status 403
       :header {}
       :body {:error (:message allowed?)}})))

(def test-payment-data {
                        :merchantId "500238"
                        :ApiKey "6u39nqhq8ftd0hlvnjfs66eh8c"
                        :referenceCode "TestPayU"
                        :accountId "500537"
                        :description "Test PAYU"
                        ;; :amount "3"
                        :tax "0"
                        :taxReturnBase "0"
                        :currency "MXN"
                        :signature "be2f083cb3391c84fdf5fd6176801278"
                        :test "1"
                        ;; :buyerEmail "test@test.com"
                        })

(defn get-payment [code request]
    (layout/blank-page "Pagar"
                       (let [payment-request (db/get-payment-request-by-code code)]
                         (list [:ul
                                ]
                               [:form {:method "POST" :action "https://stg.gateway.payulatam.com/ppp-web-gateway"}

                                  (list
                                   (map (fn [field] [:input {:type :hidden
                                                             :name (key field)
                                                             :value (val field)}])
                                        (merge test-payment-data {:amount (:amount payment-request)
                                                                  :buyerEmail (:client_email payment-request)}))

                                   [:div.col-md-4.col-md-offset-4
                                    [:div.panel.panel-info
                                     [:div.panel-heading
                                      [:h3.text-center (:service payment-request)]
                                      [:p.text-center (:service_description payment-request)]]
                                     [:div.panel-body.text-center
                                      [:p.lead (str (:currency test-payment-data) " " (:amount payment-request))]]
                                     [:ul.list-group.list-group-flush.text-center
                                      (map (fn [elem] [:li.list-group-item [:b (str (name (key elem)) ": ")] (val elem)])
                                           (select-keys payment-request [:client :client_email :lawyer]))]
                                     [:div.panel-footer
                                      [:button.btn.btn-lg.btn-block.btn-info "Pagar"]
                                      ]]])]))))

(defroutes payments-routes
  (GET "/payments/:code" [code :as request] (get-payment code request))

  (GET "/payment-requests" [] (restrict get-payment-requests
                                {:handler {:or [ac/admin-access ac/lawyer-access]}
                                 :on-error access-error-handler}))
  (GET "/payment-requests/:id" [id :as request]
       (restrict (fn [request] (get-payment-request id request))
                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                  :on-error access-error-handler}))
  (POST "/payment-requests" [] (restrict (fn [request] (create-payment-request request))
                                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                                  :on-error access-error-handler}))
  (PUT "/payment-requests/:id" [id :as request]
       (restrict (fn [request] (update-payment-request id request))
                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                  :on-error access-error-handler}))
  (DELETE "/payment-requests/:id" [id :as request]
          (restrict (fn [request] (remove-payment-request id request))
                    {:handler {:or [ac/admin-access ac/lawyer-access]}
                     :on-error access-error-handler})))
