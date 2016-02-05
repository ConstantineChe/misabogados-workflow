(ns misabogados-workflow.util
  (:require [clojure.string :as str]))

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (str/capitalize (str/replace str "-" " ")))
