(ns misabogados-workflow.dashboard
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [reagent.session :as session]))

(defn dashboard []
  (let [users (r/atom {})
        error (r/atom nil)
        selected-cell (r/atom [])
        _ (GET (str js/context "/users")
               {:handler (fn [response]
                           (reset! users response) nil)
                :error-handler (fn [response] (reset! error (get "error" response)) nil)})]
    (fn []
      [:div.container
       (if (:role (session/get :user))
         [:legend (clojure.string/capitalize (:role (session/get :user)))  " Dashboard"])
       (if-not (nil? @error) [:p.error (str @error)])
       (if-let [users (seq @users)]
         [:div.container
          [:legend "Users"]
          [:p (str @selected-cell)]
          [:table.table.table-hover.table-striped.panel-body
           [:th "name"]
           [:th "email"]
           [:th "role"]
           [:th "verified"]
           (doall
            (for [user users]
              [:tr {:key (get user "email")}
               (let [name (r/atom (get user "name"))]
                 [:td {:on-click #(reset! selected-cell [(get user "email") "name"])
                       :on-blur #(do (reset! selected-cell []))}
                  (if (= @selected-cell [(get user "email") "name"])
                    [:input {:autoFocus "true" :type :text :value @name}]
                    @name)])
               [:td (get user "email")]
               [:td (get user "role")]
               [:td (str (get user "verified"))]]))]])])))
