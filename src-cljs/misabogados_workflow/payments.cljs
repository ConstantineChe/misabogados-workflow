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
                                      :handler #(js/alert (str %))
                                      :error-handler #(js/alert (str "error: " %))}))
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
         [:div.list-group-item {:key :own} "Own"]
         [:div.list-group-item {:key :MisAbogados} "MisAbogados"]]]
      [:div.form-group
       [:label "Acepto los Términos y condiciones Transacciones"]
       [:input.form-control {:field :checkbox}]]]
  )



(defn create-payment-request-form []
  (fn []
    [:div#payment-request-form.modal.fade {:role :dialog}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header
        [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
         [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
        [:h3.modal-title "Payment-Request Request"]]
       [bind-fields
        payment-request-form-template
        form-data]
       [:div.modal-footer
        [:button.btn.btn-default {:type :button :data-dismiss :modal} "Cerrar"]
        [:button.btn.btn-primary {:type :button
                                  :on-click #(do (create-payment-request @form-data)
                                                 (u/close-modal "payment-request-form")
                                                 (reset! form-data {})
                                                 (refresh-table))} "Guardar"]]]
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
                [:h3.modal-title "Edit Payment-Request Request"]]
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
  (if-not (empty? @table-data)
    [:div
     [:legend "Payment-Request Requests"]
     [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
      [:th "Botón de pago"]
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
                              (u/show-modal (str "payment-request-form" row-key))
                              (reset! form-data (into {:_id row-key} values)))}
            [:td [:a {:href (str "/payments/" (get values :code)) 
                      :data-toggle "tooltip"
                      :title "Este enlace fue enviado al cliente"
                      } "Pagar"]]
            [:td (get values :client) ]
            [:td (get values :service)]
            [:td (get values :amount)]
            [:td (get values :own_client)]
            ])))]]
    [:h4 "You have no payment-request requests"]))

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
                                  (reset! form-data {}))} "Create payment-request request"]
       [:button.btn {:type :button
                     :on-click #(do
                                  (u/show-modal "payment-request-form1")
                                  (reset! form-data {}))} "Create test"]
       [table]
       [create-payment-request-form]
       [edit-payment-request-form (into {:_id "56ce38d3d8963c5c72fdfde81"} (:56ce38d3d8963c5c72fdfde8 @table-data))]
        (for [row @table-data]
         (let [row-key (key row)
               values (apply merge (map (fn [field]
                               {(keyword (key field)) (val field)})
                                        (get @table-data row-key)))]
           [edit-payment-request-form (into {:_id row-key} values)]
))
       ])))
