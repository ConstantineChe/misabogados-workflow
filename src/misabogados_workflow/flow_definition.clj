(ns misabogados-workflow.flow-definition)



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
                                                 :roles #{:admin :operator}}]]
            :find-lawyer [[:lead :user :basic-info :match] [{:name "Done"
                                                             :action :arrange-meeting
                                                             :roles #{:admin :operator}}
                                                            {:name "Finalize"
                                                             :action :archive
                                                             :roles #{:admin :operator}}]]
            :arrange-meeting [[:lead :user :basic-info [:match :meeting]] [{:name "Change lawyer"
                                                                            :action :find-lawyer
                                                                            :roles #{:admin :operator}}
                                                                           {:name "Done"
                                                                            :action :archive
                                                                            :roles #{:admin :operator}}]]
            :archive [[:lead :user :basic-info [:match :meeting]] [{:name "Reopen"
                                                                    :action :check
                                                                    :roles #{:admin}}]]
            :archive-and-print [(fn [lead] (println (str "[AUTOMATIC ACTION]" lead))) :archive]
            :archive-and-send [prn :archive]})
