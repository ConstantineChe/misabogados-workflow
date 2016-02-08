(ns misabogados-workflow.flow-definition
  (:require [misabogados-workflow.flow :refer [->Step ->AutoStep]]
            [postal.core :refer [send-message]]))


(def steps {:check (->Step [:lead :user :basic-info] {:finalize :archive
                                                      :refer :find-lawyer
                                                      :finalize-and-print :archive-and-print
                                                      :finalize-and-send :archive-and-send})
            :find-lawyer (->Step [:lead :user :basic-info :match] {:done :arrange-meeting
                                                                   :finalize :archive})
            :arrange-meeting (->Step [:lead :user :basic-info [:match :meeting]] {:change-lawyer :find-lawyer
                                                                                  :done :archive})
            :archive (->Step [:lead :user :basic-info [:match :meeting]] {:reopen :check})
            :archive-and-print (->AutoStep (fn [lead] (println (str "[AUTOMATIC ACTION]" lead))) :archive)
            :archive-and-send (->AutoStep (fn [lead] (send-message
                                                     {:host "smtp.gmail.com"
                                                      :user "kotya@misabogados.com"
                                                      :pass "misqw15azot"
                                                      :ssl :yes}
                                                     {:from "test@gmail.com"
                                                      :to "kotya@misabogados.com"
                                                      :subject "Test"
                                                      :body (str "Hi, " "Lead was:" lead)})) :archive)})
