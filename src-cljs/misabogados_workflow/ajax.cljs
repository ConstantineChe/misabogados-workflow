(ns misabogados-workflow.ajax
  (:require [ajax.core :as ajax]
            [reagent.core :as r]))

(def csrf-token (r/atom nil))

(defn update-csrf-token! []
  (ajax/GET (str js/context "/csrf-token") {:handler #(reset! csrf-token (get % "token"))}))

(defn GET [url body]
  (ajax/GET url body))

(defn POST [url body]
  (ajax/POST url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token}))))

(defn PUT [url body]
  (ajax/PUT url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token}))))

(defn DELETE [url body]
  (ajax/DELETE url (if-not (contains? :headers body)
                   (assoc body :headers {:X-CSRF-Token @csrf-token})
                   (assoc-in body [:headers] {:X-CSRF-Token @csrf-token}))))
