(ns misabogados-workflow.dashboard
  (:require [reagent.core :as r]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [reagent.session :as session]
            [misabogados-workflow.utils :as util]
            [misabogados-workflow.flow-definition :refer [steps]]))


(def table-data (r/atom {}))

(def show-all (r/atom false))


(defn get-actions [lead]
  (let [step (if (get lead "step") (get lead "step") "pitch")]
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
         (doall (for [lead @table-data
                      :let [id (get lead "_id")
                            pending-action (util/remove-kebab (get lead "step"))
                            actions (get-actions lead)]]
                  [:tr {:key id}
                   [:td id]
                   [:td pending-action]
                   [:td (get-in lead ["lead_type" 0 "name"])]
                   [:td (get-in lead ["client" 0 "name"])]
                   [:td (get-in lead ["client"  0 "email"])]
                   [:td (get-in lead ["category" 0 "name"])]
                   [:td (get-in lead ["client" 0 "phone"])]
                   [:td (let [problem (get lead "problem")]
                          (if (< 80 (count problem)) (str (subs problem  0 80) "...") problem))]
                   [:td (get lead "region_code")]
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
  (let [leads (GET (str js/context "/leads") {:params {:per-page 20
                                                       :page 1
                                                       :sort-field :_id
                                                       :sort-dir -1}
                                              :handler #(reset! table-data (get % "leads"))
                                              :error-handler #(js/alert (str %))})]
    (fn []
      [:div.container-fluid
       [:h3 "Dashboard"]
       (when-let [notification (session/get :notification)]
       (js/setTimeout #(session/put! :notification nil) 10000)
                  notification)
       [:a {:class "btn btn-primary"
            :href "#lead"} "New Lead"]
       [table]])))
