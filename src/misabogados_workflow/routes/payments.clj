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

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-payments [request]
  (response {:payments {} :status "ok" :role (-> request :session :role)}))

(defn get-payment [id request]
  (response {:payment {:get id} :status "ok" :role (-> request :session :role)}))

(defn create-payment [request]
  (response {:payment {:create "new"} :status "ok" :role (-> request :session :role)}))

(defn update-payment [id request]
  (response {:payment {:update id} :status "ok" :role (-> request :session :role)}))

(defn remove-payment [id request]
  (response {:payment {:remove id} :status "ok" :role (-> request :session :role)}))

(defroutes payments-routes
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
