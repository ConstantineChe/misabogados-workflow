(ns misabogados-workflow.utils
)

(def jquery (js* "$"))

(defn show-modal [id]
  (->
   (jquery (str "#" id))
   (.modal "show")))

(defn close-modal [id]
  (->
   (jquery (str "#" id))
   (.modal "hide")))
