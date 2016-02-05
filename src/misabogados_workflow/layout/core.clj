(ns misabogados-workflow.layout.core
  (:require [hiccup.core :refer [html]]
            [hiccup.def :refer :all]
            [hiccup.page :as hp]
            [hiccup.element :as hel]
            [misabogados-workflow.layout.elements :as el]
            [misabogados-workflow.util :as util]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))


(defmacro defpage [name title args & fbody]
  `(defn ~name
     ~args
     (blank-page ~title ~@fbody)))

(defn include-bootstrap []
  (list (hp/include-js "https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js")
        (hp/include-js "/js/jquery.bootstrap.wizard.min.js")
        (hp/include-js "/js/main.js")

        [:link {:rel "stylesheet"
                :integrity
                "sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7"
                :crossorigin "anonymous"
                :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"}]
        [:script {:integrity
                  "sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS"
                  :crossorigin "anonymous"
                  :src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"}]))

(defhtml blank-page [title & content]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   (include-bootstrap)
   [:link {:href "https://fonts.googleapis.com/css?family=Open+Sans:400,700,300"
           :rel "stylesheet"
           :type "text/css"}]
   [:title title]
   (hp/include-css "/css/main.css")]
  [:body
   [:div.main
    content]])


(defn render-form [title target fields]
  (blank-page title
              [:div.col-md-8.col-md-offset-2 (el/form target title (list (anti-forgery-field) fields))
               ]))

(defpage dashboard "Dashboard" [data]
  [:div.container
   [:h1 "Dashboard"]
   [:hr]
   (hel/link-to {:class "btn btn-primary btn-lg"} "/leads/create" "Create new lead")
   [:hr]
   [:table.table.table-hover.table-striped
    [:caption "Leads"]
    [:thead
     [:tr
      [:th "Id"]
      [:th "User name"]
      [:th "Pending Action"]
      [:th ""]]]
    (for [lead data
          :let [id (:_id lead)
                name (get-in lead [:user :name])
                pending-action (util/remove-kebab (:step lead))
                actions (hel/link-to {:class "btn btn-success btn-sm"} (str "/lead/" id "/action/"(:step lead)) "Start") ]]
      [:tr
       [:td id]
       [:td name]
       [:td pending-action]
       [:td.text-right actions]])]])
