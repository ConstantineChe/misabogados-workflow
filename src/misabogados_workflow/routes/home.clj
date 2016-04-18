(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :refer [render]]
            [ring.util.response :refer [redirect response]]
            [monger.collection :as mc]
            [misabogados-workflow.db.core :as db]
            [clojure.pprint :refer [pprint]]
            [misabogados-workflow.layout :refer [render]]
            [misabogados-workflow.email :as email]
            ))

(defn home-page [request]
  (render "app.html" {:forms-css (-> "reagent-forms.css" io/resource slurp)}))
  ;; (layout/blank-page "home" [:div.container [:div "hi"
                               ;; (map (fn [item] [:div.row [:h4 (key item)]
                                               ;; [:p (val item)]]) request)]]))

(defn create-lead-from-contact [{:keys [params]}]
  (let [recaptcha-response (u/check-recaptcha params)]
    (if (:valid? recaptcha-response)
      (let [
            client-fields (clojure.set/rename-keys (select-keys params [:client_name :client_phone :client_email])
                                                   {:client_name :name :client_phone :phone :client_email :email})
            client (mc/insert-and-return @db/db "clients" client-fields)
            lead-fields (into {:client_id (:_id client) :lead_source_code "page"} (select-keys params [:lead_type_code :problem]))
            ]
        (mc/insert @db/db "leads" lead-fields)
        (future (email/contact-email params))
        (redirect "/"))
      (render "contact.html" (assoc params :messages {:errors {:g-recaptcha-response "Captcha es requerido"}}) ))))

(defn save-document [doc]
  (pprint doc)
  {:status "ok"})

(defn show-category [slug]
  (let [category (mc/find-one-as-map @db/db "categories" {:slug slug})]
    (render "category.html" {:title (:name category)
                             :category category})))

(defroutes home-routes
  (GET "/home" [] (render "home_page.html" {:title "Home"}))
  (GET "/garantia" [] (render "guarantee.html" {:title "Garantia"}))
  (GET "/terminos-y-condiciones" [] (render "terms_and_conditions.html" {:title "Garantia"}))
  (GET "/categorias/:slug" [slug :as request] (show-category slug))
  (GET "/" [] home-page)
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/contact" [] (render "contact.html"))
  (POST "/contact" [] create-lead-from-contact))
