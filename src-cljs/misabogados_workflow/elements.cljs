(ns misabogados-workflow.elements
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as s]
            [misabogados-workflow.utils :as u]
            [markdown.core :refer [md->html]]
            [reagent-forms.datepicker
             :refer [parse-format format-date datepicker]])
)

(defn gen-name [cursor] (->> cursor (into []) (map #(if (keyword? %) (name %) %)) (interpose "-") (apply str)))

(defn drop-nth [coll n]
   (keep-indexed #(if (not= %1 n) %2) coll))

(defn prepare-input [cursor form]
  ((juxt gen-name #(r/cursor form (into [] %))) cursor))

(defn typeahead [form options util label cursor min-chars & addons]
  (let [f-opts (r/cursor util (into [:typeahead] cursor))
        text (r/cursor util (into [:typeahead-t] cursor))
        dropdown-class (r/cursor util (into [:typeahead-c] cursor))
        selected-index (r/cursor util (into [:typeahead-i] cursor))
        opt-cursor (r/cursor options (->> cursor (filter keyword?) vec))
        options (get-in @options (->> cursor (filter keyword?) vec))
        [name cursor] (prepare-input cursor form)
        match #(some (fn [[l v]] (when (= v %) l)) options)
        select (fn [[l v]] (do (reset! cursor v)
                              (reset! text l)
                              (reset! dropdown-class "")))
        dropdown-items (map-indexed (fn [i [label value :as option]]
                            [:li {:key i
                                  :on-mouse-over #(reset! selected-index i)
                                  :class (if (= i @selected-index) "active" "")
                                  :on-click #(select option)
                                  :on-focus #(reset! cursor value)}
                             [:a label]])
                          (if @f-opts @f-opts options))
        input [:input.form-control
               {:type :text
                :key :input
                :value @text
                :on-click #(.setSelectionRange (.-target %) 0 (count @text))
                :on-focus #(js/setTimeout (fn [_] (reset! dropdown-class "open")) 100)
                :on-blur #(js/setTimeout (fn [_] (do (reset! dropdown-class "")
                                             (reset! text (match @cursor)))) 100)
                :on-change #(do (reset! text (-> % .-target .-value))
                                (when-not (< (count @text) min-chars)
                                  (reset! f-opts (filter
                                                  (fn [[l]] (s/includes? (s/lower-case l) (s/lower-case @text)))
                                                  options))
                                  (reset! selected-index -1)))
                :on-key-down #(do
                                (case (.-which %)
                                  38 (do
                                       (.preventDefault %)
                                       (when-not (= @selected-index 0)
                                         (swap! selected-index dec)))
                                  40 (do
                                       (.preventDefault %)
                                       (when-not (= @selected-index (dec (count (if @f-opts @f-opts options))))
                                         (swap! selected-index inc)))
                                  9  (do (.preventDefault %)
                                         (select (get (if @f-opts (vec @f-opts) options) @selected-index)) (-> % .-target .blur))
                                  13 (do (select (get (if @f-opts (vec @f-opts) options) @selected-index)) (-> % .-target .blur))
                                  27 (do (reset! dropdown-class ""))
                           "default"))}]]
      (when (nil? @text) (reset! text (match @cursor)))
      (when (nil? @selected-index) (reset! selected-index -1))
      (when (seq? @opt-cursor) (dorun (map (fn [[l v]] (if (= v @cursor) (reset! text l))) @opt-cursor))
            (swap! opt-cursor vec))
      (when (and (not @f-opts) (>(count @text) 3)) (reset! f-opts (filter
                                                                   (fn [[l]] (s/includes? (s/lower-case l) (s/lower-case @text)))
                                                        options)))
      [:div.form-group.col-xs-6 {:id (str name "-g") :key name}
       [:label.control-label {:for name} label]
       (if addons [:div.input-group input addons]
         input)
       [:div.dropdown {:class @dropdown-class}
        (if (< (count @text) min-chars)
          [:ul.dropdown-menu
           {:id name
            :role :menu}
           [:li [:a  "type " (- min-chars (count @text)) " more characters"]]]
          (into [:ul.dropdown-menu
                 {:id name
                  :role :menu
                  :style {:height :auto :max-height :200px :overflow-x :hidden}}]
                dropdown-items
                ))]]))

(defn input [type label cursor]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group.col-xs-6 {:key name}
       [:label.control-label {:for name} label]
       [:input.form-control {:type type
                             :id name
                             :value @cursor
                             :on-change #(reset! cursor (-> % .-target .-value))
                             }]])))

(defn input-checkbox [label cursor]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group.col-xs-6 {:key name}
       [:label.control-label {:for name} label]
       [:input.form-control (merge {:type :checkbox
                                    :on-change #(swap! cursor not)
                                    :id name}
                                   (if @cursor {:checked :true}))]])))

(defn input-dropdown [label cursor]
  (fn [[form options]]
    (let [options (get-in @options (->> cursor (filter keyword?) vec))
          [name cursor] (prepare-input cursor form)]
      [:div.form-group.col-xs-6 {:key name}
       [:label.control-label {:for name} label]
       (into [:select.form-control {:id name
                                    :value @cursor
                                    :on-change #(reset! cursor (-> % .-target .-value))}]
             (map (fn [[label value]] [:option {:key value :value value} label]) options))])))

(defn input-typeahead [label cursor]
  (fn [[form options util]]
    (typeahead form options util label cursor 0)))


(defn input-datetimepicker [label cursor]
  (fn [[form _ util]]
    (let [r-key cursor
          time (r/cursor util (into [:time] cursor))
          [name cursor] (prepare-input cursor form)
          selected-date (.parse js/Date @cursor)
          selected-month (if (pos? (:month selected-date)) (dec (:month selected-date)) (:month selected-date))
          today (js/Date.)
          year (or (:year selected-date) (.getFullYear today))
          month (or selected-month (.getMonth today))
          day (or (:day selected-date) (.getDate today))
          [hour minute] (if @cursor (let [time (second (s/split @cursor #"T"))]
                              (s/split time #":")))
          expanded? (r/cursor util (into [:date-extended?] r-key))]
      (when (and (not @time) @cursor) (reset! time (str hour ":" minute)))
      [:div {:key r-key}
       [:div.datepicker-wrapper.col-xs-3
        [:label.control-label (first label)]
        [:div.input-group.date
        [:input.form-control.col-xs-3
         {:read-only true
          :type :text
          :on-click #(swap! expanded? not)
          :value (when-let [date (first (s/split @cursor #"T"))] date)}]
        [:span.input-group-addon
         {:on-click #(swap! expanded? not)}
         [:i.glyphicon.glyphicon-calendar]]]
        [datepicker year month day expanded? true #(deref cursor)
         #(if @cursor (swap! cursor (fn [date]
                                       (let [{:keys [day year month]} %]
                                         (s/replace date (re-pattern (first (s/split date #"T")))
                                                    (str year "-"
                                                         (if (= 1 (count (str month))) (str 0 month) year) "-"
                                                         (if (= 1 (count (str day))) (str 0 day) day))))))
              (reset! cursor (let [{:keys [day year month]} %] (.toISOString (js/Date. year day month))))) false :es-ES]]
       [:div.form-group.col-xs-3
        [:label.control-label (second label)]
        [:input.form-control {:type :text
                              :value @time
                              :on-change #(let [value (-> % .-target .-value)]
                                            (when (re-matches #"^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$"
                                                              value)
                                              (swap! cursor (fn [date]
                                                              (s/replace date (re-pattern (str hour ":" minute))
                                                                         value))))
                                              (reset! time value))}]]])))


(defn input-textarea [label cursor]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group.col-xs-12 {:key name}
       [:label.control-label {:for name} label]
       [:textarea.form-control {:type type
                             :id name
                             :value @cursor
                             :on-change #(reset! cursor (-> % .-target .-value))
                                }]])))


(defn input-entity [label cursor edit create]
  (fn [[form options util]]
    (let [id "lead-client_id"
          plus [:span.input-group-addon {:key :add
                                         :on-click #(u/show-modal (str id "-create"))}
                [:i.glyphicon.glyphicon-plus]]
          pencil [:span.input-group-addon {:key :edit
                                           :on-click #(u/show-modal (str id "-edit"))}
                  [:i.glyphicon.glyphicon-pencil]]]
      [:div {:key id} (typeahead form options util label cursor 3 plus pencil)
       [edit]
       [create]
       ])))

(defn input-markdown
  "textarea with markdown->html preview."
  [label path]
  (fn [[form _ util]]
    (let [preview (r/cursor util (into [:preview] path))
          [name cursor] (prepare-input path form)]
      (when (and @cursor (not @preview)) (reset! preview (md->html @cursor)))
      [:div.col-xs-12 {:key name}
       [:div.form-group.col-xs-6
         [:label.control-label {:for name} label]
         [:textarea.form-control
          {:type type
           :id name
           :value @cursor
           :on-change #(do (reset! cursor (-> % .-target .-value))
                           (reset! preview (md->html (-> % .-target .-value))))
           }]]
       [:div.col-xs-6
        [:label.control-label "Preview"]
        [:div.preview {:style {:border "solid grey 1px"
                               :padding :5px}
                       :dangerouslySetInnerHTML {:__html @preview}}]]]

       ))
  )


(defn btn-new-fieldset [cursor label]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:button.btn.btn-secondary
       {:key (str "add-" label)
        :on-click #(swap! cursor conj {})}
       label])))

(defn btn-remove-fieldset [cursor index label]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      (list
       [:span.clearfix {:key :remove-cf}]
       [:button.btn.btn-secondary
        {:key (str label index)
         :on-click #(swap! cursor drop-nth index)}
        label]))))

(def input-text (partial input :text))

(def input-password (partial input :password))

(def input-email (partial input :email))

(def input-number (partial input :number))

(def input-types
  {:text input-text
   :email input-email
   :password input-password
   :number input-number
   :dropdown input-dropdown
   :textarea input-textarea
   :typeahead input-textarea
   :date-time input-datetimepicker
   :checkbox input-checkbox
   :markdown input-markdown
   :entity input-entity})



(defn fieldset-fn [form-data [legend & fields] path hidden?]
  (let [[data _ util] form-data
        new-path (conj path (keyword legend))
        hidden (r/cursor util (conj new-path :hidden))
        fieldset-list (group-by first (filter (fn [fld] (some #(and (and (vector? fld) (vector? %))
                                                     (= (first fld) (first %))) fields))
                                              fields))]
    (when (nil? @hidden) (reset! hidden hidden?))
    (list
     [:span.clearfix {:key (str legend "-cf")}]
     [:fieldset {:key legend}
      [:legend legend (if @hidden
                        [:button.btn.btn-default {:on-click #(do (swap! hidden not) nil)} "show"]
                        [:button.btn.btn-default {:on-click #(do (swap! hidden not) nil)} "hide"])]
      (if-not @hidden
        [:div
         (doall (for [field fields
                      :let [hidden? (if (and (sequential? field)
                                             (= field (last (get fieldset-list (first field))))) false true)]]
                  (if (sequential? field)
                    (if (< 1 (count (get fieldset-list (first field))))
                      (fieldset-fn form-data field
                                   (conj new-path (.indexOf
                                                   (to-array (get fieldset-list (first field)))
                                                   field)) false) ;;hidden?
                      (fieldset-fn form-data field new-path false))
                    (field form-data))))])])))


(defn form [legend form-data & fieldsets]
  [:div.form-horizontal
   [:legend legend]
   [:div
    (doall (for [fieldset fieldsets]
             (fieldset-fn form-data fieldset [:visiblity] false)))]])



(defmulti render-form
  (fn [[key schema] data path]
;    (prn key ":" schema)
    (:render-type schema)))

(defmethod render-form :collection [[key schema] data path]
  (let [{label :label content :field-definitions} schema
        label (if label label (name key))
        collection {:render-type :entity
                    :label (:entity-label schema)
                    :field-definitions content}]
    (into [label]
          (conj (vec (for [i (range (count (get-in data (conj path key))))]
                       (render-form [i collection] data (conj path key) key) ))
                (btn-new-fieldset (conj path key) (str "New " (:entity-label schema)))))))

(defmethod render-form :entity [[key schema] data path]
  (let [{label :label content :field-definitions} schema
        label (if label label (name key))
        content (map #(render-form % data (conj path key)) content)]
    (into [label]
          (if (number? key)
            (conj (vec content) (btn-remove-fieldset path key (str "Remove " label)))
            content))))

(defmethod render-form :default [[key schema] data path]
  (let [{type :render-type label :label} schema]
    ((type input-types) (if label label (name key)) (conj path key))))


(defmulti get-struct (fn [[key schema]] (:render-type schema)))

(defmethod get-struct :collection [[key schema]]
  (let [{content :field-definitions} schema]
    {key [(apply merge  (map get-struct content))]}))

(defmethod get-struct :entity [[key schema]]
  (let [{content :field-definitions} schema
        struct (map get-struct content)]
    {key (if (vector? struct) struct (apply merge struct))}))

(defmethod get-struct :default [[key schema]]
  {key nil})

(defn prepare-atom [schema atom]
  (reset! atom (apply merge (map get-struct schema)))
  atom)


(defn create-form
  "Create form from schema."
  [legend schema atoms]
  (apply form legend atoms (map #(render-form % @(first atoms) []) schema))
  )
