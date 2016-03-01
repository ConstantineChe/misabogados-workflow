(ns misabogados-workflow.utils
  (:require [misabogados-workflow.ajax :refer [GET]]
            [reagent.session :as session]
            [misabogados-workflow.access-control :as ac])
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

(defn get-session! []
  (GET (str js/context "/session")
       {:handler (fn [response]
                   (if-not (nil? (get response "identity"))
                     (session/put! :user {:identity (get response "identity" )
                                          :role (get response "role")}))
                   (ac/reset-access!)
                   nil)}))
