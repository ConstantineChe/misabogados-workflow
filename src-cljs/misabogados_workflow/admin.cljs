(ns misabogados-workflow.admin
  (:require [reagent.core :as r]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [reagent.session :as session]))

(def table-data (r/atom {}))

(defn cell [col item id selected-cell]
  (let [value (get-in @table-data [id col])]
                 [:td {:on-click #(reset! selected-cell [id col])
                       :on-blur #(do (PUT (str js/context "/users/" (get item "email")) {:params {:id (get item "email")
                                                                             :data {col (-> % .-target .-value)}}
                                                                       :handler (fn [response]

                                                                                  nil)
                                                                       :error-handler (fn [response]
                                                                                        (js/alert (str "error: " response))
                                                                                        nil)})
                                     (reset! selected-cell []))}
                  (if (= @selected-cell [id col])
                    [:input {:autoFocus "true"
                             :type :text
                             :on-change #(reset! table-data (assoc-in @table-data [id col] (-> % .-target .-value)))
                             :value value}]
                    value)]))

(defn admin []
  (let [error (r/atom nil)
        selected-cell (r/atom [])
        _ (GET (str js/context "/users")
               {:handler (fn [response]
                           (reset! table-data (into {} (map (fn [item]
                                                                 {(get item "_id") item})
                                                               response))) nil)
                :error-handler (fn [response] (reset! error (get "error" response)) nil)})]
    (fn []
      [:div.container
       (if (:role (session/get :user))
         [:legend (clojure.string/capitalize (:role (session/get :user)))  " Dashboard"])
       (if-not (nil? @error) [:p.error (str @error)])
       (if-not (empty? @table-data)
         [:div.container
          [:legend "Users"]
          [:table.table.table-hover.table-striped.panel-body {:style {:width "100%"}}
           [:th "name"]
           [:th "email"]
           [:th "role"]
           [:th "verified"]
           (doall
            (for [user @table-data]
              [:tr {:key (key user)}
               (cell "name" (val user) (key user) selected-cell)
               (cell "email" (val user) (key user) selected-cell)
               (cell "role" (val user) (key user) selected-cell)
               (cell "verified" (val user) (key user) selected-cell)]))]])])))
