(ns misabogados-workflow.flow-definition
  #?(:clj (:require [misabogados-workflow.auto-actions :refer :all])))

#?(:cljs (declare change-lawyer schedule-meeting))


(def steps {:pitch  {:type :manual
                     :render-attributes {:lead {:fields :all}}
                     :actions [{:name "Declinar"
                                :action :case-closed
                                :roles #{:admin :operator}}
                               {:name "Referir"
                                :action :person-enterprise-switch
                                :roles #{:admin :operator}}
                               ]
                     :description "Llamar al cliente, hacer pitch, derivar o declinar lead."}
            :person-enterprise-switch {:type :auto :auto-action person-enterprise-switch :endpoint :refer}

            :refer {:type :manual
                    :render-attributes {:lead {:fields :readonly :matches {:fields :all :meetings {:fields :all}}}}
                    :actions [{:name "Reunion agendada"
                               :action :schedule-meeting
                               :roles #{:admin :operator}}
                              {:name "Cerrar caso"
                               :action :case-closed
                               :roles #{:admin :operator}}]
                    :description "Derivar el caso al abogado especialista, confirmar con abogado y cliente, agendar reunión"}

            :meeting-scheduled {:type :manual
                                :render-attributes {:lead {:fields :readonly :matches {:fields :readonly :meetings {:fields :all}}}}
                                :actions [{:name "Referar de nuevo"
                                           :action :change-lawyer
                                           :roles #{:admin :operator}}
                                          {:name "Reunión reagendada"
                                           :action :meeting-scheduled
                                           :roles #{:admin :operator}}
                                          {:name "Contrato firmado"
                                           :action :contract-signed
                                           :roles #{:admin :operator}}
                                          {:name "Cerrar caso"
                                           :action :case-closed
                                           :roles #{:admin :operator}}]
                                :description "TODO description"}

            :meeting-happened {:type :manual
                               :render-attributes {:lead {:fields :readonly :matches {:fields :readonly :meetings {:fields :all}}}}
                               :actions [{:name "Referar de nuevo"
                                          :action :change-lawyer
                                          :roles #{:admin :operator}}
                                         {:name "Reunión reagendada"
                                          :action :meeting-scheduled
                                          :roles #{:admin :operator}}
                                         {:name "Contrato firmado"
                                          :action :contract-signed
                                          :roles #{:admin :operator}}
                                         {:name "Cerrar caso"
                                          :action :case-closed
                                          :roles #{:admin :operator}}]
                                :description "TODO description"}

            :contract-signed {:type :manual
                               :render-attributes {:lead {:fields :readonly :matches {:fields :readonly :meetings {:fields :all}}}}
                               :actions [{:name "Referar de nuevo"
                                          :action :change-lawyer
                                          :roles #{:admin :operator}}
                                         {:name "Reunión reagendada"
                                          :action :meeting-scheduled
                                          :roles #{:admin :operator}}
                                         {:name "Generar cupon de pago"
                                          :action :generate-payment-request
                                          :roles #{:admin :operator}}
                                         {:name "Cerrar caso"
                                          :action :case-closed
                                          :roles #{:admin :operator}}]
                                :description "TODO description"}

            :case-closed {:type :manual
                          :render-attributes :all
                          :actions [{:name "Reopen"
                                     :action :pitch
                                     :roles #{:admin :operator}}]
                      :description "Archive description"}
            :change-lawyer {:type :auto :auto-action change-lawyer :endpoint :refer}
            :schedule-meeting {:type :auto :auto-action schedule-meeting :endpoint :meeting-scheduled}})
