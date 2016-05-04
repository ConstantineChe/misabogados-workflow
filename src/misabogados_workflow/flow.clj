(ns misabogados-workflow.flow
  (:require [misabogados-workflow.util :as util]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            ))



(defn get-action-buttons [actions dataset role]
  (map (fn [action-def]
         (if (contains? (:roles action-def) role)
           (let [label (name (:name action-def))
                 action (name (:action action-def))]
             [:button.btn.btn-primary
              {:type :submit
               :title (str "Saves and goes to \"" (util/remove-kebab action) "\"")
               :formaction (str "/lead/"
                                (get-in dataset [:lead :_id])
                                "/action/" action)}
              (util/remove-kebab label)])))
       actions))
