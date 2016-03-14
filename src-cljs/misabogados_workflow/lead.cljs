(ns misabogados-workflow.lead
    (:require [reagent.core :as r]
              [misabogados-workflow.utils :as u]
              [reagent.session :as session]
              [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
              [misabogados-workflow.elements :as el]
              [bouncer.core :as b]
              [bouncer.validators :as v]
              [json-html.core :refer [edn->hiccup]]))


(defn lead []
  (let [lead-data (r/atom {:base {:name "Test"
                                  :email "no@mail.re"
                                  :count 15
                                  :check true}
                           :etc {:etc "hahhahahhaha"}})]
    (fn []
      [:div.container
       (str @lead-data)
       (el/form "Lead" lead-data
                ["Base"
                 (el/input-text "Name" [:base :name])
                 (el/input-email "Email" [:base :email])
                 (el/input-number "Count" [:base :count])
                 (el/input-checkbox "Check" [:base :check])]
                ["Etc"
                 (el/input-text "Etc" [:etc :etc])])])))
