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
              [json-html.core :refer [edn->hiccup]]
              [misabogados-workflow.schema :as s]
              [misabogados-workflow.flow-definition :refer [steps]])
    )

(defn update-lead [id form-data actions]
  (PUT (str js/context "/lead/" id) {:params {:lead (dissoc form-data :_id)
                                              :actions (keys (filter #(val %) actions))}
                                     :handler #(do (session/put! :notification
                                                                 [:div.alert.alert-sucsess "Lead with id "
                                                                  [:a {:href (str "#/lead/" id "/edit")} id]
                                                                  " was updated."])
                                                   (u/redirect "#dashboard"))
                                     :error-handler #(case (:status %)
                                                       403 (js/alert "Access denied")
                                                       404 (js/alert "Lead not found")
                                                       500 (js/alert "Internal server error")
                                                       (js/alert (str %)))}))

(defn create-lead [form-data actions]
  (POST (str js/context "/lead") {:params {:lead form-data
                                           :actions (keys (filter #(val %) actions))}
                                  :handler #(let [id (get % "id")]
                                              (session/put! :notification
                                                            [:div.alert.alert-sucsess "Lead created with id "
                                                             [:a {:href (str "#/lead/" id "/edit")} id]])
                                                (u/redirect "#dashboard"))
                                  :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))}))


(defn step-description [step]
  [:div.modal.fade {:role :dialog :id "step-description"}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  [:p (get ((keyword step) steps) 2)]]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]]]]])

(defn do-lead-action []
  (let [id (session/get :current-lead-id)
        action (session/get :current-lead-action)
        fetch (fn [atom] #(reset! atom {:lead (keywordize-keys %)}))
        lead-data (r/atom {})
        options (r/atom {})
        actions (r/atom {})
        util (r/atom {})
        selected-client (r/atom nil)
        step-actions (r/atom nil)]
    (GET (str js/context "/leads/options") {:handler (fetch options)})
    (GET (str js/context "lead/" id) {:handler (fetch lead-data)})
    (GET (str js/context "/lead/" id "/actions") {:handler #(let [response (keywordize-keys %)]
                                                              (reset! step-actions (:actions response)))})
    (fn []
      [:div.container
;       (str "lead data: " @lead-data) [:br]
;       (str "step actions: " @step-actions) [:br]
;       (str "options: " (dissoc @options :lead)) [:br]
                                        ;       (str "actions: " @actions)
       (step-description action)
       (el/create-form [:div [:h1 (u/remove-kebab action)] [:a {:on-click #(u/show-modal "step-description")}  " Description"]]
                        s/lead [lead-data options util])
       (into [:div.btn-group [:button.btn.btn-primary {:type :button
                                         :on-click #(if true ;;(validate-lead-form @lead-data)
                                                      (do (update-lead (session/get :current-lead-id) (:lead @lead-data) @actions)
                                                          ;; (reset! validation-message nil))
                                                          ))} "Guardar"]]
             (map #(el/action-button lead-data % (str "lead/" id "/action/") "#dashboard") @step-actions))])))


(defn new-lead []
  (let [lead-data (r/atom {})
        options (r/atom {})
        util (r/atom {})
        actions (r/atom {})
        selected-client (r/atom nil)]
    (GET (str js/context "/leads/options") {:handler #(reset! options {:lead (keywordize-keys %)})})
    (fn []
      [:div.container
;       (str "form data: " @lead-data) [:br]
;       (str "clients: " (:client_id (:lead @options)))
       (el/create-form "New Lead" s/lead [lead-data options util])
       [:fieldset
        [:legend "Actions"]
        (doall (map (fn [[name label]]
                      (let [cursor (r/cursor actions [name])]
                        [:div.form-group {:key name} [:label
                                                      [:input {:type :checkbox
                                                               :value @cursor
                                                               :on-change #(reset! cursor (-> % .-target .-checked))}]
                                                      label]]))
                    {:derivation_email "Mail enviar a derivación (a Dani)"
                     :meeting_email "Mail consejos para reunión (al cliente)"
                     :phone_coordination_email "Mail coordinación telefónica (al cliente y al abogado)"
                     :thanks_email "Mail de agradecimiento (al cliente)"
                     :extension_email "Mail de Extensión (al cliente)"
                     :trello_email "Nuevo asunto en trello"}))]
       [:button.btn.btn-primary {:type :button
                                 :on-click #(if true ;;(validate-lead-form @lead-data)
                                              (do (create-lead (:lead @lead-data) @actions)
                                                  ;; (reset! validation-message nil))
                                                  ))} "Guardar"]])))

(defn edit-lead []
  (let [id (session/get :current-lead-id)
        fetch (fn [atom] #(reset! atom {:lead (keywordize-keys %)}))
        lead-data (r/atom {})
        options (r/atom {})
        actions (r/atom {})
        util (r/atom {})
        selected-client (r/atom nil)]
    (GET (str js/context "/leads/options") {:handler (fetch options)})
    (GET (str js/context "lead/" id) {:handler (fetch lead-data)})
    (fn []
      [:div.container
;       (str "form data: " @lead-data) [:br]
;       (str "options: " (dissoc @options :lead)) [:br]
;       (str "actions: " @actions)
       (el/create-form "Edit Lead" s/lead [lead-data options util])
       [:button.btn.btn-primary {:type :button
                                 :on-click #(if true ;;(validate-lead-form @lead-data)
                                              (do (update-lead (session/get :current-lead-id) (:lead @lead-data) @actions)
                                                  ;; (reset! validation-message nil))
                                                  ))} "Guardar"]])))



(def pages
  {:new-lead #'new-lead
   :edit-lead #'edit-lead
   :do-action-lead #'do-lead-action})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/lead" []
  (session/put! :page :new-lead))

(secretary/defroute "/lead/:id/edit" {id :id}
  (do (session/put! :page :edit-lead)
      (session/put! :current-lead-id id)))

(secretary/defroute "/lead/:id/action/:action" {id :id action :action}
  (do (session/put! :page :do-action-lead)
      (session/put! :current-lead-id id)
      (session/put! :current-lead-action action)))
