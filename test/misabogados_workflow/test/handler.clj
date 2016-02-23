(ns misabogados-workflow.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]
            [misabogados-workflow.handler :refer :all]
            [misabogados-workflow.db.core :as db]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [misabogados-workflow.test.util :refer [*test-role*]]))

(db/connect!)


(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "Dashboard"
    (let [response (app (request :get "/leads"))]
      (is (= 200 (:status response)))))

  (testing "Create lead"
    (let [response (app (request :get "/leads/create"))]
      (is (= 200 (:status response))))))

(deftest test-users-routes
  (testing "Get users"
    (let [response (app (request :get "/users"))
          admin-response (app (binding [*test-role* :admin] (request :get "/users")))]
      (is (= 403 (:status response)))
      (is (= 200 (:status admin-response)))))
  (testing "Get user"
    (let [response (app (request :get "/user"))]
      (is (= 200 (:status response))))))

(deftest test-payments-routes
  (testing "Get payments"
    (let [admin-response (binding [*test-role* :admin]
                           (app (request :get "/payments")))
          norole-response (app (request :get "/payments"))]
      (is (= 200 (:status admin-response)))
      (is (= 403 (:status norole-response))))
    )
  (testing "Create payment"
    (let [admin-response (binding [*test-role* :admin]
                           (app (request :post "/payments")))
          norole-response (app (request :post "/payments"))]
      (is (= 200 (:status admin-response)))
      (is (= 403 (:status norole-response))))
    )
  (testing "Get payment"
    (let [admin-response (binding [*test-role* :admin]
                           (app (request :get "/payments/test")))
          norole-response (app (request :get "/payments/test"))]
      (is (= 200 (:status admin-response)))
      (is (= 403 (:status norole-response))))
    )
  (testing "Update payment"
    (let [admin-response (binding [*test-role* :admin]
                           (app (request :put "/payments/test")))
          norole-response (app (request :put "/payments/test"))]
      (is (= 200 (:status admin-response)))
      (is (= 403 (:status norole-response))))
    )
  (testing "Delete payment"
    (let [admin-response (binding [*test-role* :admin]
                           (app (request :delete "/payments/test")))
          norole-response (app (request :delete "/payments/test"))]
      (is (= 200 (:status admin-response)))
      (is (= 403 (:status norole-response))))
  )
)
