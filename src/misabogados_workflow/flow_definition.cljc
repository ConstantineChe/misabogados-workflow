(ns misabogados-workflow.flow-definition
  #?(:clj (:require [misabogados-workflow.auto-actions :refer [change-lawyer schedule-meeting]])))

#?(:cljs (declare change-lawyer schedule-meeting))


(def steps {:check  [[:lead :user :basic-info] [{:name "Finalize"
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
                     "Check description"]
            :find-lawyer [[:lead :user :basic-info :match] [{:name "Done"
                                                             :action :arrange-meeting
                                                             :roles #{:admin :operator}}
                                                            {:name "Finalize"
                                                             :action :archive
                                                             :roles #{:admin :operator}}]
                          "Find lawyer description"]
            :arrange-meeting [[:lead :user :basic-info [:match :meeting]] [{:name "Change lawyer"
                                                                            :action :change-lawyer
                                                                            :roles #{:admin :operator}}
                                                                           {:name "Done"
                                                                            :action :schedule-meeting
                                                                            :roles #{:admin :operator}}]
                              "Arrange meeting description"]
            :archive [[:lead :user :basic-info [:match :meeting]] [{:name "Reopen"
                                                                    :action :check
                                                                    :roles #{:admin}}]
                      "Archive description"]
            :change-lawyer [change-lawyer :find-lawyer]
            :schedule-meeting [schedule-meeting :archive]})
