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

(defn create-client! [data id-cursor options text]
  (POST (str js/context "/users/client") {:params {:data (:new-client data)}
                                          :handler #(let [client (keywordize-keys %)
                                                          label (str (:name client) " (" (:email client) ")")]
                                                      (reset! text label)
                                                      (swap! options conj
                                                             [label (:_id client)])
                                                      (reset! id-cursor (:_id (keywordize-keys %))))
                                          :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))}))

(defn edit-client! [data options id]
  (PUT (str js/context "/users/client") {:params {:id id
                                                  :data (dissoc (:edit-client @data) :_id)}
                                         :handler #(let [client (keywordize-keys %)]
                                                     (swap! options
                                                            (fn [x]
                                                              (for [[label id] x]
                                                                (if (= id (:_id client))
                                                                  [(str (:name client) " (" (:email client) ")") id]
                                                                  [label id])))))
                                         :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           404 (js/alert "Client not found")
                                                           (js/alert (str %)))}))
(defn update-lead [id form-data actions]
  (PUT (str js/context "/lead/" id) {:params {:lead (dissoc form-data :_id)
                                              :actions (keys (filter #(val %) actions))}
                                     :handler #(do (session/put! :notification
                                                                 [:div.alert.alert-sucsess "Lead with id "
                                                                  [:a {:href (str "#/lead/" id "/edit")} id]
                                                                  " was updated."])
                                                   (aset js/window "location" "/#dashboard"))
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
                                                (aset js/window "location" "/#dashboard"))
                                  :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))}))

(defn get-client [id client]
  (if id (GET (str js/context "/users/client/" id) {:handler #(reset! client {:edit-client (keywordize-keys %)})}) client))

(defn create-client [client-data options id-cursor text]
  (r/create-class
   {:render (fn []
              [:div.modal.fade {:role :dialog :id "lead-client_id-create"}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  (el/form "New client" [client-data]
                           ["Client"
                            (el/input-text "Clients name" [:new-client :name])
                            (el/input-email "Clients email" [:new-client :email])
                            (el/input-text "clients tel" [:new-client :phone])])]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]
                  [:button.btn.btn-primary {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Create"
                                            :on-click #(create-client! @client-data id-cursor options text)} "Create"]]]]])
    :component-did-mount (fn [this] (let [modal (-> this r/dom-node js/jQuery)]
                                     (.attr modal "tabindex" "-1")
                                     (.on  modal "hide.bs.modal"
                                           #(js/setTimeout (fn [] (reset! client-data nil)) 100))))}))

(defn edit-client [client-data options id]
  (r/create-class
   {:render (fn []
              (when (and id (not= id (-> @client-data :edit-client :_id))) (get-client id client-data))
              [:div.modal.fade {:role :dialog :id "lead-client_id-edit"}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  (el/form "Edit client" [client-data]
                           ["Client"
                            (el/input-text "Clients name" [:edit-client :name])
                            (el/input-email "Clients email" [:edit-client :email])
                            (el/input-text "clients tel" [:edit-client :phone])])]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]
                  [:button.btn.btn-primary {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Update"
                                            :on-click #(edit-client! client-data options id)} "Update"]]]]])
    :component-did-mount (fn [this] (let [modal (-> this r/dom-node js/jQuery)]
                                     (.attr modal "tabindex" "-1")
                                     (.on  modal "hide.bs.modal"
                                           #(js/setTimeout (fn [] (reset! client-data nil)) 100))))})
  )


(defn new-lead []
  (let [lead-data (r/atom {})
        options (r/atom {})
        util (r/atom {})
        actions (r/atom {})
        selected-client (r/atom nil)]
    (GET (str js/context "/leads/options") {:handler #(reset! options {:lead (keywordize-keys %)})})
    (fn []
      [:div.container
       (str "form data: " @lead-data) [:br]
;       (str "clients: " (:client_id (:lead @options)))
       (el/form "New Lead" [lead-data options util]
                ["Lead"
                 (el/input-entity "Client Id" [:lead :client_id]
                                  (edit-client selected-client
                                               (r/cursor options [:lead :client_id])
                                               (get-in @lead-data [:lead :client_id]))
                                  (create-client (r/atom {}) (r/cursor options [:lead :client_id])
                                                 (r/cursor lead-data [:lead :client_id])
                                                 (r/cursor util (into [:typeahead-t] [:lead :client_id]))))
                 (el/input-text "Region" [:lead :region_name])
                 (el/input-text "City" [:lead :city])
                 (el/input-typeahead "Category" [:lead :category_id])
                 (el/input-dropdown "Lead Type" [:lead :lead_type_code])
                 (el/input-dropdown "Lead Source" [:lead :lead_source_code])
                 (el/input-text "Referrer" [:lead :refer])
                 (el/input-number "NPS" [:lead :nps])
                 (el/input-text "Adwords url" [:lead :adwords_url])
                 (el/input-textarea "Problem" [:lead :problem])
                 ["Match"
                  (el/input-typeahead "Lawyer" [:lead :matches :lawyer_id])
                  ["Meeting"
                   (el/input-text "Type" [:lead :matches :meetings :type])
                   (el/input-datetimepicker ["Date" "Time"] [:lead :matches :meetings :time])]]
                 ]
                ) [:fieldset
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
       (str "form data: " @lead-data) [:br]
;       (str "options: " (dissoc @options :lead)) [:br]
;       (str "actions: " @actions)
       (el/form "Edit Lead" [lead-data options util]
                (reduce conj ["Lead"
                              (el/input-entity "Client Id" [:lead :client_id]
                                               (edit-client selected-client
                                                            (r/cursor options [:lead :client_id])
                                                            (get-in @lead-data [:lead :client_id]))
                                               (create-client (r/atom {}) (r/cursor options [:lead :client_id])
                                                              (r/cursor lead-data [:lead :client_id])
                                                              (r/cursor util (into [:typeahead-t] [:lead :client_id]))))
                       (el/input-text "Region" [:lead :region_name])
                       (el/input-text "City" [:lead :city])
                       (el/input-typeahead "Category" [:lead :category_id])
                       (el/input-dropdown "Lead Type" [:lead :lead_type_code])
                       (el/input-dropdown "Lead Source" [:lead :lead_source_code])
                       (el/input-text "Referrer" [:lead :refer])
                       (el/input-number "NPS" [:lead :nps])
                       (el/input-text "Adwords url" [:lead :adwords_url])
                       (el/input-textarea "Problem" [:lead :problem])]
                        (map-indexed (fn [i match]
                                       (reduce conj ["Match"
                                                     (el/input-typeahead "Lawyer" [:lead :matches i :lawyer_id])]
                                               (map-indexed (fn [j meeting]
                                                              ["Meeting"
                                                               (el/input-text "Type" [:lead :matches i :meetings j :type])
                                                               (el/input-datetimepicker ["Date" "Time"]
                                                                                        [:lead :matches i :meetings j :time])])
                                                            (if-let [meeting (:meetings match)]
                                                              meeting (reset! (r/cursor lead-data [:lead :matches i :meetings])
                                                                              [{}])))))
                                     (if-let [match (get-in @lead-data [:lead :matches])]
                                       match (reset! (r/cursor lead-data [:lead :matches]) [{}])))))
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
                                              (do (update-lead (session/get :current-lead-id) (:lead @lead-data) @actions)
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
