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

(defn get-lawyer-profile [request]
  (if (= (-> request :session :role) :admin)
    (mc/find-one-as-map @db "lawyers" {:_id (oid (-> request :params :lawyer))})
    (if-let  [lawyer-profile (-> request :identity dbcore/get-user :lawyer_profile)]
      (mc/find-one-as-map @db "lawyers" {:_id lawyer-profile})
      (if-let [lawyer-profile (mc/find-one-as-map @db "lawyers" {:email (:identity request)})]
        (do (mc/update @db "users" {:email (:identity request)}
                       {$set {:lawyer_profile (:_id lawyer-profile)}})
            lawyer-profile)
        (let [user (dbcore/get-user (:identity request))
              lawyer-profile (mc/insert-and-return @db "lawyers" (select-keys user [:name :email]))]
          (mc/update @db "users" {:email (:identity request)}
                     {$set {:lawyer_profile (:_id lawyer-profile)}})
          lawyer-profile)
        ))))

(defn get-lawyer-profile-id [request]
  (:_id (get-lawyer-profile request)))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn allowed-to-edit [id request]
  (if (or (= :admin (-> request :session :role))
          (= :finance (-> request :session :role))
          (= (:lawyer (mc/find-one-as-map @db "payment_requests" {:_id id}))
             (get-lawyer-profile-id request)))
    true
    {:message "Not allowed"}))

(defn get-payment-requests [request]
  (let [{:keys [page per-page filters sort-field sort-dir]} (:params request)
        per-page (Integer. per-page)
        offset (* per-page (dec (Integer. page)))
        payment-requests (apply merge (map (fn [payment-request]
                          {(str (:_id payment-request)) (dissoc payment-request :_id)})
                                           (cond (= :lawyer (-> request :session :role))
                                                 (dbcore/get-payment-requests (get-lawyer-profile-id request)
                                                                              (util/wrap-datetime filters)
                                                                              page per-page offset sort-dir sort-field)
                                                 (or (= :admin (-> request :session :role))
                                                     (= :finance (-> request :session :role)))
                                                 (let [reqs (mc/aggregate @db "payment_requests"
                                                                               (vec (concat
                                                                                     [{"$lookup" {:from "lawyers"
                                                                                                  :localField :lawyer
                                                                                                  :foreignField :_id
                                                                                                  :as :lawyer_data}}]
                                                                                     (doall (map second
                                                                                                 (util/wrap-datetime filters)))
                                                                                     [{"$skip" offset}
                                                                                      {"$limit" per-page}
                                                                                      {"$sort" {sort-field (Integer. sort-dir)}}]
                                                                                     )))]
                                                   (map #(update-in % [:lawyer_data 0]
                                                                    (fn [c] (into {} (filter
                                                                                     (fn [f] (not (contains? #{:_id :password :verification-code}
                                                                                                            (key f))))
                                                                                     c))))
                                                        reqs)))))
        reqs-count (mc/aggregate @db "payment_requests"
                                 (vec (concat
                                       (if-not (= (-> request :session :role) :admin)
                                         [{"$match" {:lawyer_id (get-lawyer-profile-id request)}}])
                                       (doall (map second filters))
                                       [{"$group" {:_id nil :count {"$sum" 1}}}]
                                       )))]
    (response {:payment-requests payment-requests :count reqs-count :status "ok" :role (-> request :session :role)})))



(defn get-payment-request [id request]
  (response {:payment-request (mc/find-one-as-map @db "payment-requests" {:_id (oid id)})
             :status "ok" :role (-> request :session :role)}))

(defn create-payment-request [request]
  (let [params (:params request)
        current-user (get-lawyer-profile request)
        payment-request (-> (assoc (:params request)
                                   :lawyer (:_id current-user)
                                   :code (util/generate-hash params)
                                   :date_created (new java.util.Date)))]
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
          (let [current-user (get-lawyer-profile request)
                payment-request (mc/find-one-as-map @db "payment_requests" {:_id id})]
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
                     :on-error access-error-handler}))
  (GET "/payment-requests/js/options" [] (restrict (fn [request] (response {:lawyer (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db "lawyers"))
                                                                           :own_client [["" :empty] ["Cliente propio" true] ["Cliente MisAbogados" false]]}))
                                    {:handler {:or [ac/admin-access ac/lawyer-access ac/finance-access]}
                                     :on-error access-error-handler})))
