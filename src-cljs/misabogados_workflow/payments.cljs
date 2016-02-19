(ns misabogados-workflow.payments
  (:require [reagent.core :as r]
            [misabogados-workflow.utils :as u]
            [reagent-forms.core :refer [bind-fields init-field value-of]]))

(def table-data (r/atom 
                 {"1" {:client "John Dillinger" :amount 100}
                  "2" {:client "Tobias Knight" :amount 200}}
                 ))

(def form-data (r/atom {:client "Name" :amount 1000}))

;;probably for move into helpers
(defn input [label type id]
  [:div.form-group 
   [:label {:for id} label]
   [:input.form-control {:field type :id id}]])

;; 

;;todo server request
(defn refresh-table []
  )


(def payment-form-template
  [:div#payment-form.modal.fade {:role :dialog}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:button.close {:type :button :data-dismiss :modal :aria-label "Close"} [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
      [:h3.modal-title "Payment Request"]]
     [:div.modal-body
      [:label {:field :label :id :_id}]
      (input "Client name" :text :client)
      (input "Amount" :numeric :amount)]
     [:div.modal-footer
      [:button.btn.btn-default {:type :button :data-dismiss :modal} "Cerrar"]
      [:button.btn.btn-primary {:type :button} "Guardar"]]]
    ]]
  )

(defn table [] 
  (if-not (empty? @table-data)
    [:div
     [:legend "Payment Requests"]
     [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
      [:th "client"]
      [:th "amount"]
      (doall
       (for [row @table-data]
         (let [row-key (key row)
                values (get @table-data row-key)]
           [:tr {:key row-key
                 :on-click #(do 
                              (u/show-modal "payment-form") 
                              (reset! form-data (into {:_id row-key} values)))}
            [:td (:client values)]
            [:td (:amount values)]
            ])))]]))

(defn payments []
  (let []
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
