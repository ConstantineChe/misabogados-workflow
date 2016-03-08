(ns misabogados-workflow.routes.payments
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
          (= (:lawyer (mc/find-one-as-map @db "payment_requests" {:_id id}))
             (get-current-user-id request)))
    true
    {:message "Not allowed"}))

(defn get-payment-requests [request]
  (let [payment-requests (apply merge (map (fn [payment-request]
                          {(str (:_id payment-request)) (dissoc payment-request :_id)})
                                           (cond (= :lawyer (-> request :session :role))
                                                 (dbcore/get-payment-requests (get-current-user-id request))
                                                 (= :admin (-> request :session :role))
                                                 (mc/aggregate @db "payment_requests"
                                                               [{"$lookup" {:from "users"
                                                                          :localField :lawyer
                                                                          :foreignField :_id
                                                                          :as :lawyer_data}}]))))]
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

(def test-payment-data {
                        :merchantId "500238"
                        :ApiKey "6u39nqhq8ftd0hlvnjfs66eh8c"
                        :referenceCode "TestPayU"
                        :accountId "500537"
                        ;; :description "Test PAYU"
                        ;; :amount "3"
                        :tax "0"
                        :taxReturnBase "0"
                        :currency "MXN"
                        ;; :signature "be2f083cb3391c84fdf5fd6176801278"
                        :test "1"
                        :confirmationUrl "http://localhost:3000/payments/confirmation"
                        ;; :buyerEmail "test@test.com"
                        })

(defn- add-signature [data]
  (let [signature-string (clojure.string/join "~" [(:ApiKey data)
                                                   (:merchantId data)
                                                   (:referenceCode data)
                                                   (:amount data)
                                                   (:currency data)
                                    ])
        signature (util/md5 signature-string)]
    (println (str "~~~SIGNATURE-STRING " signature-string))
    (assoc data :signature signature)))




(defn get-payment-hiccup [code request]
    (layout/blank-page "Pagar"
                       (let [payment-request (dbcore/get-payment-request-by-code code)]
                         (list [:ul
                                ]
                               [:form {:method "POST" :action "https://stg.gateway.payulatam.com/ppp-web-gateway"}
                                [:input {:type :hidden
                                         :name :code
                                         :value code}]
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
                                   ]]]]))))

(defn get-payment [code request]
  (render "payment.html"
          {:payment-request (mc/find-one-as-map @db "payment_requests" {:code code})
           :payment-options test-payment-data}))

(defn start-payment-attempt [request]
  (let [params (:params request)
        date (new java.util.Date)
        id (oid (:_id params))
        payment-request (mc/find-one-as-map @db "payment_requests" {:_id id})
        data (add-signature (merge test-payment-data {:amount (:amount payment-request)
                                                      :referenceCode (str (:_id payment-request) "-" (.getTime date))
                                                      :description (:service payment-request)
                                                      :buyerEmail (:client_email payment-request)
                                                      }))]
    (if (= (:code params) (:code payment-request))
      (do
        (mc/update @db "payment_requests" {:_id (:_id payment-request)} {$push {:payment_log {:date date
                                                                                              :action "start_payment_attempt"
                                                                                              :data data}}})
          {:status 200
           :body data})
      {:status 409
       :body {:error "Codigo de pago no es valido. Probablemente el comprobante ha sido cambiado, el enlace nuevo debe ser en su correo."}}
      )))

(defn confirmation [request]
  (let [params (:params request)
        ]
    (println (str "----CONFIRMATION " params) )
    (redirect "/")))

(defroutes payments-routes
  (GET "/payments/:code" [code :as request] (get-payment code request))
  ;; (POST "/payments/pay" [code :as request] (pay code request))

  (POST "/payments/pay" []
        start-payment-attempt)
  (POST "/payments/confirmation" []
        confirmation)

  (GET "/payment-requests" [] (restrict get-payment-requests
                                {:handler {:or [ac/admin-access ac/operator-access ac/lawyer-access]}
                                 :on-error access-error-handler}))
  (GET "/payment-requests/:id" [id :as request]
       (restrict (fn [request] (get-payment-request id request))
                 {:handler {:or [ac/admin-access ac/operator-access ac/lawyer-access]}
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
