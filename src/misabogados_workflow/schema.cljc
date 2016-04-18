(ns misabogados-workflow.schema
  (:require
   #?(:cljs [misabogados-workflow.elements :as el])
   #?(:cljs [reagent.core :as r])
   [clojure.walk :as w]
   [clojure.zip :as z]
   [inflections.core :refer :all]))

(defmacro defentity
  "Defines an entity. The data provided here is enough to tell what is the
possible structure of stored entity and where it is stored.
It is also enough to generate scaffold form to edit this entity"
  [name & fields]

  (let [name# (str name)]
    `(do (def ~(symbol name#) {:name (keyword ~name#)
                               :type :entity
                               :collection-name (plural (underscore ~name#))
                               :field-definitions [~@fields]
                               })
         (defn ~(symbol (str name# "-get")) [] (println "GET " ~name#)))))


;; Functions that define fields
(defn embeds-many [name & fields]
  {:name name
   :type :embedded-collection
   :field-definitions (into [] fields)})

(defn text-field [name] {:name name
                         :type :text-field})
(defn has-many [entity] {:name (:name entity)
                         :type :collection-refenence})
(defn has-one [entity] {:name (:name entity)
                        :type :entity-refenence})
(defn simple-dict-field [name] {:name name
                                :type :dictionary-reference})
(defn embeds-one [name & fields] {:name name
                                  :type :embedded-entity
                                  :field-definitions (into [] fields)})
(defn datetime-field [name] {:name name
                             :type :datetime-field})

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
  (text-field :phone))

(defentity lead
  (has-one client)
  (text-field :problem)
  (simple-dict-field :lead-type)
  (embeds-one :match
               (has-one lawyer)
               (embeds-many :meeting
                           (simple-dict-field :meeting-type)
                           (datetime-field :time))))

;; This is what entity definitions should be expanded to. This data structures holds all information abount entity and it's fields in format easily digestable programmatically.
(def category-schema-expanded
  {:name :category
   :type :entity
   :collection-name "categories"
   :field-definitions [{:name :faq-item
                        :type :embedded-collection
                        :field-definitions [{:name :question
                                             :type :text-field}
                                            {:name :contents
                                             :type :markdown-field}]}
                       {:name :post
                        :type :embedded-collection
                        :field-definitions [{:name :title
                                             :type :text-field}
                                            {:name :link
                                             :type :url-field}]}
                       {:name :name
                        :type :text-field
                        :label "Name"}
                       {:name :slug
                        :type :text-field}
                       {:name :description
                        :type :markdown-field}]})

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
