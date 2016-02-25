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

(defn get-payment-requests [request]
  (let [payment-requests (apply merge (map (fn [payment-request]
                          {(str (:_id payment-request)) (dissoc payment-request :_id)})
                        (db/get-payment-requests (get-current-user-id request))))]
    (response {:payment-requests payment-requests :status "ok" :role (-> request :session :role)})))



(defn get-payment-request [id request]
  (response {:payment-request {:get id} :status "ok" :role (-> request :session :role)}))

(defn create-payment-request [request]
  (db/create-payment-request (assoc (:params request) :lawyer (get-current-user-id request)))
  (response {:payment-request {:create "new"}
             :status "ok"
             :role (-> request :session :role)
             :params (:params request)}))

(defn update-payment-request [id request]
  (response {:payment-request {:update id} :status "ok" :role (-> request :session :role)}))

(defn remove-payment-request [id request]
  (response {:payment-request {:remove id} :status "ok" :role (-> request :session :role)}))

(defroutes payments-routes
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
