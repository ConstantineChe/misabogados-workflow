(ns misabogados-workflow.schema
  (:require
   #?(:cljs [misabogados-workflow.elements :as el])
   #?(:cljs [reagent.core :as r])
   [clojure.walk :as w]
   [clojure.zip :as z]))


(def schema
  [:schema "Schema"
   [:field-outer "Outer" {:type :text}]
   [:fieldset "Fieldset"
    [:field-inner1 "Inner 1" {:type :text}]
    [:field-inner2 "Inner 2" {:type :text}]]])
