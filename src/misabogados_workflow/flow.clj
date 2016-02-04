(ns misabogados-workflow.flow
  (:require [misabogados-workflow.model :refer :all]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            )
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]
           ))

(defprotocol PManual
  (create-form [this dataset]))

(def dataset {:lead {:_id "test"
                     :user {:name "myname" :etc "some"}
                     :basic_info {:date_created "date" :status "new"}
                     :match {:date nil
                             :meeting {:date nil}}}})

(declare get-struct)

(defn get-key [fieldset]
  (if (vector? fieldset)
    (first fieldset)
    fieldset))

(defn get-children [fieldset]
  (if (vector? fieldset)
    (rest fieldset)
    []))

(defn render-field [path data]
  (let [path (map ->snake_case_keyword path)]
    (apply merge
           (map (fn [field] (if-not (map? (val field))
                             {(key field) (->TextField (name (key field)) (name (key field)) (val field))}))
                (get-in data path)))))

(defn get-factory [entity]
  (fn [map] ((resolve (symbol (str "misabogados-workflow.model/map->" (->PascalCaseString entity)))) map)))

(defn get-struct-args [ancestry dataset child]
  {(get-key child) (get-struct
                    child
                    dataset
                     ancestry)}
  )

(defn get-struct [fieldset dataset ancestry]
  (let [key (get-key fieldset)
        children (get-children fieldset)]
    ((get-factory  key) (merge (map (partial get-struct-args (conj ancestry key) dataset)  children)
                               (render-field (conj ancestry key) dataset)))))




(defrecord Step [fieldset actions]
  PManual
  (create-form [this dataset] (list (.render  (get-struct fieldset (update-in dataset [:lead] dissoc :_id) []) "lead")
                                    (map (fn [button] [:button {:type :submit
                                                               :formaction (str "/lead/"
                                                                                (get-in dataset [:lead :_id])
                                                                                "/action/" (name button))} button]) actions))))

(def steps {:create (->Step [:lead :user :basic-info] [])
            :check (->Step [:lead :user :basic-info] [:finish :find-lawyer])
            :find-lawyer (->Step [:lead :user :basic-info :match] [:create-meeting :finish])
            :finish (->Step [:leaad :user :basic-info] [:save])})
