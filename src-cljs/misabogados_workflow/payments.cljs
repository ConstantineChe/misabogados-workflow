(ns misabogados-workflow.payments
  (:require [reagent.core :as r]
            [misabogados-workflow.utils :as u]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [misabogados-workflow.elements :as el]
            [clojure.walk :refer [keywordize-keys]]
            [json-html.core :refer [edn->hiccup]]))

;;TODO this is for non-reagent payment pages. Move everything else to payment_requests.cljs
(defn- create-hidden-input [param]
  (let [key (key param) val (val param) input (.createElement js/document "input")]
    (aset input "type" "hidden")
    (aset input "name" key)
    (aset input "value" val)

    input))

;; (defn- redirect [url method params]
;;   (let [form (.createElement js/document "form")]
;;     (aset form "action" url)
;;     (aset form "method" method)
;;     (aset form "enctype" "multipart/form-data")
;;     (doall (map #(.appendChild form (create-hidden-input %)) params))
;;     (js/console.log form)
;;     ;; (.submit form)
;;     ))

;; (.addEventListener
;;   js/window
;;   "DOMContentLoaded"
;;   (fn []
;;     (.submit (js/jQuery "form#payment_form")
;;              (fn [e]
;;                (this-as form
;;                  (do (-> (js/jQuery form)
;;                             (.find "button")
;;                             (.button "loading"))
;;                      (POST (str js/context "/payments/pay")
;;                            {:params {:code (-> (js/jQuery "form#payment_form")
;;                                                (.find "input[name='code']")
;;                                                .val)
;;                                      :_id (-> (js/jQuery "form#payment_form")
;;                                                (.find "input[name='_id']")
;;                                                .val)}
;;                             :handler (fn [response]
;;                                        (redirect (get response "form-path")
;;                                                  "post"
;;                                                  (get response "form-data")))}))
;;                  (.preventDefault e))
;;                ))
;;     ))

(def table-data (r/atom {}
                 ))
(def form-data (r/atom {}))

(def validation-message (r/atom nil))

(defn get-filters []
  (filter #(not (empty? %))
          (concat
                (if-let [name (session/get-in [:filters :payment-requests :name])]
                  [{"$match" {:client {"$regex" name "$options" "i"}}}])
                (if-let [email (session/get-in [:filters :payment-requests :email])]
                  [{"$match" {:client_email {"$regex" email "$options" "i"}}}])
                (let [own-client (true? (session/get-in [:filters :payment-requests :own-client]))
                      misabogados-client (true? (session/get-in [:filters :payment-requests :misabogados-client]))]
                  (if (not (and own-client misabogados-client))
                    [{"$match" {:own_client own-client}}
                     {"$match" {:own_client (not misabogados-client)}}]))
                (let [status-pending (true? (session/get-in [:filters :payment-requests :status-pending]))
                      status-in-process (true? (session/get-in [:filters :payment-requests :status-in-process]))
                      status-paid (true? (session/get-in [:filters :payment-requests :status-paid]))
                      status-failed (true? (session/get-in [:filters :payment-requests :status-failed]))]
                  [ ;;(if-not status-pending {"$match" {:koko nil}})
                   ])
                (if-let [from-date (session/get-in [:filters :payment-requests :from-date])]
                  [{"$match" {:date_created {"$gte" from-date}}}])
                (if-let [to-date (session/get-in [:filters :payment-requests :to-date])]
                  [{"$match" {:date_created {"$lte" to-date}}}]))
           ))

;;todo server request
(defn get-payment-requests []
  (let [filters (get-filters)]
    (GET (str js/context "/payment-requests")
               {:params {:per-page 10
                         :page (if-let [page (session/get-in [:payment-requests :page])] page 1)
                         :sort-field :_id
                         :filters filters
                         :sort-dir -1
                         }
                :handler #(do (reset! table-data (get % "payment-requests"))
                              (session/assoc-in! [:payment-requests :count] (get-in % ["count" 0 "count"])))})))

(defn create-payment-request [form-data]
  (POST (str js/context "/payment-requests") {:params form-data
                                      :handler #(get-payment-requests)
                                      :error-handler (fn [] nil)}))
(defn update-payment-request [id form-data]
  (PUT (str js/context "/payment-requests/" id) {:params (dissoc form-data :_id :terms)
                                                 :handler #(get-payment-requests)
                                                 :error-handler #(js/alert (str %))}))

(defn remove-payment-request [id]
  (DELETE (str js/context "/payment-requests/" id) {:handler #(do (js/alert (str %)) (get-payment-requests))
                                                    :error-handler #(js/alert (str %))}))

(defn validate-payment-request-form [data]
  (reset! validation-message
          (b/validate data
                      :client [[v/required :message "Client's name should me present"]]
                      :amount [[v/required :message "Payment amount sould be present"]]
                      :client_email [[v/required :message "Client's email should be present"]
                                     [v/email :message "Client's email should be a valid email address"]]
                      :client_tel [[v/required :message "Client's phone number should be present"]]
                      :service [[v/required :message "Service name should be present"]]
                      :service_description [[v/required :message "Service description should be present"]]
                      :own_client [[(fn [x] (or (true? x) (false? x))) :message "Client type should be specified"]]
;                      :terms [[v/required :message "Youd should accept terms and conditions in order to send payment request"]]
                      ))
  (b/valid? data
            :client v/required
            :amount v/required
            :client_email [v/required v/email]
            :client_tel v/required
            :service v/required
            :service_description v/required
            :own_client v/required
 ;           :terms v/required
            ))



(defn create-payment-request-form []
  (let [data (r/atom nil)
        options (r/atom nil)
        utils (r/atom nil)]
    (GET (str js/context "/payment-requests/js/options") {:handler #(reset! options (keywordize-keys %))})
      (fn []
      [:div#payment-request-form.modal.fade {:role :dialog}
       [:div.modal-dialog
        [:div.modal-content
         [:div.modal-header
          [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
           [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
          [:h3.modal-title "Create Payment Request"]]
         [:div.modal-body
          (el/form "" [data options utils] (into (if (= "lawyer" (session/get-in [:user :role]))
                              ["Payment Request"]
                              ["Payment Request"
                               (el/input-typeahead "Lawyer" [:lawyer])])
                                                     [(el/input-text "Nombre del cliente*" [:client])
                                                      (el/input-email "Email del cliente*" [:client_email])
                                                      (el/input-text "Teléfono del cliente*" [:client_tel])
                                                      (el/input-text "Servicio*" [:service])
                                                      (el/input-textarea "Descripción del servicio*" [:service_description])
                                                      (el/input-number "Amount*" [:amount])
                                                      (el/input-dropdown "Cliente tipo" [:own_client])
                                                      ;; (el/input-checkbox "Acepto los Términos y condiciones Transacciones" [:terms])
                                                      ]))]
         [:div.modal-footer
          [:div.validation-messages
           (doall (for [message (first @validation-message)]
                    [:p {:key (key message)} (str (first (val message)))]))]
          [:button.btn.btn-default {:type :button
                                    :on-click #(do ((u/close-modal "payment-request-form")
                                                    (reset! validation-message nil)))} "Cerrar"]
          [:button.btn.btn-primary {:type :button
                                    :on-click #(if (validate-payment-request-form @data)
                                                 (do (create-payment-request @data)
                                                     (u/close-modal "payment-request-form")
                                                     (reset! data {})
                                                     (get-payment-requests)
                                                     (reset! validation-message nil))
                                                 )} "Guardar"]]]]])))

(defn edit-payment-request-form [data]
  (let [edit-form-data (r/atom data)
        options (r/atom nil)
        utils (r/atom nil)]
    (GET (str js/context "/payment-requests/js/options") {:handler #(reset! options (keywordize-keys %))})
    (fn []
      [:div.modal.fade {:role :dialog :id (str "payment-request-form" (:_id data))
                        :key (:_id data)}
             [:div.modal-dialog
              [:div.modal-content
               [:div.modal-header
                [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                 [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
                [:h3.modal-title "Edit Payment Request"]]
               (el/form "" [edit-form-data options utils]
                        (into (if (= "lawyer" (session/get-in [:user :role]))
                                ["Payment Request"]
                                ["Payment Request"
                                 (el/input-typeahead "Lawyer" [:lawyer])])
                              [(el/input-text "Nombre del cliente*" [:client])
                               (el/input-email "Email del cliente*" [:client_email])
                               (el/input-text "Teléfono del cliente*" [:client_tel])
                               (el/input-text "Servicio*" [:service])
                               (el/input-textarea "Descripción del servicio*" [:service_description])
                               (el/input-number "Amount*" [:amount])
                               (el/input-dropdown "Cliente tipo*" [:own_client])
                               ;; (el/input-checkbox "Acepto los Términos y condiciones Transacciones" [:terms])
                               ]))
                              [:div.modal-footer
                (doall (for [message (first @validation-message)]
                         [:p {:key (key message)} (str (first (val message)))]))
                [:button.btn.btn-default {:type :button
                                          :on-click #(do (u/close-modal (str "payment-request-form" (:_id data)))
                                                         (reset! validation-message nil))} "Cerrar"]
                [:button.btn.btn-primary {:type :button
                                          :on-click #(if (validate-payment-request-form @edit-form-data)
                                                       (do (update-payment-request (:_id data) @edit-form-data)
                                                           (u/close-modal (str "payment-request-form" (:_id data)))
                                                           (get-payment-requests)
                                                           (reset! validation-message nil))
                                                       )} "Guardar"]]]]])))

(defn payment-data-modal []
  (let [data (r/cursor session/state [:payments :payment-log])]
      (fn []
      [:div.modal.fade {:role :dialog :id "payment-data-modal"
                        :key (:_id data)}
       [:div.modal-dialog
        [:div.modal-content
         [:div.modal-header
          [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
           [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
          [:h3.modal-title "Payment request history"]]
         [:div.modal-body
          [:div (edn->hiccup @data)]]
         [:div.modal-footer
          [:button.btn.btn-default {:type :button
                                    :on-click #(u/close-modal "payment-data-modal")} "Cerrar"]]]]])))

(defn lawyer-data-modal []
  (let [data (r/cursor session/state [:payments :lawyer])]
    (fn []
      [:div.modal.fade {:role :dialog :id "lawyer-data-modal"
                        :key (:_id data)}
       [:div.modal-dialog
        [:div.modal-content
         [:div.modal-header
          [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
           [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]
          [:h3.modal-title "Lawyer"]]
         [:div.modal-body
          [:div (edn->hiccup @data)]]
         [:div.modal-footer
          [:button.btn.btn-default {:type :button
                                    :on-click #(u/close-modal "lawyer-data-modal")} "Cerrar"]]]]])))
(defn payments-table-render []
  (fn []
    [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
     [:th "Botón de pago"]
     (if (or (= "admin" (session/get-in [:user :role]))
             (= "finance" (session/get-in [:user :role]))) [:th "Lawyer" ])
     [:th "Client"]
     [:th "Service"]
     [:th "Amount"]
     [:th "Client type"]
     [:th "Last action"]
     [:th "Actions"]
     [:tbody
      (doall
       (for [row @table-data]
         (let [row-key (key row)
               values (apply merge (doall (map (fn [field]
                                                 {(keyword (key field)) (val field)})
                                               (get @table-data row-key))))]
           [:tr {:key row-key}
            [:td [:a {:href (str "/payments/pay/" (get values :code))
                      :data-toggle "tooltip"
                      :title "Este enlace fue enviado al cliente"
                      } "Pagar"]]
            (if (or (= "admin" (session/get-in [:user :role]))
                    (= "finance" (session/get-in [:user :role])))
              [:td {:on-click #(do (session/assoc-in! [:payments :lawyer] (first (get values :lawyer_data)))
                                   (u/show-modal "lawyer-data-modal"))}
               (get (first (get values :lawyer_data)) "name")])
            [:td (get values :client) ]
            [:td (get values :service)]
            [:td (get values :amount)]
            [:td [:span.balloon-tooltip {:data-toggle "tooltip" :data-placement "bottom" :title "Tooltip"
                                         } (if (= "true" (get values :own_client)) "Own" "MisAbogados")]]
            [:td {:on-click #(do (session/assoc-in! [:payments :payment-log] (get values :payment_log))
                                 (u/show-modal "payment-data-modal"))}
             (get (last (get values :payment_log)) "action")]
            [:td
             (if-not (get values :payment_log)
               [:div.btn-group [:button.btn.btn-primary {:on-click #(do
                                                                      (u/show-modal (str "payment-request-form" row-key)))} "Edit"]
                [:button.btn {:on-click #(remove-payment-request row-key)} "Delete"]])]
            ])))]]))

(defn table []
  (fn []
    (if-not (empty? @table-data)
      [:div
       [:legend "Payment Requests"]
       [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
        [:thead
         [:tr
          [:th "Botón de pago"]
          (if (or (= "admin" (session/get-in [:user :role]))
                  (= "finance" (session/get-in [:user :role]))) [:th "Lawyer" ])
          [:th "Client"]
          [:th "Service"]
          [:th "Amount"]
          [:th "Client type"]
          [:th "Last action"]
          [:th "Actions"]]]
        [:tbody
         (doall
          (for [row @table-data]
            (let [row-key (key row)
                values (apply merge (doall (map (fn [field]
                                                  {(keyword (key field)) (val field)})
                                                (get @table-data row-key))))]

              [:tr {:key row-key}
               [:td [:a {:href (str "/payments/pay/" (get values :code))
                         :data-toggle "tooltip"
                         :title "Este enlace fue enviado al cliente"
                         } "Pagar"]]
               (if (or (= "admin" (session/get-in [:user :role]))
                       (= "finance" (session/get-in [:user :role])))
                 [:td {:on-click #(do (session/assoc-in! [:payments :lawyer] (first (get values :lawyer_data)))
                                      (u/show-modal "lawyer-data-modal"))}
                  (get (first (get values :lawyer_data)) "name")])
               [:td (get values :client) ]
               [:td (get values :service)]
               [:td (get values :amount)]
               [:td (if (= "true" (get values :own_client)) "Own" "MisAbogados")]
               [:td {:on-click #(do (session/assoc-in! [:payments :payment-log] (get values :payment_log))
                                    (u/show-modal "payment-data-modal"))}
                (get (last (get values :payment_log)) "action")]
               [:td
                (if-not (get values :payment_log)
                  [:div.btn-group [:button.btn.btn-primary {:on-click #(do
                                                                         (u/show-modal (str "payment-request-form" row-key)))} "Edit"]
                   [:button.btn {:on-click #(remove-payment-request row-key)} "Delete"]])]
               ])))]]]
      [:h4 "You have no payment-requests"])))

(defn payments []
  (let [payment-requests (get-payment-requests)
        filters (r/cursor session/state [:filters :payment-requests])
        options (r/atom nil)
        util (r/atom nil)]
    (fn []
      (let [reqs-count (session/get-in [:payment-requests :count])
            total-pages (inc (/ (- reqs-count (mod reqs-count 10)) 10) )]
        (when-not (session/get-in [:payment-requests :page])
          (session/assoc-in! [:payment-requests :page] 1))

        [:div.container
         [:div.col-md-4
          [:h1 "PagoLegal"]
          (if (#{"admin" "finance" "lawyer"} (session/get-in [:user :role]))
            [:button.btn.btn-danger {:type :button
                                     :on-click (fn [] (do
                                                       (u/show-modal "payment-request-form")
                                                       (reset! form-data {})))} "Cobra online aqui >"])]
         [:div.col-md-8
          [:div.form-horizontal
           (doall (map #(% [filters options util]) [(el/input-text "Clients name" [:name])
                                                    (el/input-email "Clients email" [:email])
                                                    (el/input-datepicker "From" [:from-date])
                                                    (el/input-datepicker "To" [:to-date])
                                                    (el/input-checkbox "Pendiente" [:status-pending] {:div-class "col-xs-3"})
                                                    (el/input-checkbox "En proceso de pagar" [:status-in-process] {:div-class "col-xs-3"})
                                                    (el/input-checkbox "Pagado" [:status-paid] {:div-class "col-xs-3"})
                                                    (el/input-checkbox "Fallado" [:status-failed] {:div-class "col-xs-3"})
                                                    (el/input-checkbox "Cliente propio" [:own-client] {:div-class "col-xs-3"})
                                                    (el/input-checkbox "Cliente MisAbogados" [:misabogados-client] {:div-class "col-xs-3"})]))
           [:div.form-group.col-xs-12
            [:button.btn.btn-secondary {:on-click #(do (get-payment-requests)
                                                       (session/assoc-in! [:payment-requests :page] 1))}
             "Filtrar >"]]]]
         [table]
         [create-payment-request-form]
         [lawyer-data-modal]
         [payment-data-modal]
         (doall (for [row @table-data]
                  (let [row-key (key row)
                        values (apply merge (map (fn [field]
                                                   {(keyword (key field)) (val field)})
                                                 (get @table-data row-key)))]
                    [:div {:key row-key} [(edit-payment-request-form (into {:_id row-key} values))]]
                    )))
         [:ul.pagination
          [:li [:a {:on-click #(do (session/update-in! [:payment-requests :page]
                                                     (fn [x]
                                                       (if (> x 1)
                                                         (dec x)
                                                         x)))
                                   (get-payment-requests))}
                "«"]]
          (doall (for [page (range 1 (inc total-pages))]
                   [:li {:key page :class (if (= page (session/get-in [:payment-requests :page])) "active" "")}
                    [:a {:on-click #(do (session/assoc-in! [:payment-requests :page] page)
                                                        (get-payment-requests))}
                                     page]]))
          [:li [:a {:on-click #(do (session/update-in! [:payment-requests :page]
                                                       (fn [x]
                                                         (if (< x total-pages)
                                                           (inc x)
                                                           x)))
                                   (get-payment-requests))}
                "»"]]]]))
    ))
