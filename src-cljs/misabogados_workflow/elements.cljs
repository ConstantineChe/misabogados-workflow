(ns misabogados-workflow.elements
  (:require [reagent.core :as r]
            [reagent.session :as session])
)


(defn gen-name [cursor] (->> cursor (into []) (map name) (interpose "-") (apply str)))

(defn prepare-input [cursor form]
  ((juxt gen-name (fn [x] (r/cursor form (into [] x)))) cursor))

(defn input [form type label cursor]
  (let [[name cursor] (prepare-input cursor form)]
    [:div.form-group {:key name}
     [:label.control-label {:for name} label]
     [:input.form-control {:type type
                           :id name
                           :value @cursor
                           :on-change #(reset! cursor (-> %  .-target .-value))
                           }]]))

(defn form-input [f & args]
  (fn [form] (apply f form args)))


(defn input-checkbox [label cursor]
  (fn [form]
    (let [[name cursor] (prepare-input cursor form)]
      [:div.form-group {:key name}
       [:label.control-label label]
       [:input.form-control (merge {:type :checkbox
                                    :on-change #(swap! cursor not)
                                    :id name}
                                   (if @cursor {:checked :true}))]])))

(defn input-dropdown [label cursor options]
  (fn [form]
    (let [[name cursor] (prepare-input cursor form)]
      [:div "noen"])))


(def input-text (partial form-input input :text))

(def input-password (partial form-input input :password))

(def input-email (partial form-input input :email))

(def input-number (partial form-input input :number))

(defn form [legend form-data & fieldsets]
  [:form.form-horizontal
   [:legend legend]
   (doall (for [[legend & fields] fieldsets]
       [:fieldset {:key legend}
        [:legend legend]
        (doall (for [field fields]
            (field form-data)))]))])
