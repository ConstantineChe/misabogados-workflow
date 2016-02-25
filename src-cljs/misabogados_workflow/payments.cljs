(ns misabogados-workflow.payments
  (:require [reagent.core :as r]
            [misabogados-workflow.utils :as u]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [json-html.core :refer [edn->hiccup]]))

(def table-data (r/atom {}
                 ))

(def form-data (r/atom {}))

;;probably for move into helpers
(defn input [label type id]
  [:div.form-group
   [:label {:for id} label]
   (if (= :textarea type)
     [:textarea.form-control {:field type :id id}]
     [:input.form-control {:field type :id id}])])


(defn input-select-btn [options id]
  [:div.btn-group {:field :single-select :id id}
   (for [[label value] options]
     [:button.btn.btn-default {:key value} label])])

;;


;;todo server request
(defn refresh-table []
  (GET (str js/context "/payments")
                      {:handler #((reset! table-data (get % "payments"))
                                  nil)})())

(defn create-payment [form-data]
  (POST (str js/context "/payments") {:params form-data
                                      :handler #(js/alert (str %))
                                      :error-handler #(js/alert (str "error: " %))}))


(def payment-form-template
  [:div#payment-form.modal.fade {:role :dialog}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
       [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
      [:h3.modal-title "Payment Request"]]
     [:div.modal-body
      [:div (str @form-data)]
      [:label {:field :label :id :_id}]
      (input "Nombre del cliente" :text :client)
      (input "Email del cliente" :email :client_email)
      (input "Teléfono del cliente" :text :client_tel)
      (input "Servicio" :text :service)
      (input "Descripción del servicio" :textarea :service_description)
      (input "Amount" :numeric :amount)
      (input-select-btn [["Cliente MisAbogados" :MisAbogados]
                         ["Cliente propio" :own]] :own_client)
      [:div.form-group
       [:label "Acepto los Términos y condiciones Transacciones"]
       [:input.form-control {:field :checkbox}]]]
     [:div.modal-footer
      [:button.btn.btn-default {:type :button :data-dismiss :modal} "Cerrar"]
      [:button.btn.btn-primary {:type :button
                                :on-click #(do (create-payment @form-data)
                                               (u/close-modal "payment-form")
                                               (reset! form-data {})
                                               (refresh-table))} "Guardar"]]]
    ]]
  )

(defn table []
  (if-not (empty? @table-data)
    [:div
     [:legend "Payment Requests"]
     [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
      [:th "Client"]
      [:th "Service"]
      [:th "Amount"]
      [:th "Client type"]
      (doall
       (for [row @table-data]
         (let [row-key (key row)
               values (apply merge (map (fn [field]
                               {(keyword (key field)) (val field)})
                             (get @table-data row-key)))]
           [:tr {:key row-key
                 :on-click #(do
                              (u/show-modal "payment-form")
                              (reset! form-data (into {:_id row-key} values)))}
            [:td (get values :client)]
            [:td (get values :service)]
            [:td (get values :amount)]
            [:td (get values :own_client)]
            ])))]]
    [:h4 "You have no payment requests"]))

(defn payments []
  (let [payments (GET (str js/context "/payments")
                      {:handler #((reset! table-data (get % "payments"))
                                  nil)})]
    (fn []
      [:div.container
       [:h1 "PagoLegal"]
       [:button.btn {:type :button
                     :on-click #(do
                                  (u/show-modal "payment-form")
                                  (reset! form-data {}))} "Create payment request"]
       [table]
       [bind-fields
        payment-form-template
        form-data]])))
