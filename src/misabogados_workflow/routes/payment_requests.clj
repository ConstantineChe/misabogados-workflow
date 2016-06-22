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
        filters-parsed (util/wrap-datetime (clojure.edn/read-string filters))
        project-fields {"amount" 1 "service" 1 "lawyer" 1 "service_descrition" 1 "client" 1
                        "client_tel" 1 "client_email" 1 "code" 1 "date_created" 1 "own_client" 1 "lawyer_data" 1}

        filter-query (vec (concat
                             (if-let [client (:client filters-parsed)]
                               (if-not (empty? client) [{"$match" {"$or" [{:client {"$regex" client "$options" "-i"}}
                                                                          {:client_email {"$regex" client "$options" "-i"}}]}}]))
                             (if-let [lawyer (:lawyer filters-parsed)]
                               (if-not (empty? lawyer) [{"$match" {"$or" [{:lawyer_data.name {"$regex" lawyer "$options" "-i"}}
                                                                          {:lawyer_data.email {"$regex" lawyer "$options" "-i"}}]}}]))

                             (let [status-pending (true? (:status-pending filters-parsed))
                                   status-in-process (true? (:status-in-process filters-parsed))
                                   status-paid (true? (:status-paid filters-parsed))
                                   status-failed (true? (:status-failed filters-parsed))]
                               (concat []
                                       (if-not status-pending [{"$match" {"last_payment" {"$ne" "pending"}}}])
                                       (if-not status-in-process [{"$match" {"last_payment.action" {"$ne" "start_payment_attempt"}}}])
                                       (if-not status-paid [{"$match" {"last_payment.action" {"$ne" "payment_attempt_succeded"}}}])
                                       (if-not status-failed [{"$match" {"last_payment.action" {"$ne" "payment_attempt_failed"}}}])))
                             (if-not (and (:own-client filters-parsed)
                                          (:misabogados-client filters-parsed))
                               (concat (if-let [own-client (:own-client filters-parsed)]
                                         [{"$match" {:own_client "true"}}])
                                       (if-let [misabogados-client (:misabogados-client filters-parsed)]
                                         [{"$match" {:own_client "false"}}])))
                             (if-let [from-date (:from-date filters-parsed)]
                               [{"$match" {:date_created {"$gte" from-date}}}])
                             (if-let [to-date (:to-date filters-parsed)]
                               [{"$match" {:date_created {"$lte" to-date}}}])))
        _ (prn filters)
        query (vec (concat
                    (if (= :lawyer (-> request :session :role))
                      [{"$match" {:lawyer (get-lawyer-profile-id request)}}])
                    [{"$lookup" {:from "lawyers"
                                 :localField :lawyer
                                 :foreignField :_id
                                 :as :lawyer_data}}]
                    [{"$project" (assoc project-fields
                                        "last_payment" {"$slice" ["$payment_log", -1]})}
                     {"$project" (assoc project-fields
                                        "last_payment" {"$ifNull" ["$last_payment", "pending"]})}]
                    ;; (doall (map second filters))

                    filter-query
                    ))
        pagination [{"$skip" offset}
                     {"$limit" per-page}
                     {"$sort" {sort-field (Integer. sort-dir)}}]
        payment-requests (apply merge (map (fn [payment-request]
                                             {(str (:_id payment-request)) (dissoc payment-request :_id)})

                                           (let [query (vec (concat query pagination))
                                                 reqs (mc/aggregate @db "payment_requests" query)
                                                 ]
                                             (prn "--------QUERY: " query)
                                             ;; (clojure.pprint/pprint reqs)
                                             ;; (map #(update-in % [:lawyer_data 0]
                                             ;;                  (fn [c] (into {} (filter-query
                                             ;;                                   (fn [f] (not (contains? #{:_id :password :verification-code}
                                             ;;                                                          (key f))))
                                             ;;                                   c))))
                                             ;;      reqs)
                                             reqs)))
        payment-requests-summary (reduce (fn [acc elem]
                                           (let [amount (reduce #(+ %1 (Long. (:amount %2))) 0 (:amounts elem))]
                                             (case (-> elem :_id :status)
                                               nil (assoc acc "pending" amount)
                                               ["start_payment_attempt"] (assoc acc "start_payment_attempt"
                                                                                amount)
                                               ["payment_attempt_failed"] (assoc acc "payment_attempt_failed" amount)
                                               ["payment_attempt_succeded"] (assoc acc "payment_attempt_succeded" amount))))
                                         {}
                                         (mc/aggregate @db "payment_requests"
                                                       (vec (concat
                                                             (if (= :lawyer (-> request :session :role))
                                                               [{"$match" {:lawyer (get-lawyer-profile-id request)}}])
                                                             [{"$project" (assoc project-fields
                                                                                 "last_payment" {"$slice" ["$payment_log", -1]})}]
                                                             [{"$group" {:_id {:status "$last_payment.action"}
                                                                         :amounts {"$push" {:amount "$amount"}}}}]))))
        payment-requests-count (mc/aggregate @db "payment_requests"
                                             (vec (concat query
                                                          [{"$group" {:_id nil :count {"$sum" 1}}}])))]
    ;; (prn "PRS~~~" payment-requests-summary)
    (response {:payment-requests payment-requests
               :count (-> payment-requests-count first :count)
               :payment-requests-summary payment-requests-summary
               :status "ok" :role (-> request :session :role)})))

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
                                {:handler {:or [ac/admin-access
                                                ;; ac/operator-access
                                                ac/lawyer-access ac/finance-access]}
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
  (GET "/payment-requests/js/options" []
       (restrict (fn [request] (response {:lawyer (map #((juxt (fn [x] (str (:name x) " (" (:email x) ")")) :_id) %) (mc/find-maps @db "lawyers"))
                                          :own_client [["" :empty] ["Cliente propio" true] ["Cliente MisAbogados" false]]}))
                 {:handler {:or [ac/admin-access ac/lawyer-access ac/finance-access]}
                  :on-error access-error-handler})))
