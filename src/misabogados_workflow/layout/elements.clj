(ns misabogados-workflow.layout.elements
  (:require [hiccup.def :refer [defelem]]
            [hiccup.form :as form]
            [hiccup.element :as el]
            [clojure.string :as str]))

(defelem submit-button [text]
  [:div.control-group
     [:div.controls [:button.btn.btn-lg.btn-primary text]]])

(defelem input [label field value comment password?]
  [:div.form-group
   [:label {:for field} label]
   (if-not (false? comment) [:p.help-block comment])
   (let [type (if password? form/password-field form/text-field)]
     (type {:class "form-control"
            :placeholder (str "Enter " (str/lower-case label))}
           field value))])

(defelem input-text [label field value & comment?]
  (if-let [comment (first comment?)]
    (input label field value comment false)
    (input label field value false false)))

(defelem input-password [label field value & comment?]
  (if-let [comment (first comment?)]
    (input label field value comment true)
    (input label field value false true)))

(defelem form [target legend & fields]
  (list [:h1 legend]
        (form/form-to {:class "form-horisontal"} target
                      fields)))
