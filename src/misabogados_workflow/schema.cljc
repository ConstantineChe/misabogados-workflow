(ns misabogados-workflow.schema
  (:require
   [clojure.walk :as w]
   [inflections.core :as i]
)
  #?(:cljs (:require-macros [misabogados-workflow.schema :refer [defexpand]]))
   )


(defmacro defentity
  "Defines an entity. The data provided here is enough to tell what is the possible structure of stored entity and where it is stored. It is also enough to generate scaffold form to edit this entity"
  [name & fields]
  (let [name# (str name)]
    `(do (def ~(symbol name#) (entity ~(keyword name#) 
                                      ~fields))
         (defn ~(symbol (str name# "-get")) [] (println "GET " ~name#)))))

      ;; (let [name# (str name)]
      ;;   `(do (def ~(symbol name#) {:name (keyword ~name#)
      ;;                              :type :entity
      ;;                              :collection-name (i/plural (i/underscore ~name#))
      ;;                              :field-definitions [~@fields]
      ;;                              })
      ;;        (defn ~(symbol (str name# "-get")) [] (println "GET " ~name#)))))

(defn entity-schema [name mixin fields] {name (conj mixin {:field-definitions (reduce conj fields)})})

(defn entity
  "Defines an entity. The data provided here is enough to tell what is the possible structure of stored entity and where it is stored. It is also enough to generate scaffold form to edit this entity"
  [name & fields]
  (entity-schema (keyword name) 
                 {:type :entity
                  :collection-name name}
                 fields)
  
  )


;; Functions that define fields
(defn embeds-many [name & fields] (entity-schema name {:type :embadded-collection} fields))

(defn text-field [name] {name {
                               :type :text-field}})
(defn has-many [entity] {(key (first entity)) 
                         {
                                         :type :collection-refenence}})
(defn has-one [entity] {(key (first entity)) 
                        {
                                        :type :entity-refenence}})
(defn simple-dict-field [name] {name {
                                      :type :dictionary-reference}})
(defn embeds-one [name & fields] (entity-schema name {:type :embadded-entity} fields))
(defn datetime-field [name] {name {
                               :type :datetime-field}})

;; Definitions of entities. This is what we actually have to write in order to define entities in the system
(declare category lawyer lead client)

(defentity category
  (embeds-many :faq-item
               (text-field :question)
               (text-field :contents))
  (text-field :title))

(defentity lawyer
  (has-many category)
  (text-field :name))

(defentity client
  (text-field :name)
  (text-field :phone)
  (text-field :email))

;; (defentity lead
;;   (has-one client)
;;   (text-field :problem)
;;   (simple-dict-field :lead-type)
;;   (embeds-one :match
;;                (has-one lawyer)
;;                (embeds-many :meeting
;;                            (simple-dict-field :meeting-type)
;;                            (datetime-field :time))))

;; This is what entity definitions should be expanded to. This data structures holds all information abount entity and it's fields in format easily digestable programmatically.

(defmacro defexpand [name schema]
  `(def ~(symbol (str name "-expanded")) ~schema))

(defexpand category-macro
  (array-map
   :category
   {:render-type :entity
    :collection-name "categories"
    :field-definitions
    (array-map
     :name
     {:render-type :text
      :label "Name"}
     :quote
     {:render-type :text}
     :slug
     {:render-type :text}
     :persons
     {:render-type :checkbox}
     :enterprises
     {:render-type :checkbox}
     :showed_by_default
     {:render-type :checkbox}
     :meta_description
     {:render-type :markdown}
     :faq_items
     {:render-type :collection
      :entity-label "FAQ item"
      :field-definitions
      (array-map
       :name
       {:render-type :text}
       :text
       {:render-type :markdown})}
     :posts
     {:render-type :collection
      :entity-label "Post"
      :field-definitions
      (array-map
       :name
       {:render-type :text}
       :url
       {:render-type :text})})
     }))

(def category-schema-expanded
  (array-map
   :category
   {:render-type :entity
    :collection-name "categories"
    :field-definitions
    (array-map
     :name
     {:render-type :text
      :label "Name"}
     :quote
     {:render-type :text}
     :slug
     {:render-type :text}
     :persons
     {:render-type :checkbox}
     :enterprises
     {:render-type :checkbox}
     :showed_by_default
     {:render-type :checkbox}
     :meta_description
     {:render-type :markdown}
     :faq_items
     {:render-type :collection
      :entity-label "FAQ item"
      :field-definitions
      (array-map
       :name
       {:render-type :text}
       :text
       {:render-type :markdown})}
     :posts
     {:render-type :collection
      :entity-label "Post"
      :field-definitions
      (array-map
       :name
       {:render-type :text}
       :url
       {:render-type :text})})
     }))

(def lead-schema-expanded
  {:name :lead
   :type :entity
   :collection-name "leads"
   :field-definitions [{:name :client
                        :type :entity-reference}
                       {:name :match
                        :type :embedded-entity
                        :field-definitions [{:name :lawyer
                                             :type :entity-reference}
                                            {:name :meetings
                                             :type :embedded-collection
                                             :field-definitions [{:name :time
                                                                  :type :datetime-field}
                                                                 {:name :meeting_type
                                                                  :type :dictionary-reference}]}]}
                       {:name :lead_type
                        :type :text-field}
                       {:name :problem
                        :type :text-field}]})

;; This is the data that we can fetch from the database
(def category-data {:faq_items [{:question "What?" :contents "That!"}
                                {:question "Why?" :contents "Because!"}]
                    :name "Divorce"
                    :slug "divorce-1"
                    :description "your rel is over"})

(def lead-data {:problem "problem1"
                :type "case"
                :client_id 12
                :match {:lawyer_id 23
                        :meetings [{:type "meeting"
                                    :time "2016-01-03"}
                                   {:type "call"
                                    :time "2016-01-06"}]}})

;; This is the desired result of default rendering of the form. It uses schema-expanded (expanded from defentity macro) and data (later to be fetched from db)
;; (defn default-leads-form []
;;   (fn []
;;       [:div.container
;;        (el/form "Edit Lead" [lead-data options util]
;;                 ["Lead"
;;                  (el/input-typeahead "Client" [:lead :client])
;;                  (el/input-text "Type" [:lead :type])
;;                  (el/input-textarea "Problem" [:lead :problem])
;;                  ["Match"
;;                   (el/input-typeahead "Lawyer" [:lead :match :lawyer_id])
;;                   ["Meetings"
;;                    ["Meeting 0"
;;                     (el/input-text "Type" [:lead :match :meetings 0 :type])
;;                     (el/input-datetimepicker ["Date" "Time"]
;;                                              [:lead :match :meetings 0 :time])]
;;                    ["Meeting 1"
;;                     (el/input-text "Type" [:lead :match :meetings 0 :type])
;;                     (el/input-datetimepicker ["Date" "Time"]
;;                                              [:lead :match :meetings 0 :time])]]]])]))
