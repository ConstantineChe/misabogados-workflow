(ns misabogados-workflow.payments
  (:require [reagent.core :as r]
            [misabogados-workflow.utils :as u]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [json-html.core :refer [edn->hiccup]]))

(def table-data (r/atom {}
                 ))
(def form-data (r/atom {}))

(def validation-message (r/atom nil))


;;probably for move into helpers
(defn input [label type id]
  [:div.form-group
   [:label {:for id} label]
   (if (= :textarea type)
     [:textarea.form-control {:field type :id id}]
     [:input.form-control {:field type :id id}])])


(defn input-select-btn [options id]
  [:select.form-control {:field :list :id id}
   (for [[label value] options]
     [:option {:key value} label])])

;;


;;todo server request
(defn refresh-table []
  (GET (str js/context "/payment-requests")
                      {:handler #((reset! table-data (get % "payment-requests"))
                                  nil)})())

(defn create-payment-request [form-data]
  (POST (str js/context "/payment-requests") {:params form-data
                                      :handler #(refresh-table)
                                      :error-handler (fn [] nil)}))
(defn update-payment-request [form-data]
  (js/alert (str form-data)))

(def payment-request-form-template
     [:div.modal-body
      [:label {:field :label :id :_id}]
      (input "Nombre del cliente" :text :client)
      (input "Email del cliente" :email :client_email)
      (input "Teléfono del cliente" :text :client_tel)
      (input "Servicio" :text :service)
      (input "Descripción del servicio" :textarea :service_description)
      (input "Amount" :numeric :amount)
      [:div.form-group
       [:div.list-group {:field :single-select :id :own_client}
         [:div.list-group-item {:key "own"} "Cliente propio"]
         [:div.list-group-item {:key "MisAbogados"} "Cliente MisAbogados"]]]
      [:div.form-group
       [:label "Acepto los Términos y condiciones Transacciones"]
       [:input.form-control {:type :checkbox :required "true"}]]]
  )

(defn validate-payment-request-form [data]
  (reset! validation-message nil)
  (some false? (for [field data]
                 (if-not (empty? (str (val field)))
                   (do (reset! validation-message (str (key field) " not present"))
                       false)
                   true))))



(defn create-payment-request-form []

  (fn []
    [:div#payment-request-form.modal.fade {:role :dialog}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header
        [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
         [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
        [:h3.modal-title "Payment Request"]]
       [bind-fields
        payment-request-form-template
        form-data]
       (edn->hiccup @form-data)
       [:p @validation-message]
       [:div.modal-footer
        [:button.btn.btn-default {:type :button :data-dismiss :modal} "Cerrar"]
        [:button.btn.btn-primary {:type :button
                                  :on-click #(if (validate-payment-request-form @form-data)
                                               (do (create-payment-request @form-data)
                                                (u/close-modal "payment-request-form")
                                                (reset! form-data {})
                                                (refresh-table))
                                               (js/alert "INVALID!!!#!#!@"))} "Guardar"]
        [:div "disabled"]]]
      ]]
    ))

(defn edit-payment-request-form [data]
  (let [edit-form-data (r/atom data)]
    (fn []
            [:div.modal.fade {:role :dialog :id (str "payment-request-form" (:_id data))}
             [:div.modal-dialog
              [:div.modal-content
               [:div.modal-header
                [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                 [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
                [:h3.modal-title "Edit Payment Request"]]
               (edn->hiccup @edit-form-data)
               [bind-fields
                payment-request-form-template
                edit-form-data]
               [:div.modal-footer
                [:button.btn.btn-default {:type :button :data-dismiss :modal} "Cerrar"]
                [:button.btn.btn-primary {:type :button
                                          :on-click #(do (update-payment-request @edit-form-data)
                                                         (u/close-modal (str "payment-request-form" (:_id data)))
                                                         (refresh-table))} "Guardar"]]]
              ]]
            )))

(defn table []
  (fn []
    (if-not (empty? @table-data)
      [:div
       [:legend "Payment Requests"]
       [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
        [:th "Client"]
        [:th "Service"]
        [:th "Amount"]
        [:th "Client type"]
        [:tbody
         (doall
          (for [row @table-data]
            (let [row-key (key row)
                  values (apply merge (map (fn [field]
                                             {(keyword (key field)) (val field)})
                                           (get @table-data row-key)))]

              [:tr {:key row-key
                    :on-click #(do
                                 (u/show-modal (str "payment-request-form" row-key)))}
               [:td (get values :client) ]
               [:td (get values :service)]
               [:td (get values :amount)]
               [:td (get values :own_client)]
               ])))]]]
      [:h4 "You have no payment-request requests"])))

(defn payments []
  (let [payment-requests (GET (str js/context "/payment-requests")
                      {:handler #((reset! table-data (get % "payment-requests"))
                                  nil)})]
    (fn []
      [:div.container
       [:h1 "PagoLegal"]
       [:button.btn {:type :button
                     :on-click #(do
                                  (u/show-modal "payment-request-form")
                                  (reset! form-data {}))} "Create payment request"]
       [table]
       [create-payment-request-form]
        (for [row @table-data]
         (let [row-key (key row)
               values (apply merge (map (fn [field]
                               {(keyword (key field)) (val field)})
                                        (get @table-data row-key)))]
           [(edit-payment-request-form (into {:_id row-key} values))]
))
       ])))
