(ns misabogados-workflow.flow-definition
  (:require [misabogados-workflow.flow :refer [->Step ->AutoStep]]))


(def steps {:check (->Step [:lead :user :basic-info] {:finalize :archive
                                                      :refer :find-lawyer
                                                      :test-auto :archive-and-print})
            :find-lawyer (->Step [:lead :user :basic-info :match] {:done :arrange-meeting
                                                                   :finalize :archive})
            :arrange-meeting (->Step [:lead :user :basic-info [:match :meeting]] {:change-lawyer :find-lawyer
                                                                                  :done :archive})
            :archive (->Step [:lead :user :basic-info [:match :meeting]] {:reopen :check})
            :archive-and-print (->AutoStep (fn [lead] (println (str "[AUTOMATIC ACTION]" lead))) :archive)})
