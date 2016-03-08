(ns misabogados-workflow.dashboard
  (:require [reagent.core :as r]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [reagent.session :as session]
            [misabogados-workflow.utils :as util]))


(def table-data (r/atom {}))

(defn get-actions [lead]
  [:a {:class "btn btn-success"
       :href (str "/lead/" (get lead "_id") "/action/" (get lead "step"))} [:span.glyphicon.glyphicon-play]])

(defn table []
  (let []
    [:div
     [:legend "Leads"]
     (if-not (empty? @table-data)
       [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
        [:th "ID"]
        [:th "User name"]
        [:th "Pedning action"]
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
            :href "/leads/create"} "New Lead"]
       [table]])))
