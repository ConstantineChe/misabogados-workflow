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

(defn redirect [url]
  (aset js/window "location" url)
  (.scrollTo js/window 0 0))

(defn get-session!
  ([]
   (let [logged-in? (r/atom nil)]
     (GET (str js/context "/session")
          {:handler (fn [response]
                      (reset! logged-in? (nil? (get response "identity")))
                      (if-not @logged-in?
                        (session/put! :user {:identity (get response "identity" )
                                             :role (get response "role")}))
                      (session/put! :own-profile (get response "own-profile"))
                      (session/put! :filters {:payment-requests {:own-client true
                                                                 :misabogados-client true
                                                                 :status-pending true
                                                                 :status-in-process true
                                                                 :status-paid true
                                                                 :status-failed true}})
                      (ac/reset-access!)
                      (if (= (get response "role") "lawyer") (redirect "#payments"))
                      nil)})
     logged-in?))
  ([done]
   (let [logged-in? (r/atom nil)]
     (GET (str js/context "/session")
          {:handler (fn [response]
                      (reset! logged-in? (nil? (get response "identity")))
                      (reset! done true)
                      nil)})
     logged-in?)))

(defn enable-tooltips []
  (.tooltip (js/jQuery ".balloon-tooltip")))
