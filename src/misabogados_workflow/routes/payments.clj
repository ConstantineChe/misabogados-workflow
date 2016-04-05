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
           :body {:form-data data
                  :form-path "https://stg.gateway.payulatam.com/ppp-web-gateway/"}})
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
        confirmation))
