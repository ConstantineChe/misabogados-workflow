(ns misabogados-workflow.elements
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as s])
)

(defn gen-name [cursor] (->> cursor (into []) (map #(if (keyword? %) (name %) %)) (interpose "-") (apply str)))

(defn prepare-input [cursor form]
  ((juxt gen-name #(r/cursor form (into [] %))) cursor))

(defn input [type label cursor]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group {:key name}
       [:label.control-label {:for name} label]
       [:input.form-control {:type type
                             :id name
                             :value @cursor
                             :on-change #(reset! cursor (-> % .-target .-value))
                             }]])))

(defn input-checkbox [label cursor]
  (fn [[form]]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group {:key name}
       [:label.control-label {:for name} label]
       [:input.form-control (merge {:type :checkbox
                                    :on-change #(swap! cursor not)
                                    :id name}
                                   (if @cursor {:checked :true}))]])))

(defn input-dropdown [label cursor]
  (fn [[form options]]
    (let [options (get-in @options (->> cursor (filter keyword?) vec))
          [name cursor] (prepare-input cursor form)]
      [:div.form-group {:key name}
       [:label.control-label {:for name} label]
       (into [:select.form-control {:id name
                                    :value @cursor
                                    :on-change #(reset! cursor (-> % .-target .-value))}]
             (map (fn [[label value]] [:option {:key value :value value} label]) options))])))

(defn input-typeahead [label cursor]
  (fn [[form options]]
    (let [f-opts (r/cursor options (into [:typeahead] cursor))
          text (r/cursor options (into [:typeahead-t] cursor))
          dropdown-class (r/cursor options (into [:typeahead-c] cursor))
          selected-index (r/cursor options (into [:typeahead-i] cursor))
          options (get-in @options (->> cursor (filter keyword?) vec))
          [name cursor] (prepare-input cursor form)
          match #(some (fn [[l v]] (when (= v %) l)) options)
          select (fn [[l v]] (do (reset! cursor v)
                                (reset! text l)
                                (reset! dropdown-class "")))
          list (map-indexed (fn [i [label value :as option]]
                              [:li {:key i
                                    :on-mouse-over #(reset! selected-index i)
                                    :class (if (= i @selected-index) "active" "")
                                    :on-click #(select option)
                                    :on-focus #((do (print value)
                                                    (reset! cursor value)))} [:a label]])
                   (if @f-opts @f-opts options))]
      (if (nil? @text) (reset! text (match @cursor)))
      (if (nil? @selected-index) (reset! selected-index -1))
      [:div.form-group {:id (str name "-g") :key name}
       [:label.control-label {:for name} label]
       [:input.form-control
        {:type :text
         :value @text
         :on-click #(.setSelectionRange (.-target %) 0 (count @text))
         :on-focus #(js/setTimeout (fn [_] (reset! dropdown-class "open")) 100)
         :on-blur #(js/setTimeout (fn [_] (do (reset! dropdown-class "")
                                             (reset! text (match @cursor)))) 100)
         :on-change #(do (reset! text (-> % .-target .-value))
                         (reset! f-opts (filter
                                         (fn [[l]] (re-find (re-pattern (s/lower-case @text)) (s/lower-case l)))
                                         options)))
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
                           9  (do (select (get (if @f-opts @f-opts options) @selected-index)) (-> % .-target .blur))
                           13 (do (select (get (if @f-opts @f-opts options) @selected-index)) (-> % .-target .blur))
                           27 (do (reset! dropdown-class ""))
                           "default"))}]
       [:div.dropdown {:class @dropdown-class}
        (into [:ul.dropdown-menu
               {:id name
                :role :menu
                :style {:height :auto :max-height :200px :overflow-x :hidden}}]
              list
              )]])))


(def input-text (partial input :text))

(def input-password (partial input :password))

(def input-email (partial input :email))

(def input-number (partial input :number))

(def input-texarea (partial input :textarea))

(defn fieldset-fn [form-data [legend & fields]]
  [:fieldset {:key legend}
        [:legend legend]
   (doall (for [field fields]
            (if (sequential? field)
              (fieldset-fn form-data field)
              (field form-data))))])

(defn form [legend form-data & fieldsets]
  [:div.form-horizontal
   [:legend legend]
   (doall (for [fieldset fieldsets]
       (fieldset-fn form-data fieldset)))])
