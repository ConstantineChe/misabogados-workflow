(ns misabogados-workflow.model
  (:require [misabogados-workflow.layout.elements :as elem]))

(defprotocol PLead
  (traverse [this] "traverse lead and build fields"))

(defprotocol PField
  (render [this] "render field"))

(defn traverse-fn [item] (cond (satisfies? PLead (val item))
                               (.traverse (val item))
                               (satisfies? PField (val item))
                               (.render (val item))))

(defrecord Lead [user basic-info]
  PLead
  (traverse [this] (map traverse-fn this)))

(defrecord User [name etc]
  PLead
  (traverse [this] (map traverse-fn this)))

(defrecord BasicInfo [date-created status]
  PLead
  (traverse [this] (map traverse-fn this)))

(deftype TextField [label name value]
  PField
  (render [this] (elem/input-text label name value)))

(def datas (->Lead (->User (->TextField "Name" "name" "Spurdo")
                           (->TextField "Etc" "etc" "Sprade"))
                   (->BasicInfo (->TextField "Date created" "date_creared"
                                             (str (new java.util.Date)))
                                (->TextField "Status" "status" "new"))))
