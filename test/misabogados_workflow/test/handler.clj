(ns misabogados-workflow.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]
            [misabogados-workflow.handler :refer :all]
            [misabogados-workflow.flow :refer [->Step]]
            [misabogados-workflow.db.core :as db]))

(db/connect!)

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "dashboard"
    (let [response (app (request :get "/leads"))]
      (is (= 200 (:status response)))))

  (testing "create lead"
    (let [response (app (request :get "/leads/create"))]
      (is (= 200 (:status response))))))


(fact "test-test"
      (+ 2 2) => 4)
(fact  (list? (.create-form (->Step [:lead :user] []) {:lead {:user {:name "name1" :etc "etc1"}}})) => true)
