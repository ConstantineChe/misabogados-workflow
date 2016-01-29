(ns misabogados-workflow.layout.core
  (:require [hiccup.core :refer [html]]
            [hiccup.def :refer :all]
            [hiccup.page :as hp]
            [misabogados-workflow.layout.elements :as el]))


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
   [:title title]
   (hp/include-css "/css/main.css")]
  [:body
   [:div.main
    content]])
