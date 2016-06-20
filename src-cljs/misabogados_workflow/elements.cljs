(ns misabogados-workflow.elements
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as s]
            [misabogados-workflow.utils :as u]
            [goog.events :as gev]
            [markdown.core :refer [md->html]]
            [clojure.walk :refer [keywordize-keys]]
            [misabogados-workflow.ajax :refer [GET POST PUT csrf-token]]
            [reagent-forms.datepicker
             :refer [parse-format format-date datepicker]])
  (:import goog.net.IframeIo
           goog.net.EventType
           [goog.events EventType]))

(declare create-form)

(def file (r/atom nil))

(defn gen-name [cursor] (->> cursor (into []) (map #(if (keyword? %) (name %) %)) (interpose "-") (apply str)))

(defn drop-nth [coll n]
   (keep-indexed #(if (not= %1 n) %2) coll))

(defn prepare-input [cursor form]
  ((juxt gen-name #(r/cursor form (into [] %))) cursor))


(defn upload-file! [upload-form-id status name url]
  (reset! status [:p "Uploading..."])
  (let [io (IframeIo.)]
    (gev/listen io goog.net.EventType.SUCCESS
                #(do
                   (reset! name {:tmp-filename (.-filename (.getResponseJson io))})
                   (reset! status [:p "File uploaded successfully"])))
    (gev/listen io goog.net.EventType.ERROR
                #(reset! status [:p "Error uploading"]))
    (.setErrorChecker io #(= "error" (.getResponseText io)))
    (.sendFromForm io
                   (.getElementById js/document upload-form-id)
                   url)))

(defn typeahead [form options util label cursor readonly min-chars & addons]
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
                :read-only readonly
                :key :input
                :value @text
                :on-click #(.setSelectionRange (.-target %) 0 (count @text))
                :on-focus #(if-not readonly (js/setTimeout (fn [_] (reset! dropdown-class "open")) 100))
                :on-blur #(js/setTimeout (fn [_] (do (reset! dropdown-class "")
                                             (reset! text (match @cursor)))) 500)
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
       (if (and addons (not readonly)) [:div.input-group input addons]
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

(defn create-entity-modal
  "Create entity form modal"
  [title id schema data options opt util cursor cursor-label create-fn]
  (r/create-class
   {:render (fn []
              [:div.modal.fade {:role :dialog :id id}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  (create-form title schema [data opt util])]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]
                  [:button.btn.btn-primary {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Create"
                                            :on-click #(create-fn @data cursor options cursor-label)} "Create"]]]]])
    :component-did-mount (fn [this] (let [modal (-> this r/dom-node js/jQuery)]
                                     (.attr modal "tabindex" "-1")
                                     (.on  modal "hide.bs.modal"
                                           #(js/setTimeout (fn [] (reset! data nil)) 100))))}))

(defn edit-entity-modal
  "Edit entity form modal"
  [title id schema data options opt util entity-id edit-fn root-key get-entity-fn]
  (r/create-class
   {:render (fn []
              (when (and entity-id (not= entity-id (-> @data root-key :_id))) (get-entity-fn entity-id data))
              [:div.modal.fade {:role :dialog :id id}
               [:div.modal-dialog.modal-lg
                [:div.modal-content
                 [:div.modal-header
                  [:button.close {:type :button :data-dismiss :modal :aria-label "Close"}
                   [:span {:aria-hidden true :dangerouslySetInnerHTML {:__html "&times;"}}]]]
                 [:div.modal-body
                  (create-form title schema [data opt util])]
                 [:div.modal-footer
                  [:button.btn.btn-default {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Close"} "Close"]
                  [:button.btn.btn-primary {:type :button
                                            :data-dismiss :modal
                                            :aria-label "Update"
                                            :on-click #(edit-fn @data options entity-id)} "Update"]]]]])
    :component-did-mount (fn [this] (let [modal (-> this r/dom-node js/jQuery)]
                                     (.attr modal "tabindex" "-1")
                                     (.on  modal "hide.bs.modal"
                                           #(js/setTimeout (fn [] (reset! data nil)) 100))))}))

(defn get-entity [url root-key]
  (fn [id data]
    (if id (GET (str js/context url "/" id) {:handler #(reset! data {root-key (keywordize-keys %)})}) data)))

(defn create-entity [url root-key label-fn]
  (fn [data cursor options cursor-label]
    (POST (str js/context url) {:params {:data (root-key data)}
                                :handler #(let [entity (keywordize-keys %)
                                                label (label-fn entity)]
                                            (reset! cursor-label label)
                                            (swap! options conj
                                                   [label (:_id entity)])
                                            (reset! cursor (:_id (keywordize-keys %))))
                                :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           404 (js/alert "Client not found")
                                                           (js/alert (str %)))})))

(defn edit-entity [url root-key label-fn]
  (fn [data options id]
    (PUT (str js/context url) {:params {:id id
                                        :data (dissoc (root-key data) :_id)}
                               :handler #(let [entity (keywordize-keys %)]
                                           (swap! options
                                                  (fn [x]
                                                    (for [[label id] x]
                                                      (if (= id (:_id entity))
                                                        [(label-fn entity) id]
                                                        [label id])))))
                               :error-handler #(case (:status %)
                                                 403 (js/alert "Access denied")
                                                 500 (js/alert "Internal server error")
                                                 404 (js/alert "Client not found")
                                                 (js/alert (str %)))})))

(defn input [type label cursor & attrs]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)
          {:keys [readonly div-class]} (first attrs)]
      [:div {:key name :class (str "form-group " (or div-class "col-xs-6"))}
       [:label.control-label {:for name} label]
       [:input.form-control {:type type
                             :read-only readonly
                             :id name
                             :value @cursor
                             :on-change #(reset! cursor (-> % .-target .-value))
                             }]])))

(defn input-checkbox [label cursor & attrs]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)
          {:keys [readonly div-class]} (first attrs)]
      [:div {:key name :class (str "form-group " (or div-class "col-xs-6"))}
       [:label.control-label {:for name} label]
       [:input.form-control (merge {:type :checkbox
                                    :read-only readonly
                                    :on-change #(swap! cursor not)
                                    :id name}
                                   (if @cursor {:checked :true}))]])))

(defn input-dropdown [label cursor & attrs]
  (fn [[form options]]
    (let [options (get-in @options (->> cursor (filter keyword?) vec))
          [name cursor] (prepare-input cursor form)
          {:keys [readonly div-class]} (first attrs)]
      (when (nil? @cursor) (reset! cursor (second (first options))))
      (if readonly
        [:div.form-group.col-xs-6 {:key name}
         [:label.control-label {:for name} label]
         [:input.form-control {:id name
                               :type :text
                               :read-only true
                               :value (some (fn [[l v]] (when (= v @cursor) l)) options)}]]
        [:div.form-group.col-xs-6 {:key name}
         [:label.control-label {:for name} label]
         (into [:select.form-control {:id name
                                      :value @cursor
                                      :read-only readonly
                                      :on-change #(if-not readonly (reset! cursor (-> % .-target .-value)))}]
               (map (fn [[label value]] [:option {:key value :value value} label]) options))]))))

(defn input-typeahead [label cursor & attrs]
  (let [{:keys [readonly div-class]} (first attrs)]
    (fn [[form options util]]
            (typeahead form options util label cursor readonly 0))))

(defn input-datepicker [label path & attrs]
  (fn [[form _ util]]
    (let [[name cursor] (prepare-input path form)
          {:keys [readonly div-class]} attrs
          text (r/cursor util (into [:t-date] path))
          to-date-string (fn [date]
                           (let [date (js/Date. date)]
                             (str (.getFullYear date) "-"
                                  (if (> 10  (inc (.getMonth date))) (str 0 (inc (.getMonth date)))
                                      (inc (.getMonth date))) "-"
                                      (if (> 10 (.getDate date)) (str 0 (.getDate date)) (.getDate date)))))]
      [:div.form-group.col-xs-6 {:key name}
       [:label.control-label label]
       [:input.form-control {:id name
                             :type :date
                             :readonly readonly
                             :value (let [date (if @cursor @cursor
                                                   (if (and @text(re-matches #"^\d{4}-(0[0-9])|(1[0-2])-([0-2][0-9])|(3[0-1])$"
                                                                             @text))
                                                     @text))]
                                      (if date (to-date-string date)))
                             :on-change #(let [val (-> % .-target .-value)]
                                           (if (re-matches #"^\d{4}-[0-1][0-9]-[0-3][0-9]$" val)
                                             (reset! cursor (.toISOString (new js/Date val)))
                                             (reset! text val)))}]])))


(defn input-datetimepicker [label cursor & attrs]
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
          expanded? (r/cursor util (into [:date-extended?] r-key))
          {:keys [readonly div-class]} (first attrs)]
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
        (when-not readonly
          [datepicker year month day expanded? true #(deref cursor)
                      #(if @cursor (swap! cursor (fn [date]
                                                   (let [{:keys [day year month]} %]
                                                     (s/replace date (re-pattern (first (s/split date #"T")))
                                                                (str year "-"
                                                                     (if (= 1 (count (str month))) (str 0 month) year) "-"
                                                                     (if (= 1 (count (str day))) (str 0 day) day))))))
                           (reset! cursor (let [{:keys [day year month]} %] (.toISOString (js/Date. year (dec month) day))))) false :es-ES])]
       [:div.form-group.col-xs-3
        [:label.control-label (second label)]
        [:input.form-control {:type :text
                              :read-only readonly
                              :value @time
                              :on-change #(let [value (-> % .-target .-value)]
                                            (when (re-matches #"^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$"
                                                              value)
                                              (swap! cursor (fn [date]
                                                              (s/replace date (re-pattern (str hour ":" minute))
                                                                         value))))
                                              (reset! time value))}]]])))


(defn input-textarea [label cursor & attrs]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)
          {:keys [readonly div-class]} (first attrs)]
      [:div.form-group.col-xs-12 {:key name}
       [:label.control-label {:for name} label]
       [:textarea.form-control {:type type
                                :id name
                                :read-only readonly
                                :value @cursor
                                :on-change #(reset! cursor (-> % .-target .-value))
                                }]])))


(defn input-entity [label path & attrs]
  (fn [[form options util]]
    (let [[id cursor] (prepare-input path form)
          plus [:span.input-group-addon {:key :add
                                         :on-click #(u/show-modal (str id "-create"))}
                [:i.glyphicon.glyphicon-plus]]
          pencil [:span.input-group-addon {:key :edit
                                           :on-click #(u/show-modal (str id "-edit"))}
                  [:i.glyphicon.glyphicon-pencil]]
          {:keys [readonly edit-legend create-legend schema url label-fn]} (first attrs)
          selected-entity (r/cursor util (into [:entity] path))
          entity-options (r/cursor options path)
          entity-id @cursor
          entity-label (r/cursor util (into [:typeahead-t] path))
          root-key-create (keyword (str "new-" (name (first (keys schema)))))
          root-key-edit (keyword (str "edit-" (name (first (keys schema)))))
          create-schema {root-key-create ((first (keys schema)) schema)}
          edit-schema {root-key-edit ((first (keys schema)) schema)}
          get-entity-fn (get-entity url root-key-edit)
          create-fn (create-entity url root-key-create label-fn)
          edit-fn (edit-entity url root-key-edit label-fn)
          opts (r/atom {})
          utl (r/atom {})]
      [:div {:key id} (typeahead form options util label path readonly 3 plus pencil)
       [(edit-entity-modal edit-legend (str id "-edit") edit-schema selected-entity
                           entity-options opts utl entity-id edit-fn root-key-edit get-entity-fn)]
       [(create-entity-modal edit-legend (str id "-create") create-schema selected-entity
                             entity-options opts utl cursor entity-label create-fn) ]
       ])))


(defn input-markdown
  "textarea with markdown->html preview."
  [label path & attrs]
  (fn [[form _ util]]
    (let [preview (r/cursor util (into [:preview] path))
          [name cursor] (prepare-input path form)
          {:keys [readonly div-class]} (first attrs)]
      (when (and @cursor (not @preview)) (reset! preview (md->html @cursor)))
      [:div.col-xs-12 {:key name}
       [:div.form-group.col-xs-6
         [:label.control-label {:for name} label]
         [:textarea.form-control
          {:type type
           :read-only readonly
           :id name
           :value @cursor
           :on-change #(do (reset! cursor (-> % .-target .-value))
                           (reset! preview (md->html (-> % .-target .-value))))
           }]]
       [:div.col-xs-6
        [:label.control-label (str label " Preview")]
        [:div.preview {:style {:border "solid grey 1px"
                               :padding :5px}
                       :dangerouslySetInnerHTML {:__html @preview}}]]])))

(defn input-image
  "Image input with preview."
  [label path upload-url]
  (fn [[form _ util]]
    (let [[name cursor] (prepare-input path form)
          status (r/cursor util (into [:file-status] path))]
      [:div.form-group.col-xs-6 {:key name}
       [:form {:id (str name "-form")
               :enc-type "multipart/form-data"
               :method "POST"}
        [:label.control-label {:for name} label]
        @status
        [:input {:type :hidden :name :__anti-forgery-token :value @csrf-token}]
        [:input {:id name :name "file" :type "file"
                 :on-change #(upload-file! (str name "-form") status cursor upload-url)}]]]))
  )


(defn data-table [data headers getters]
  [:table.table.table-hover.table-striped.panel-body
   [:thead
    (for [header headers]
      [:th {:key (.indexOf (to-array headers) header)} header])]
   [:tbody
    (for [item data]
      [:tr {:key (.indexOf (to-array data) item)}
       (for [getter getters]
         [:td {:key (.indexOf (to-array getters) getter)}
          (getter item)])])]])

(defn action-button [data attributes url return-url]
  (let [{:keys [name action]} attributes
        action-url (str url action)
        submit (fn [e] (PUT (str js/context action-url)
                           {:params @data
                            :handler #(let [response (keywordize-keys %)
                                            id (:id response)]
                                        (session/put! :notification
                                                      [:div.alert.alert-sucsess "Lead with id "
                                                       [:a {:href (str "#/lead/" id "/edit")} id]
                                                       " was updated."])
                                        (u/redirect return-url))
                            :error-handler #(case (:status %)
                                              403 (js/alert "Access denied")
                                              404 (js/alert "Lead not found")
                                              500 (js/alert "Internal server error")
                                              (js/alert (str %)))}))]
    [:button.btn.btn-primary {:on-click submit} name]))

(defn btn-new-fieldset [cursor label]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:button.btn.btn-secondary
       {:key (str "add-" label)
        :on-click #(swap! cursor (fnil conj []) {})}
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
   :typeahead input-typeahead
   :date-time input-datetimepicker
   :checkbox input-checkbox
   :markdown input-markdown
   :image input-image
   :input-entity input-entity
   :date input-datepicker})



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
                        [:span.glyphicon.glyphicon-plus {:on-click #(do (swap! hidden not) nil)}]
                        [:span.glyphicon.glyphicon-minus {:on-click #(do (swap! hidden not) nil)}])]
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
  (fn [[key schema] data path attributes]
    (:render-type schema)))

(defmethod render-form :collection [[key schema] data path attributes]
  (let [{label :label fields :field-definitions} schema
        label (if label label (name key))
        collection {:render-type :entity
                    :label (:entity-label schema)
                    :field-definitions fields}
        attributes (if (not attributes) :all attributes)
        content (vec (for [i (range (count (get-in data (conj path key))))]
                       (render-form [i collection] data (conj path key) attributes)))]
    (into [label]
          (if-not (= :readonly (:fields attributes))
            (conj content
                  (btn-new-fieldset (conj path key) (str "New " (:entity-label schema))))
            content))))

(defmethod render-form :entity [[key schema] data path attributes]
  (let [{label :label fields :field-definitions} schema
        label (if label label (name key))
        child-attributes (if-not (= :all attributes)
                           (if (keyword? key)
                             (key attributes) attributes)
                           attributes)
        schema-to-render (filter #(not (nil? %))
                                 (map #(if (not (#{:entity :collection} (:render-type (second %))))
                                         (cond (= :all (:fields child-attributes)) %
                                               (= :readonly (:fields child-attributes))
                                               (assoc-in % [1 :args] (if-let [args (get-in % [1 :args])]
                                                                       [(assoc (first args) :readonly true)]
                                                                       [{:readonly true}])))
                                         (if ((first %) child-attributes) %)) fields))
        content (map #(render-form % data (conj path key) ((first %) child-attributes))
                     (if (= :all child-attributes)
                       fields
                       schema-to-render))]
    (into [label]
          (if (and (number? key) (not= :readonly (:fields attributes)))
            (conj (vec content) (btn-remove-fieldset path key (str "Remove " label)))
            content))))

(defmethod render-form :default [[key schema] data path attributes]
  (let [{type :render-type label :label args :args} schema]
    (if args
      (apply (type input-types) (if label label (name key)) (conj path key) args)
      ((type input-types) (if label label (name key)) (conj path key)))))


(defmulti get-struct (fn [[key schema]] (:render-type schema)))

(defmethod get-struct :collection [[key schema]]
  (let [{content :field-definitions} schema]
    {key []}))

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
  ([legend schema atoms]
   (apply form legend atoms (map #(render-form % @(first atoms) [] :all) schema)))
  ([legend schema atoms attributes]
   (apply form legend atoms (map #(render-form % @(first atoms) [] attributes) schema)))
  )
