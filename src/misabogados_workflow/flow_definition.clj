(ns misabogados-workflow.flow-definition
  (:require [misabogados-workflow.flow :refer [->Step ->AutoStep]]
            [postal.core :refer [send-message]]))

(defn send-mail [lead]
  (send-message {:host "smtp.gmail.com"
                 :user "kotya@misabogados.com"
                 :pass "misqw15azot"
                 :ssl :yes}
                {:from "kebab@misabogados.com"
                 :to (-> lead :user :email)
                 :subject "Test"
                 :body (str "Hi, " (-> lead :user :name) " Lead was:" lead)}))

(def steps {:check (->Step [:lead :user :basic-info] [{:name :finalize
                                                       :action :archive
                                                       :roles #{:admin :operator}}
                                                      {:name :refer
                                                       :action :find-lawyer
                                                       :roles #{:admin :operator}}
                                                      {:name :finalize-and-print
                                                       :action :archive-and-print
                                                       :roles #{:admin :operator}}
                                                      {:name :finalize-and-send
                                                       :action :archive-and-send
                                                       :roles #{:admin :operator}}])
            :find-lawyer (->Step [:lead :user :basic-info :match] [{:name :done
                                                                    :action :arrange-meeting
                                                                    :roles #{:admin :operator}}
                                                                   {:name :finalize
                                                                    :action :archive
                                                                    :roles #{:admin :operator}}])
            :arrange-meeting (->Step [:lead :user :basic-info [:match :meeting]] [{:name :change-lawyer
                                                                                   :action :find-lawyer
                                                                                   :roles #{:admin :operator}}
                                                                                  {:name :done
                                                                                   :action :archive
                                                                                   :roles #{:admin :operator}}])
            :archive (->Step [:lead :user :basic-info [:match :meeting]] [{:name :reopen
                                                                           :action :check
                                                                           :roles #{:admin}}])
            :archive-and-print (->AutoStep (fn [lead] (println (str "[AUTOMATIC ACTION]" lead))) :archive)
            :archive-and-send (->AutoStep send-mail :archive)})
