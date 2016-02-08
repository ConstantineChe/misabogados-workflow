(ns misabogados-workflow.util
  (:require [clojure.string :as str]))

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (if str (str/capitalize (str/replace str "-" " "))))

(defn get-button-class [action]
  )
