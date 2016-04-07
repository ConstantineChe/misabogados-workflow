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
            [config.core :refer [env]]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [monger.joda-time]))

(def payu-test-payment-data {
                        :merchantId "500238"
                        :ApiKey "6u39nqhq8ftd0hlvnjfs66eh8c"
                        :referenceCode "TestPayU"
                        :accountId "500537"
                        ;; :description "Test PAYU"
                        ;; :amount "3"
                        :tax "0"
                        :taxReturnBase "0"
                        :currency (:currency env)
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




(defn get-payment [code request]
  (render "payment.html"
          {:payment-request (mc/find-one-as-map @db "payment_requests" {:code code})
           :payment-options payu-test-payment-data}))

(defmulti get-payment-request-by-payment-code (fn [request] (:payment-system env)))

(defmethod get-payment-request-by-payment-code "webpay" [request]
  (mc/find-one-as-map @db "payment_requests" {:payment_log {$elemMatch {"data.TBK_ORDEN_COMPRA" (-> request :params :TBK_ORDEN_COMPRA)}}}))

(defmulti construct-payment-attempt-form (fn [request payment-request date] (:payment-system env)))

(defmethod construct-payment-attempt-form "payu" [request payment-request date]
  {:form-data (add-signature (merge payu-test-payment-data {:amount (:amount payment-request)
                                                            :referenceCode (str (:_id payment-request) "-" (c/to-long date))
                                                            :description (:service payment-request)
                                                            :buyerEmail (:client_email payment-request)
                                                       }))
   :form-path "https://stg.gateway.payulatam.com/ppp-web-gateway/"})

(defmethod construct-payment-attempt-form "webpay" [request payment-request date]
  {:form-data {:TBK_URL_EXITO (util/full-path request "/payments/success")
               :TBK_URL_FRACASO (util/full-path request "/payments/failure")
               :TBK_TIPO_TRANSACCION "TR_NORMAL"
               :TBK_MONTO (* 100 (:amount payment-request))
               :TBK_ORDEN_COMPRA (str (:_id payment-request) "-" (c/to-long date))}
   :form-path "http://payments.misabogados.com/cljpay/tbk_bp_pago.cgi"})

(defmulti confirm-payment (fn [request] (:payment-system env)))

(defmethod confirm-payment "webpay" [request]
  (let [payment-request (get-payment-request-by-payment-code request)]
    [payment-request "ACEPTADO" ""]))

(defn start-payment-attempt [request]
  (let [params (:params request)
        date (t/now)
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
  (let [params (:params request)
        [payment-request result message] (confirm-payment request)]
    (println (str "----CONFIRM " params) )
    (mc/update @db "payment_requests" {:_id (:_id payment-request)} {$push {:payment_log {:date (t/now)
                                                                                          :action "confirm_payment_attempt"
                                                                                          :result result
                                                                                          :message message
                                                                                          :data params}}})
    result))

(defn failure [request]
    (let [params (:params request)
          payment-request (get-payment-request-by-payment-code request) ]
      (println (str "----FAILURE " params))
      (mc/update @db "payment_requests" {:_id (:_id payment-request)} {$push {:payment_log {:date (t/now)
                                                                                            :action "payment_attempt_failed"
                                                                                            :data params}}})
      (render "payment_failure.html" (:payment-request payment-request))))

(defn success [request]
    (let [params (:params request)
          payment-request (get-payment-request-by-payment-code request) ]
      (println (str "----SUCCESS " params))
      (mc/update @db "payment_requests" {:_id (:_id payment-request)} {$push {:payment_log {:date (t/now)
                                                                                            :action "payment_attempt_succeded"
                                                                                            :data params}}})
      (render "payment_success.html" (:payment-request payment-request))))

(defroutes payments-routes
  (GET "/payments/pay/:code" [code :as request] (get-payment code request))
  ;; (POST "/payments/pay" [code :as request] (pay code request))
  (POST "/payments/pay" []
        start-payment-attempt))

(defroutes payments-integration-routes
  (POST "/payments/confirm" []
        confirm)
  (POST "/payments/failure" []
        failure)
  (POST "/payments/success" []
        success))
