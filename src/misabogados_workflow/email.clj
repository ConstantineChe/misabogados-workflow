(ns misabogados-workflow.email
  (:require [postal.core :refer [send-message]]
            [selmer.parser :as parser]))

(def settings {:host "smtp.mandrillapp.com"
               :user "panduro@misabogados.com"
               :pass "4FCHR1oN9NUam7sBXzgHEw"
               :ssl :yes

               })

(defn payment-request-email [email data]
  (send-message settings {:from "no-reply@misabogados.com"
                          :to email
                          :subject (str (:lawyer data) " Pago Servicios " (:service data))
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file "payment-request.html" data)}]}))
