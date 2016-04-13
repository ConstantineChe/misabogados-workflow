(ns misabogados-workflow.schema
  (:require
   #?(:cljs [misabogados-workflow.elements :as el])
   #?(:cljs [reagent.core :as r])
   [clojure.walk :as w]
   [clojure.zip :as z]))


(def schema
  [:schema "Schema1"
   [:field-outer "outer field" {:type :text}]
   [:fieldset "Fieldset"
      [:field-inner1 ["Inner date" "Inner time"] {:type :date-time}]
      [:field-inner2 "Inner 2" {:type :text}]]
   [:fieldset "Fieldset"
    [:field-inner1 ["Inner date" "Inner time"] {:type :date-time}]
    [:field-inner2 "Inner 2" {:type :text}]]])
