(ns misabogados-workflow.utils
  (:require [misabogados-workflow.ajax :refer [GET]]
            [reagent.session :as session]
            [misabogados-workflow.access-control :as ac]
)
)


(defn show-modal [id]
  (->
   (js/jQuery (str "#" id))
   (.modal "show")))

(defn close-modal [id]
  (->
   (js/jQuery (str "#" id))
   (.modal "hide")))

(defn get-session! []
  (GET (str js/context "/session")
       {:handler (fn [response]
                   (if-not (nil? (get response "identity"))
                     (session/put! :user {:identity (get response "identity" )
                                          :role (get response "role")}))
                   (ac/reset-access!)
                   nil)}))
