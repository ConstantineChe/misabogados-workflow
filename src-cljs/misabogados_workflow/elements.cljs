(ns misabogados-workflow.elements
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as s])
)

(defn gen-name [cursor] (->> cursor (into []) (map name) (interpose "-") (apply str)))

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
    (let [options (get-in @options cursor)
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
          options (get-in @options cursor)
          [name cursor] (prepare-input cursor form)
          match #(some (fn [[l v]] (when (= v %) l)) options)]
      (if (nil? @text) (reset! text (match @cursor)))
      [:div.form-group {:key name}
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
                                         options)))}]
       [:div.dropdown {:class @dropdown-class}
        (into [:ul.dropdown-menu
               {:id name
                :style {:height :auto :max-height :200px :overflow-x :hidden}}]
              (map (fn [[label value]] [:li {:key value
                                            :on-click #(do (reset! cursor value)
                                                           (reset! text label))} label])
                   (if @f-opts @f-opts options)))]])))


(def input-text (partial input :text))

(def input-password (partial input :password))

(def input-email (partial input :email))

(def input-number (partial input :number))

(defn form [legend form-data & fieldsets]
  [:div.form-horizontal
   [:legend legend]
   (doall (for [[legend & fields] fieldsets]
       [:fieldset {:key legend}
        [:legend legend]
        (doall (for [field fields]
            (field form-data)))]))])
