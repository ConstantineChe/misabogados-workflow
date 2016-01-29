(ns misabogados-workflow.model
  (:require [hiccup.form :as el]))

(defprotocol PLead
  (traverse [this] "traverse lead"))

(defn traverse-fn [item] (if (satisfies? PLead (val item))
                           (.traverse (val item)) (el/text-field (key item) (val item))))

(defrecord Lead [user basic-info]
  PLead
  (traverse [this] (map traverse-fn this)))

(defrecord User [name etc]
  PLead
  (traverse [this] (map traverse-fn this)))

(defrecord BasicInfo [date-created status]
  PLead
  (traverse [this] (map traverse-fn this)))


(def datas (->Lead (->User "Spurdo" "Sprade") (->BasicInfo (str (new java.util.Date)) "new")))
