(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :refer [render]]
            [ring.util.response :refer [redirect response]]
            [monger.collection :as mc]
            [misabogados-workflow.db.core :as db]
            [clojure.pprint :refer [pprint]]))

(defn home-page [request]
  (render "app.html" {:forms-css (-> "reagent-forms.css" io/resource slurp)}))
  ;; (layout/blank-page "home" [:div.container [:div "hi"
                               ;; (map (fn [item] [:div.row [:h4 (key item)]
                                               ;; [:p (val item)]]) request)]]))

(defn create-lead-from-contact [{:keys [params]}]
  (mc/insert @db/db "leads" (select-keys params [:client_name :client_phone :client_email :lead_type_code :problem]))
  (redirect "/"))

(defn save-document [doc]
  (pprint doc)
  {:status "ok"})

(defroutes home-routes
  (GET "/" [] home-page)
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/contact" [] (render "contact.html"))
  (POST "/contact" [] create-lead-from-contact))
