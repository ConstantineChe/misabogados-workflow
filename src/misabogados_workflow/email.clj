(ns misabogados-workflow.email
  (:require [postal.core :refer [send-message]]
            [selmer.parser :as parser]
            [misabogados-workflow.settings :as settings]
            [clojure.tools.logging :as log]))

(def settings {:host "smtp.mandrillapp.com"
               :user "panduro@misabogados.com"
               :pass "4FCHR1oN9NUam7sBXzgHEw"
               :ssl :yes

               })

(defn payment-request-email [email data]
  (clojure.pprint/pprint (merge data {:settings @settings/settings}))
  (send-message settings {:from "no-reply@misabogados.com"
                          :to email
                          :subject (str (:lawyer data) " Pago Servicios " (:service data))
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file "payment-request.html" (merge data {:settings @settings/settings}))}]}))

(defn contact-email [data]
  (send-message settings {:from "no-reply@misabogados.com"
                          ;; :to (:client_email data)
                          :to "contacto@misabogados.com"
                          :subject (str "Buscar abogado " (:client_email data))
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file "contact_email.html" {:values data})}]}))

(defn verification-email [user]
  (send-message settings {:from "no-reply@misabogados.com"
                          :to (:email user)
                          :subject "Verification email"
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file "verification_email.html" user)}]})
  )

(defn reset-password-email [user link]
  (send-message settings {:from "no-reply@misabogados.com"
                          :to (:email user)
                          :subject "Verification email"
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file "reset-password_email.html" (merge user {:reset-link link}))}]}))

(defn send-email [{:keys [email subject template data]}]
  (log/info "Sending email to: " email )
  (send-message settings {:from "no-reply@misabogados.com"
                          :to email
                          :subject subject
                          :body [{:type "text/html; charset=utf-8"
                                  :content (parser/render-file template data)}]}))
