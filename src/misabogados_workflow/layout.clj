(ns misabogados-workflow.layout
  (:require [selmer.parser :as parser]
            [selmer.filters :as filters]
            [markdown.core :refer [md-to-html-string]]
            [ring.util.http-response :refer [content-type ok]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [misabogados-workflow.settings :as s]
            [clj-recaptcha.client-v2 :as c]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [misabogados-workflow.db.core :as db]))

(declare ^:dynamic *identity*)
(declare ^:dynamic *app-context*)
(parser/set-resource-path!  (clojure.java.io/resource "templates"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(parser/add-tag! :recaptcha-field (fn [_ _] 
                                    ;; (c/render "6Lc92P4SAAAAAOBC3sqUSW0glBwwaueafC4zPxKj")
                                    (c/render "6Lco-wsTAAAAAKJL86ESJT8W7s4Fb2aOnrZxwJdu \"")))

(parser/add-tag! :stars (fn [[score] context]
                          (if-let [stars-string (get-in context                              
                                                        (map keyword (clojure.string/split (str score) #"\.")))]
                            (let [stars (read-string stars-string)]
                                 (str
                                  (reduce str (repeat stars "<img src=\"/img/lawyer-profile/star-gold.png\" alt=\"Estrella\" />")) 
                                  (reduce str (repeat (- 5 stars) "<img src=\"/img/lawyer-profile/star-white.png\" alt=\"Estrella\" />"))))
                            ""
                            )))
(filters/add-filter! :markdown (fn [content] [:safe (md-to-html-string content)]))

(defn render
  "renders the HTML template located relative to resources/templates"
  [template & [params]]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :page template
          :settings @s/settings
          :csrf-token *anti-forgery-token*
          :servlet-context *app-context*
          :categories-persons (mc/find-maps @db/db "categories" {:persons true :faq_items {$exists true}} [:name :slug])
          :categories-enterprises (mc/find-maps @db/db "categories" {:enterprises true :faq_items {$exists true}} [:name :slug]))))
    "text/html; charset=utf-8"))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "error.html" error-details)})
