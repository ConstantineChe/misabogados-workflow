(ns misabogados-workflow.layout.elements
  (:require [hiccup.def :refer [defelem]]
            [hiccup.form :as form]
            [hiccup.element :as el]))

(defelem submit-button [text]
  [:div.control-group
     [:div.controls [:button.btn.btn-lg.btn-primary text]]])

(defelem input [label field value comment password?]
  (list [:label.control-label {:for field} label]
   [:div.control-group
    [:div.controls (if-not (false? comment) [:p.help-block comment])
     (let [type (if password? form/password-field form/text-field)]
       (type {:class "input-xlarge required"
              :placeholder " "}
                                 field value))]]))

(defelem input-text [label field value & comment?]
  (if-let [comment (first comment?)]
    (input label field value comment false)
    (input label field value false false)))

(defelem input-password [label field value & comment?]
  (if-let [comment (first comment?)]
    (input label field value comment true)
    (input label field value false true)))

(defelem form [target legend & fields]
  (form/form-to {:class "form-horisontal"} target
                               [:fieldset [:div#legend [:legend legend]]
                                fields]))
