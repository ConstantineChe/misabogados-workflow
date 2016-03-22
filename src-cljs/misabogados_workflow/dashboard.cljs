(ns misabogados-workflow.dashboard
  (:require [reagent.core :as r]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [reagent.session :as session]
            [misabogados-workflow.utils :as util]))


(def table-data (r/atom {}))

(defn get-actions [lead]
  (list [:a {:class "btn btn-secondary"
             :href (str "/lead/" (get lead "_id") "/edit")} [:span.glyphicon.glyphicon-edit]]
        [:a {:class "btn btn-success"
             :href (str "/lead/" (get lead "_id") "/action/" (get lead "step"))} [:span.glyphicon.glyphicon-play]]))

(defn table []
  (let []
    [:div
     [:legend "Leads"]
     (if-not (empty? @table-data)
       [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
        [:th "ID"]
        [:th "User name"]
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
         (doall (for [lead @table-data
                      :let [id (get lead "_id")
                            name (get-in lead ["user" "name"])
                            pending-action (util/remove-kebab (get lead "step"))
                            actions (get-actions lead)]]
                  [:tr {:key id}
                   [:td id]
                   [:td name]
                   [:td pending-action]
                   [:td (get-in lead ["lead_type" 0 "name"])]
                   [:td "Nombre de usuario"]
                   [:td (get lead "client_email")]
                   [:td (get-in lead ["category" 0 "name"])]
                   [:td "Telefono"]
                   [:td (get lead "problem")]
                   [:td (get lead "region_id")]
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
       [:a {:class "btn btn-primary"
            :href "/#lead"} "New Lead"]
       [table]])))
