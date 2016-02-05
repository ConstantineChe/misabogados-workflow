(ns misabogados-workflow.model
  (:require [misabogados-workflow.layout.elements :as elem]
            [clojure.walk :as w]))

(defprotocol PRenderable
    )

(defprotocol PEntity
  (render [this ancestry] "render field"))

(defprotocol PField
  (render [this ancestry] "render field"))

(defn render-fn [ancestry item] (println ancestry)
  (cond (satisfies? PEntity (val item))
        [:fieldset [:legend (key item)] (.render (val item) (conj ancestry (name (key item))))]
        (satisfies? PField (val item))
        (.render (val item) ancestry)))

(defn field-name [name ancestry]
  (if-not (empty? ancestry)
    (reduce #(str %1 "[" %2 "]") (conj ancestry name))))

(defrecord Lead [user basic-info match]
  PEntity
  (render [this ancestry] (map (partial render-fn [ancestry]) this)))

(deftype TextField [label name value]
  PField
  (render [this ancestry] (elem/input-text label (field-name name ancestry) value)))

(defrecord Match [date meeting]
  PEntity
  (render [this ancestry] (map (partial render-fn ancestry) this)))

(defrecord Meeting [date]
  PEntity
  (render [this ancestry] (map (partial render-fn ancestry) this)))

(defrecord User [^TextField name ^TextField etc]
  PEntity
  (render [this ancestry] (map (partial render-fn ancestry) this)))

(defrecord BasicInfo [^TextField date-created ^TextField status]
  PEntity
  (render [this ancestry] (map (partial render-fn ancestry) this)))


(.getFields (map->User {}))


(def datas (->Lead (->User (->TextField "Name" "name" "")
                           (->TextField "Etc" "etc" ""))
                   (->BasicInfo (->TextField "Date created" "date_created"
                                             "")
                                (->TextField "Status" "status" ""))
                   (->Match (->TextField "Date" "date" "")
                            (->Meeting (->TextField "Date" "date" "")))
                   ))

(defprotocol PTest
  (describe-fields [this]))

(defrecord Test [^String name ^TextField test]
  PTest
  (describe-fields [this] (meta name)))
