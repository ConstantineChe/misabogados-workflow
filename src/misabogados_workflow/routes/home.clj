(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.layout :refer [render]]
            [ring.util.response :refer [redirect response]]
            [monger.collection :as mc]
            [misabogados-workflow.db.core :as db]
            [misabogados-workflow.util :as u]
            [clojure.pprint :refer [pprint]]
            [misabogados-workflow.layout :refer [render]]
            [misabogados-workflow.email :as email]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.schema :as s]
            ))

(defn home-page [request]
  (render "home_page.html" (merge {:title "Recomendamos Abogados Confiables"
                                   :logged-in? (authenticated? request)
                                   :user (db/get-user (:identity request))}
                                  (if-let [messages (-> request :flash :messages)]
                                        {:messages messages}))))

(defn app-page [request]
  (render "app.html" {:forms-css (-> "reagent-forms.css" io/resource slurp)}))

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
        (redirect "/exito-contratar"))
      (render "contact.html" (assoc params :messages {:errors {:g-recaptcha-response "Captcha es requerido"}}) ))))

(defn save-document [doc]
  (pprint doc)
  {:status "ok"})

(defn show-category [slug]
  (let [category (mc/find-one-as-map @db/db "categories" {:slug slug})]
    (render "category.html" {:title (:name category)
                             :category category})))

(defn show-lawyer [slug]
  (let [lawyer (mc/find-one-as-map @db/db "lawyers" {:slug slug})]
    (render "lawyers_profile.html" {:title  (:name lawyer)
                                    :lawyer lawyer})))

(defroutes home-routes
  (GET "/" [] home-page)
  (GET "/garantia" [] (render "guarantee.html" {:title "Garantía"}))
  (GET "/terminos-y-condiciones" [] (render "terms_and_conditions.html" {:title "Términos y Condiciones"}))
  (GET "/politica-de-privacidad" [] (render "privacy.html" {:title "Politica de privacidad"}))
  (GET "/categoria/:slug" [slug :as request] (show-category slug))
  (GET "/app" [] app-page)
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/contact" [] (render "contact.html" {:title "Contactar"}))
  (POST "/contact" [] create-lead-from-contact)
  (GET "/exito-contratar" [] (render "contact-success.html" {:title "¡Has enviado tu mensaje!"}))
  (GET "/abogado-test/alfredo-alcaino" [] (render "lawyers_profile.html" {:title "Alfredo Alcaíno"}))
  (GET "/abogado/:slug" [slug :as request] (show-lawyer slug))
  (GET "/info_para_abogados" [] (render "info_para_abogados.html" {:title "Información para los Abogados"}))
  (GET "/categories_json" [] (map (fn [i] {:id (:slug i) :name (:name i)}) (mc/find-maps @db/db "categories"))))
