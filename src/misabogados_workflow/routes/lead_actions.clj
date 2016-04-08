(ns misabogados-workflow.routes.lead-actions
  (:require [gws.mandrill.client :as client]
            [gws.mandrill.api.messages :as messages]))

(def client (client/create "4FCHR1oN9NUam7sBXzgHEw"))

(defn send-email [msg]
  (messages/send client msg))

(defn email-params [name lead]
  (case name
    :derivation_email {:template_name "mail-referral"
                       :template_content []
                       :message {:to [{:email "agnivalent@gmail.com"
                                        :name "Eugfeue"}]
                                 :from_email "no-reply@misabogados.com"
                                 :subject "HELLO TEST"
                                 :global_merge_vars [{:name "client_name" :content "DOGGO"
                                                      }]}}
    :meeting_email "Mail consejos para reunión (al cliente)"
    :phone_coordination_email "Mail coordinación telefónica (al cliente y al abogado)"
    :thanks_email "Mail de agradecimiento (al cliente)"
    :extension_email "Mail de Extensión (al cliente)"
    :trello_email "Nuevo asunto en trello")
  )

(defn do-lead-actions [actions lead]
  (map #(messages/send-template client (email-params % lead)) actions))



;; def derivation(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "Nueva derivación: #{lead.user.name} - #{lead.matches.last.workflow_lawyer.name}"
;;     merge_vars = {
;;       "client_name" => lead.user.name,
;;       "client_phone" => lead.user.phone,
;;       "client_email" => lead.user.email,
;;       "problem" => lead.problem
;;     }
;;     body = mandrill_template("mail-referral", merge_vars)

;;     send_mail("dani@misabogados.com", subject, body)
;;   end

      :derivation_email {:template_name "mail-referral"
                         :template_content []
                         :message {:to [{:email "dani@misabogados.com"
                                         :name "Dani"}]
                                   :from_email "no-reply@misabogados.com"
                                   :subject (str "Nueva derivación: " (:name client) " - " (:name lawyer))
                                   :global_merge_vars [{:name "client_name" :content (:name client)}
                                                       {:name "client_phone" :content (:phone client)}
                                                       {:name "client_email" :content (:email client)}
                                                       {:name "problem" :content (:problem lead)}]}}

;;   def meeting_scheduled(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "Consejos para tu reunión con el abogado #{lead.matches.last.workflow_lawyer.name}"
;;     merge_vars ={
;;       "FNAME" => lead.user.name
;;     }
;;     body = mandrill_template("mail-reuni-n-agendada", merge_vars)

;;     send_mail(lead.user.email, subject, body)
;;   end

;;   def phone_coordination(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "#{lead.user.name} - confirmación llamada telefónica con #{lead.matches.last.workflow_lawyer.name}"
;;     merge_vars = {
;;       "client_name" => lead.user.name,
;;       "lawyer_name" => lead.matches.last.workflow_lawyer.name,
;;       "meeting_time" => lead.matches.last.meetings.last.time,
;;       "client_phone" => lead.user.phone,
;;       "client_email" => lead.user.email,
;;       "lawyer_phone" => lead.matches.last.workflow_lawyer.phone,
;;       "lawyer_email" => lead.matches.last.workflow_lawyer.email,
;;       "lawyer_address" => lead.matches.last.workflow_lawyer.address
;;     }
;;     body = mandrill_template("presentation-email", merge_vars)

;;     send_mail([lead.user.email, lead.matches.last.workflow_lawyer.email], subject, body)
;;   end
      :phone_coordination_email {:template_name "presentation-email"
                                 :template_content []
                                 :message {:to [{:email (:email client)
                                                 :name (:name client)}
                                                {:email (:email lawyer)
                                                 :name (:name lawyer)}]
                                           :from_email "no-reply@misabogados.com"
                                           :subject (str (:name client) " - confirmación llamada telefónica con " (:name lawyer))
                                           :global_merge_vars [(let [meeting (-> lead :matches last :meetings last)]
                                                                 {:name "client_name" :content (:name client)}
                                                                 {:name "lawyer_name" :content (:name lawyer)}
                                                                 {:name "meeting_time" :content (:time meeting)}
                                                                 {:name "client_phone" :content (:phone client)}
                                                                 {:name "client_email" :content (:email client)}
                                                                 {:name "lawyer_phone" :content (:phone lawyer)}
                                                                 {:name "lawyer_email" :content (:email lawyer)}
                                                                 {:name "lawyer_address" :content (:address lawyer)})]}}

;;   def thanks(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "Gracias #{lead.user.name}"
;;     merge_vars = {
;;       "FNAME" => lead.user.name
;;     }
;;     body = mandrill_template("agradecimiento", merge_vars)

;;     # send_mail(lead.user.email, subject, body)
;;     mail(to: lead.user.email, subject: subject, body: body, content_type: "text/html", from: "gonzalo@misabogados.com")
;;   end

;;   def extension(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "#{lead.user.name}, estamos aquí para lo que necesites"
;;     merge_vars = {
;;       "FNAME" => lead.user.name,
;;       "ABOGADO" => lead.matches.last.workflow_lawyer.name
;;     }
;;     body = mandrill_template("mail-de-extensi-n", merge_vars)

;;     send_mail(lead.user.email, subject, body)
;;   end
      :extension_email {:template_name "mail-de-extensi-n"
                        :template_content []
                        :message {:to [{:email (:email client)
                                        :name (:name client)}]
                                  :from_email "gonzalo@misabogados.com"
                                  :subject (str (:name client) ", estamos aquí para lo que necesites")
                                  :global_merge_vars [{:name "FNAME" :content (:name client)}
                                                      {:name "ABOGADO" :content (:name lawyer)
                                                     }]}}

;;   def trello(lead_id)
;;     lead = Lead.find(lead_id)
;;     subject = "#{lead.user.name} - #{(lead.matches.any? and lead.matches.last.workflow_lawyer) ? lead.matches.last.workflow_lawyer.name : ""}"
;;     body = "#{lead.category ? lead.category.title : ""}<br/>"
;;     body << "#{lead.user.phone}<br/>"
;;     body << "#{lead.user.email}<br/>"
;;     body << edit_workflow_lead_url(lead_id)
;;     send_mail("eugenehardbread+c7pedaxdgpho8qnomc2b@boards.trello.com", subject, body)
;;   end
