(ns misabogados-workflow.dashboard
  (:require [reagent.core :as r]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [reagent.session :as session]
            [misabogados-workflow.utils :as util]
            [misabogados-workflow.flow-definition :refer [steps]]))


(def table-data (r/atom {}))

(def show-all (r/atom false))


(defn get-actions [lead]
  (let [step (if (get lead "step") (get lead "step") "check")]
    (list [:a {:key :edit
               :class "btn btn-secondary"
               :href (str "#lead/" (get lead "_id") "/edit")} [:span.glyphicon.glyphicon-edit] " Edit"]
          [:a {:key :action
               :class "btn btn-success"
               :href (str "#lead/" (get lead "_id") "/action/" step)} [:span.glyphicon.glyphicon-play] " " (util/remove-kebab step)])))

(defn table []
  (let []
    [:div
     [:legend "Leads"]
     [:button.btn.btn-default {:on-click #(swap! show-all not)} (if @show-all "Show less" "Show all")]
     (if-not (empty? @table-data)
       [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
        [:th "ID"]
        [:th "Pedning action"]
        [:th "Tipo"]
        [:th "Nombre de usuario"]
        [:th "Email de usuario"]
        [:th "Categoría"]
        [:th "Telefono"]
        [:th "Problema"]
        [:th "Region"]
        [:th "Ciudad"]
        [:th "Fuente"]
        [:th "Abogado"]
        [:th "Enlaces"]
        [:th ""]
        [:tbody
         (doall (for [lead (if @show-all @table-data (take 10 @table-data))
                      :let [id (get lead "_id")
                            pending-action (util/remove-kebab (get lead "step"))
                            actions (get-actions lead)]]
                  [:tr {:key id}
                   [:td id]
                   [:td pending-action]
                   [:td (get-in lead ["lead_type" 0 "name"])]
                   [:td (get-in lead ["client" 0 "name"])]
                   [:td (get lead "client_email")]
                   [:td (get-in lead ["category" 0 "name"])]
                   [:td (get-in lead ["client" 0 "phone"])]
                   [:td (get lead "problem")]
                   [:td (get lead "region_name")]
                   [:td (get lead "city")]
                   [:td (get lead "lead_source_code")]
                   [:td (get-in lead ["lawyer" 0 "name"])]
                   [:td
                    (if (get lead "referrer") [:a {:href (get lead "referrer")} "Referrer "] "")
                    (if (get lead "adwords_url") [:a {:href (get lead "adwords_url")} "Adwords"] "")]
                   [:td actions]]
                  ))]]
       [:p "there are no leads to show."])]))

(defn dashboard []
  (let [leads (GET (str js/context "/leads") {:handler #(reset! table-data (get % "leads"))
                                :error-handler #(js/alert (str %))})]
    (fn []
      [:div.container
       [:h3 "Dashboard"]
       (when-let [notification (session/get :notification)]
       (js/setTimeout #(session/put! :notification nil) 5000)
                  notification)
       [:a {:class "btn btn-primary"
            :href "#lead"} "New Lead"]
       [table]])))
