(ns misabogados-workflow.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]
            [misabogados-workflow.handler :refer :all]
            [misabogados-workflow.db.core :as db]
            [ring-test.core :refer [run-ring-app]]
            [misabogados-workflow.test.util :refer [*test-role* *test-csrf*]]))

(db/connect!)

(dissoc (run-ring-app app (request :get "/") (request :get "/")) :body)


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
    (let [response (app (request :get "/users"))]
      (is (= 403 (:status response)))))
  (testing "Get user"
    (let [response (app (request :get "/user"))]
      (is (= 200 (:status response))))))
