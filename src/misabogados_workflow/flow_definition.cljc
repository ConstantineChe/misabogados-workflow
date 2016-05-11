(ns misabogados-workflow.flow-definition
  #?(:clj (:require [misabogados-workflow.auto-actions :refer [change-lawyer schedule-meeting]])))

#?(:cljs (declare change-lawyer schedule-meeting))


(def steps {:check  {:type :manual
                     :render-attributes {:lead {:fields :all}}
                     :actions [{:name "Finalize"
                                :action :archive
                                :roles #{:admin :operator}}
                               {:name "Refer"
                                :action :find-lawyer
                                :roles #{:admin :operator}}
                               {:name "Finalize and print"
                                :action :archive-and-print
                                :roles #{:admin :operator}}
                               {:name "Finalize and send"
                                :action :archive-and-send
                                :roles #{:admin :operator}}]
                     :description "Check description"}
            :find-lawyer {:type :manual
                          :render-attributes {:lead {:fields :readonly :matches {:fields :all}}}
                          :actions [{:name "Done"
                                       :action :arrange-meeting
                                       :roles #{:admin :operator}}
                                      {:name "Finalize"
                                       :action :archive
                                       :roles #{:admin :operator}}]
                          :description "Find lawyer description"}
            :arrange-meeting {:type :manual
                              :render-attributes {:lead {:fields :readonly :matches {:fields :readonly :meetings {:fields :all}}}}
                              :actions [{:name "Change lawyer"
                                         :action :change-lawyer
                                         :roles #{:admin :operator}}
                                        {:name "Done"
                                         :action :schedule-meeting
                                         :roles #{:admin :operator}}]
                              :description "Arrange meeting description"}
            :archive {:type :manual
                      :render-attributes :all
                      :actions [{:name "Reopen"
                                 :action :check
                                 :roles #{:admin}}]
                      :description "Archive description"}
            :change-lawyer {:type :auto :auto-action change-lawyer :endpoint :find-lawyer}
            :schedule-meeting {:type :auto :auto-action schedule-meeting :endpoint :archive}})
