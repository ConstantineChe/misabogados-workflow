(ns misabogados-workflow.lead
    (:require [reagent.core :as r]
              [misabogados-workflow.utils :as u]
              [reagent.session :as session]
              [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
              [misabogados-workflow.elements :as el]
              [bouncer.core :as b]
              [bouncer.validators :as v]
              [clojure.walk :refer [keywordize-keys]]
              [secretary.core :as secretary :include-macros true]
              [json-html.core :refer [edn->hiccup]]))


(defn update-lead [id form-data actions]
  (PUT (str js/context "/lead/" id) {:params {:lead (dissoc form-data :_id) 
                                              :actions (keys (filter #(val %) actions))}
                                     :error-handler #(js/alert (str %))}))

(defn new-lead []
  (let [lead-data (r/atom {})
        options (r/atom {})]
    (GET (str js/context "/leads/options") {:handler #(reset! options {:lead (keywordize-keys %)})})
    (fn []
      [:div.container
       (str "form data: " @lead-data)
       (el/form "New Lead" [lead-data options]
                ["Lead"
                 (el/input-text "Client Id" [:lead :client_id])
                 (el/input-text "Client Email" [:lead :client_email])
                 (el/input-text "Region" [:lead :region_name])
                 (el/input-text "City" [:lead :city])
                 (el/input-typeahead "Category" [:lead :category_id])
                 (el/input-textarea "Problem" [:lead :problem])
                 (el/input-dropdown "Lead Type" [:lead :lead_type_code])
                 (el/input-dropdown "Lead Source" [:lead :lead_source_code])
                 (el/input-text "Referrer" [:lead :refer])
                 (el/input-number "NPS" [:lead :nps])
                 (el/input-text "Adwords url" [:lead :adwords_url])
                 ["Match"
                  (el/input-typeahead "Lawyer" [:lead :matches :lawyer_id])
                  ["Meeting"
                   (el/input-text "Type" [:lead :matches :meetings :type])
                   (el/input-datetimepicker ["Date" "Time"] [:lead :matches :meetings :time])]]
                 ]
                )])))

(defn edit-lead []
  (let [id (session/get :current-lead-id)
        fetch (fn [atom] #(reset! atom {:lead (keywordize-keys %)}))
        lead-data (r/atom {})
        options (r/atom {})
        actions (r/atom {})]
    (GET (str js/context "/leads/options") {:handler (fetch options)})
    (GET (str js/context "lead/" id) {:handler (fetch lead-data)})
    (fn []
      [:div.container
       (str "form data: " @lead-data) [:br]
       (str "options: " (dissoc @options :lead)) [:br]
       (str "actions: " @actions)
       (el/form "Edit Lead" [lead-data options]
                (reduce conj ["Lead"
                       (el/input-text "Client Id" [:lead :client_income_id])
                       (el/input-text "Client Email" [:lead :client_email])
                       (el/input-text "Region" [:lead :region_name])
                       (el/input-text "City" [:lead :city])
                       (el/input-typeahead "Category" [:lead :category_id])
                       (el/input-textarea "Problem" [:lead :problem])
                       (el/input-dropdown "Lead Type" [:lead :lead_type_code])
                       (el/input-dropdown "Lead Source" [:lead :lead_source_code])
                       (el/input-text "Referrer" [:lead :refer])
                       (el/input-number "NPS" [:lead :nps])
                       (el/input-text "Adwords url" [:lead :adwords_url])]
                        (map-indexed (fn [i match]
                                       (reduce conj ["Match"
                                                     (el/input-typeahead "Lawyer" [:lead :matches i :lawyer_id])]
                                               (map-indexed (fn [j meeting]
                                                              ["Meeting"
                                                               (el/input-text "Type" [:lead :matches i :meetings j :type])
                                                               (el/input-datetimepicker ["Date" "Time"] [:lead :matches i :meetings j :time])])
                                                            (:meetings match))))
                                     (get-in @lead-data [:lead :matches]))))
       [:fieldset 
        [:legend "Actions"] 
        
        (map (fn [[name label]] 
               (let [cursor (r/cursor actions [name])] 
                 [:div.form-group [:label 
                                     [:input {:type :checkbox
                                              :value @cursor
                                              :on-change #(reset! cursor (-> % .-target .-checked))}] 
                                     label]]))
             {:derivation_email "Mail enviar a derivación (a Dani)"
              :meeting_email "Mail consejos para reunión (al cliente)"
              :phone_coordination_email "Mail coordinación telefónica (al cliente y al abogado)"
              :thanks_email "Mail de agradecimiento (al cliente)"
              :extension_email "Mail de Extensión (al cliente)"
              :trello_email "Nuevo asunto en trello"})]
       [:button.btn.btn-primary {:type :button
                                 :on-click #(if true ;;(validate-lead-form @lead-data)
                                              (do (update-lead (-> @lead-data :lead :_id) (:lead @lead-data) @actions)
                                                  ;; (reset! validation-message nil))
                                                  ))} "Guardar"]])))

(def pages
  {:new-lead #'new-lead
   :edit-lead #'edit-lead})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/lead" []
  (session/put! :page :new-lead))

(secretary/defroute "/lead/:id/edit" {id :id}
  (do (session/put! :page :edit-lead)
      (session/put! :current-lead-id id)))
