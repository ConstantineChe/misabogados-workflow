(ns misabogados-workflow.ajax
  (:require [ajax.core :as ajax]
            [reagent.core :as r]
            [misabogados-workflow.websockets :as w]))

(def csrf-token (r/atom nil))

(defn extend-session []
  (try (w/send-transit-msg! {:code :extend})
       (catch js/Object e (prn e))))

(defn update-csrf-token! []
  (ajax/GET (str js/context "/csrf-token") {:handler #(reset! csrf-token (get % "token"))})
)

(defn GET [url body]
  (ajax/GET url body)
(extend-session))

(defn POST [url body]
  (ajax/POST url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token})))
(extend-session))

(defn PUT [url body]
  (ajax/PUT url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token})))
(extend-session))

(defn DELETE [url body]
  (ajax/DELETE url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token})))
(extend-session))
