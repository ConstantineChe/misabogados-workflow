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
            [misabogados-workflow.settings :as settings]
            [buddy.auth :refer [authenticated?]]
            [misabogados-workflow.schema :as s]
            [markdown.core :refer [md-to-html-string]]
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
  (let [category (mc/find-one-as-map @db/db "categories" {:slug slug})
        category (assoc category :intro [:safe (md-to-html-string (:intro category))]
                        :pricing [:safe (md-to-html-string (:pricing category))])
        category (assoc category :faq_items (map (fn [faq-item]
                                                   {:id (if-let [id (:id faq-item)] id (.indexOf (:faq_items category) faq-item))
                                                    :name (:name faq-item)
                                                   :text (md-to-html-string (:text faq-item))})
                                                 (:faq_items category)))]
    (render "category.html" {:title (:name category)
                             :category category})))

(defn show-lawyer [slug]
  (let [lawyer (mc/find-one-as-map @db/db "lawyers" {:slug slug})]
    (render "lawyers_profile.html" {:title  (:name lawyer)
                                    :lawyer (assoc lawyer :join_date (try (.getYear (:join_date lawyer))
                                                                          (catch Exception e "")))})))

(defroutes home-routes
  (GET "/" [] home-page)
  (GET "/garantia" [] (render (str "guarantee_" (settings/fetch :country) ".html") {:title "Garantía"}))
  (GET "/terminos-y-condiciones" [] (render (str "terms_and_conditions_" (settings/fetch :country) ".html")  {:title "Términos y Condiciones"}))
  (GET "/politica-de-privacidad" [] (render (str "privacy_" (settings/fetch :country) ".html")  {:title "Politica de privacidad"}))
  (GET "/categoria/:slug" [slug :as request] (show-category slug))
  (GET "/app" [] app-page)
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/contact" [] (render "contact.html" {:title "Contactar"}))
  (POST "/contact" [] create-lead-from-contact)
  (GET "/exito-contratar" [] (render "contact-success.html" {:title "¡Has enviado tu mensaje!"}))
  (GET "/abogado/:slug" [slug :as request] (show-lawyer slug))
  (GET "/info_para_abogados" [] (render "info_para_abogados.html" {:title "Información para los Abogados"}))
  (GET "/como_funciona" [] (render "como_funciona.html" {:title "¿Cómo funciona?"}))
  (GET "/precios" [] (render "precios.html" {:title "Precios"}))
  (GET "/categories_json" [] (map (fn [i] {:id (:slug i) :name (:name i)}) (mc/find-maps @db/db "categories"))))
