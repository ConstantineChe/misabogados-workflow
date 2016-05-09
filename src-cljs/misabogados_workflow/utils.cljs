(ns misabogados-workflow.utils
  (:require [misabogados-workflow.ajax :refer [GET]]
            [reagent.core :as r]
            [reagent.session :as session]
            [misabogados-workflow.access-control :as ac]
            [clojure.string :as str]
)
)

(defn remove-kebab [str]
  "removes kebab and makes string human-eatable"
  (if str (str/capitalize (str/replace str "-" " "))))

(defn show-modal [id]
  (->
   (js/jQuery (str "#" id))
   (.modal #js {:keyboard true})))

(defn close-modal [id]
  (->
   (js/jQuery (str "#" id))
   (.modal "hide")))

(defn get-session! []
  (let [logged-in? (r/atom nil)]
    (GET (str js/context "/session")
         {:handler (fn [response]
                     (reset! logged-in? (nil? (get response "identity")))
                     (if-not @logged-in?
                       (session/put! :user {:identity (get response "identity" )
                                            :role (get response "role")}))
                     (ac/reset-access!)
                     nil)})
    logged-in?))

(defn redirect [url]
  (aset js/window "location" url)
  (.scrollTo js/window 0 0))
