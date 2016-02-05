(ns misabogados-workflow.flow
  (:require [misabogados-workflow.model :refer :all]
            [misabogados-workflow.util :as util]
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
                     }})

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
    ((get-factory  key)  (map (partial get-struct-args (conj ancestry key) dataset)  children)
                               )))
(defn record-exist? [key]
  (resolve (symbol (str "misabogados-workflow.model/map->" (->PascalCaseString key)))))

(record-exist? :name)

(defn populate-struct [struct data ancestry]
  (map (fn [field]
         (let [key (key field)
               val (val field)]
           (if (nil? val)
             (if (record-exist? key) nil
                 {key (->TextField  (name key) (name key) (get-in  data (vec (map ->snake_case_keyword (conj ancestry key))) ))})
             {key ((get-factory key) (populate-struct val data (conj ancestry key)))}))) struct))



(defrecord Step [fieldset actions]
  PManual
  (create-form [this dataset] (list 
                               (.render (map->Lead (populate-struct (get-struct
                                                                     fieldset (update-in dataset [:lead] dissoc :_id) []) dataset [:lead])) "lead")
                               [:div.btn-group {:role "group"} 
                                [:button.btn.btn-secondary "Save"]
                                (map (fn [[button action]] [:button.btn.btn-primary 
                                                   {:type :submit
                                                    :formaction (str "/lead/"
                                                                     (get-in dataset [:lead :_id])
                                                                     "/action/" (name action))} (util/remove-kebab (name button))]) actions)])))

(def steps {:create (->Step [:lead :user :basic-info] {})
            :check (->Step [:lead :user :basic-info] {:finalize :archive 
                                                      :refer :find-lawyer})
            :find-lawyer (->Step [:lead :user :basic-info :match] {:done :arrange-meeting 
                                                                   :finalize :archive})
            :arrange-meeting (->Step [:lead :user :basic-info [:match :meeting]] {:change-lawyer :find-lawyer 
                                                                                  :done :archive})
            :archive (->Step [:lead :user :basic-info [:match :meeting]] {:reopen :check})})
