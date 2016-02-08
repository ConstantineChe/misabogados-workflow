(ns misabogados-workflow.util
  (:require [clojure.string :as str]
            [misabogados-workflow.flow-definition :refer [steps]]
            [misabogados-workflow.flow :refer []]))

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (if str (str/capitalize (str/replace str "-" " "))))
