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
            [misabogados-workflow.email :as email]))

(def payu-test-payment-data {
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
                                   [:p.lead (str (:currency payu-test-payment-data) " " (:amount payment-request))]]
                                  [:ul.list-group.list-group-flush.text-center
                                   (map (fn [elem] [:li.list-group-item [:b (str (name (key elem)) ": ")] (val elem)])
                                        (select-keys payment-request [:client :client_email :lawyer]))]
                                  [:div.panel-footer
                                   [:button.btn.btn-lg.btn-block.btn-info "Pagar"]
                                   ]]]]))))

(defn get-payment [code request]
  (render "payment.html"
          {:payment-request (mc/find-one-as-map @db "payment_requests" {:code code})
           :payment-options payu-test-payment-data}))


(defmulti construct-payment-attempt-form (fn [request payment-request date] :webpay))

(defmethod construct-payment-attempt-form :payu [request payment-request date]
  {:form-data (add-signature (merge payu-test-payment-data {:amount (:amount payment-request)
                                                            :referenceCode (str (:_id payment-request) "-" (.getTime date))
                                                            :description (:service payment-request)
                                                            :buyerEmail (:client_email payment-request)
                                                       }))
   :form-path "https://stg.gateway.payulatam.com/ppp-web-gateway/"})

(defmethod construct-payment-attempt-form :webpay [request payment-request date]
  {:form-data {:TBK_URL_EXITO (util/full-path request "/payments/success")
               :TBK_URL_FRACASO (util/full-path request "/payments/failure")
               :TBK_TIPO_TRANSACCION "TR_NORMAL"
               :TBK_MONTO (* 100 (:amount payment-request))
               :TBK_ORDEN_COMPRA (str (:_id payment-request) "-" (.getTime date))}
   :form-path "http://payments.misabogados.com/webpay/tbk_bp_pago.cgi"})

(defmulti confirm-payment (fn [request payment-request] :webpay))

(defmethod confirm-payment :webpay [request payment-request]
  "ACEPTADO")

(defn start-payment-attempt [request]
  (let [params (:params request)
        date (new java.util.Date)
        id (oid (:_id params))
        payment-request (mc/find-one-as-map @db "payment_requests" {:_id id})
        form (construct-payment-attempt-form request payment-request date)
        data (:form-data form)]
    (clojure.pprint/pprint form)
    (if (= (:code params) (:code payment-request))
      (do
        (mc/update @db "payment_requests" {:_id (:_id payment-request)} {$push {:payment_log {:date date
                                                                                              :action "start_payment_attempt"
                                                                                              :data data}}})
          {:status 200
           :body form})
      {:status 409
       :body {:error "Codigo de pago no es valido. Probablemente el comprobante ha sido cambiado, el enlace nuevo debe ser en su correo."}}
      )))

(defn confirm [request]
  (let [params (:params request)]

    "ACEPTADO"))

(defn failure [request]
  (let [params (:params request)]
    (println (str "----FAILURE " params) )
    "FAIL"))

(defn success [request]
  (let [params (:params request)]
    (println (str "----SUCCESS " params) )
    "SUCCESS"))

(defroutes payments-routes
  (GET "/payments/pay/:code" [code :as request] (get-payment code request))
  ;; (POST "/payments/pay" [code :as request] (pay code request))
  (POST "/payments/pay" []
        start-payment-attempt)
  (POST "/payments/confirm" []
        confirm)
  (POST "/payments/failure" []
        failure)
  (POST "/payments/success" []
        success))
