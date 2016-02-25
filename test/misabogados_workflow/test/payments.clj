(ns misabogados-workflow.test.payments
  (:require [ring.mock.request :refer :all]
            [midje.sweet :refer :all]
            [misabogados-workflow.db.core :as db]
            [misabogados-workflow.routes.payments :as payments]))
(db/connect!)

(fact "get payments returns array of ids to data"
      (type (:payments
             (:body
              (payments/get-payment-requests
               {:identity "che.constantine@gmail.com"})))) =>  (type {})
      )
(fact "keys are strings"
      (-> (:payments
           (:body
            (payments/get-payment-requests
             {:identity "che.constantine@gmail.com"}))) keys first string?) => true)
