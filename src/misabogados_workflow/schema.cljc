(ns misabogados-workflow.schema
  (:require
   [clojure.walk :as w]
   [inflections.core :as i]
)
  #?(:cljs (:require-macros [misabogados-workflow.schema :refer [defentity]]))
   )

(defn entity-schema [name mixin fields]
  [name (merge mixin {:field-definitions  (apply array-map (apply concat fields))})])

(defn entity
  "Creates entity schema structure"
  [name label & fields]
  (apply array-map
         (entity-schema (keyword name)
                        {:type :embadded-entity
                         :render-type :entity
                         :label label
                         :collection-name name}
                        fields))

)

(defmacro defentity
  "Defines an entity. The data provided here is enough to tell what is the possible structure
  of stored entity and where it is stored. It is also enough to generate scaffold form to edit this entity"
  [name label & fields]
  (let [name# (str name)]
    (do `(defn ~(symbol (str name# "-get")) [] (println "GET " ~name#))

        `(def ~(symbol name#) (entity ~(keyword name#)  ~label
                                   ~@fields)))    ))



;; Functions that define fields
(defn embeds-many [name label & fields] (entity-schema name
                                                       {:type :embadded-collection
                                                        :label label
                                                        :entity-label (i/singular label)
                                                        :render-type :collection}
                                                  fields))

(defn text-field [name label] [name {:render-type :text :label label}])

(defn field [type name label & args][name {:render-type type :label label :args args}])

(defn has-many [entity] {(key (first entity))
                         {:type :collection-refenence}})

(defn has-one [entity] [(key (first entity))
                        {:render-type :typeahead
                         :type :entity-refenence}])

(defn checkbox-field [name label]
  [name {:render-type :checkbox
         :type :boolean
         :label label}])

(defn markdown-field [name label]
  [name {:render-type :markdown
         :type :markdown
         :label label}])

(defn simple-dict-field [name] {name {:type :dictionary-reference}})

(defn embeds-one [name label & fields] (entity-schema name
                                                      {:type :embadded-entity
                                                       :label label
                                                       :render-type :entity}
                                                fields))

(defn datetime-field [name] {name {:type :datetime-field}})

;; Definitions of entities. This is what we actually have to write in order to define entities in the system
(declare category)


(defentity category "Categoria"
  (text-field :name "Title")
  (text-field :quote "Quote")
  (text-field :slug "Slug")
  (markdown-field :intro "Intro")
  (markdown-field :pricing "Pricing description")
  (field :image :image "Image" "/admin/categories/file")
  (checkbox-field :persons "Persons")
  (checkbox-field :enterprises "Enterprises")
  (checkbox-field :showed_by_default "Showed by default")
  (embeds-many :faq_items "FAQ items"
               (text-field :name "Question")
               (markdown-field :text "Contents"))
  (embeds-many :posts "Posts"
               (text-field :name "Title")
               (text-field :url "URL")))

(defentity client "Cliente"
  (text-field :name "Nombre")
  (field :email :email "Email")
  (text-field :phone "Teléfono"))

(defentity lawyer "Abogado"
  (text-field :name "Nombre")
  (text-field :email "Email")
  (text-field :phone "Teléfono")
  (text-field :address "Dirección")
  (text-field :years_of_experience "Years of experience")
  (text-field :slug "Slug")
  (field :number :rating "Rating")
  (field :date-time :join_date ["miembro desde" "."])
  (field :image :profile_picture "Profile picture" "admin/lawyers/file")
  (checkbox-field :certified "Certified lawyer")
  (field :textarea :description "Description")
  (field :textarea :quote "Quote")
  (embeds-many :experience "Experience"
               (text-field :place "Where")
               (text-field :position "Position")
               (text-field :from "From")
               (text-field :to "To"))
  (embeds-many :study "Study"
               (text-field :place "Where")
               (text-field :from "From")
               (text-field :to "To")
               (text-field :degree "Degree"))
  (embeds-many :feedback "Feedback"
               (text-field :client_name "Clients name")
               (field :textarea :text "Text")))

(defentity settings "Ajustes"
  (text-field :full_country_name "País")
  (text-field :contact_phone "Teléfono")
  (text-field :contact_email "Email")
  (text-field :whatsapp "Whatsapp")
  (text-field :phone_code "Código de telefono")
  ;; (text-field :country "Código del país")
  (text-field :base_url "Enlace básico")
  (checkbox-field :private_app_disabled "Private app disabled")
  (text-field :payment_system "Payment system")
  (text-field :currency "Currency")
  (embeds-many :regions "Regions"
               (text-field :code "Code")
               (text-field :name "Name"))
  ;; (embeds-one :custom-email-templates "Custom email templates")
  (text-field :derivation "Derivation email")
  (text-field :meeting "Meeting email")
  (text-field :phone_coordination "Phone coordination email")
  (text-field :thanks "Thanks email")
  (text-field :extension "Extension email")
  (embeds-many :payment_system_options "Payment system options"
               (text-field :key "Key")
               (text-field :value "Value")))

(defentity lead "Lead"
  (field :input-entity :client_id "Client" {:url "/users/client"
                                            :create-legend "Create Client"
                                            :edit-legend "Edit Client"
                                            :label-fn (fn [entity] (str (:name entity) " (" (:email entity) ")"))
                                            :schema client})
  (field :dropdown :region_code "Region")
  (text-field :city "City")
  (field :typeahead :category_id "Category")
  (field :dropdown :lead_type_code "Lead Type")
  (field :dropdown :lead_source_code "Lead Source")
  (text-field :refer "Referrer")
  (field :number :nps "NPS")
  (text-field :adwords_url "Adowrds url")
  (field :number :amount "Amount")
  (field :textarea :problem "Problem")
  (embeds-many :matches "Matches"
               (field :typeahead :lawyer_id "Lawyer")
               (embeds-many :meetings "Meetings"
                            (text-field :type "Type")
                            (field :date-time :time ["Date" "Time"]))))

;; This is what entity definitions should be expanded to. This data structures holds all information abount entity and it's fields in format easily digestable programmatically.


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
