(ns misabogados-workflow.routes.payments
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE]]
            [ring.util.http-response :refer [ok content-type]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect response]]
            [ring.middleware.session :as s]
            [misabogados-workflow.db.core :as db]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.access-control :as ac]
            [misabogados-workflow.middleware :as mw]
            [buddy.auth.accessrules :refer [restrict]]))

(defn get-current-user-id [request]
  (:_id (db/get-user (:identity request))))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-payments [request]
  (let [payments (apply merge (map (fn [payment]
                          {(str (:_id payment)) (dissoc payment :_id)})
                        (db/get-payments (get-current-user-id request))))]
    (response {:payments payments :status "ok" :role (-> request :session :role)})))

(defn get-payment [id request]
  (response {:payment {:get id} :status "ok" :role (-> request :session :role)}))

(defn create-payment [request]
  (db/create-payment (assoc (:params request) :lawyer (get-current-user-id request)))
  (response {:payment {:create "new"}
             :status "ok"
             :role (-> request :session :role)
             :params (:params request)}))

(defn update-payment [id request]
  (response {:payment {:update id} :status "ok" :role (-> request :session :role)}))

(defn remove-payment [id request]
  (response {:payment {:remove id} :status "ok" :role (-> request :session :role)}))

(def payment-data {})

(defn get-pay [code request]
    (layout/blank-page "Pagar"
                       (layout/render-form "Pagar"
                                           ["POST" "https://stg.gateway.payulatam.com/ppp-web-gateway"]
                                           ([:p "test"] 
                                                 [:button.btn.btn-secondary "Pagar"]))))

(defroutes payments-routes
  (GET "/pay/:code" [code :as request] get-pay)
  (GET "/payments" [] (restrict get-payments
                                {:handler {:or [ac/admin-access ac/lawyer-access]}
                                 :on-error access-error-handler}))
  (GET "/payments/:id" [id :as request]
       (restrict (fn [request] (get-payment id request))
                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                  :on-error access-error-handler}))
  (POST "/payments" [] (restrict (fn [request] (create-payment request))
                                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                                  :on-error access-error-handler}))
  (PUT "/payments/:id" [id :as request]
       (restrict (fn [request] (update-payment id request))
                 {:handler {:or [ac/admin-access ac/lawyer-access]}
                  :on-error access-error-handler}))
  (DELETE "/payments/:id" [id :as request]
          (restrict (fn [request] (remove-payment id request))
                    {:handler {:or [ac/admin-access ac/lawyer-access]}
                     :on-error access-error-handler})))
