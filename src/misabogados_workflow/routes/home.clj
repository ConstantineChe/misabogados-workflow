(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.model :refer [->Lead map->User map->BasicInfo]]
            [misabogados-workflow.layout.core :as layout]
            [misabogados-workflow.layout :refer [render]]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect]]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [misabogados-workflow.db.core :as db]
            [clojure.pprint :refer [pprint]]
            [misabogados-workflow.flow :refer [get-rendered-form dataset PManual PAutomatic]]
            [misabogados-workflow.flow-definition :refer [steps]])
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]
           ))


(defn get-step [action]
  ((keyword action) steps))

(defn home-page [request]
  (render "home.html"))
  ;; (layout/blank-page "home" [:div.container [:div "hi"
                               ;; (map (fn [item] [:div.row [:h4 (key item)]
                                               ;; [:p (val item)]]) request)]]))

(defn update-lead
  [id {:keys [params]}]
  (db/update-lead id  (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])))
  (redirect "/leads")
  )

(defn do-action [id action {:keys [params]}]
  (let [lead (assoc  (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])) :step action)
        step ((keyword action) steps)]
    (db/update-lead id lead)
    (cond (satisfies? PManual step)
          (redirect "/leads")
          (satisfies? PAutomatic step)
          (do
            (.do-action step lead)
            (db/update-lead id {:step (:action step)})
            (redirect "/leads"))
          )))

(defn create-lead [{:keys [params]}]
  (db/create-lead (assoc  (:lead (transform-keys ->snake_case_keyword params)) :step :check))
  (redirect "/leads"))

(defn edit-lead [id]
  (let [lead (db/get-lead id)]
    (layout/blank-page "Form"
                       (layout/render-form "Edit lead"
                                           ["PUT" (str "/lead/" id)]
                                           (list (.create-form (get-step "archive")  {:lead lead})
                                                            [:button.btn.btn-secondary "Save"])))))

(defn new-lead [params]
  (layout/blank-page "Form"
                     (layout/render-form "New lead"
                                         ["POST" "/leads"]
                                         (list (get-rendered-form [:lead :user :basic-info]  {:lead {}})
                                               [:button.btn.btn-secondary "Save"]))))


(defn save-document [doc]
  (pprint doc)
  {:status "ok"})

(defroutes home-routes
  (GET "/" [] home-page)
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/lead/:id/edit" {{id :id} :params} (edit-lead id))
  (GET "/leads/create" [] new-lead)
  (PUT "/lead/:id" [id :as request] (update-lead id request))
  (GET "/lead/:id/action/:action" {{id :id action :action} :params}
       (if (contains? steps (keyword action))
         (layout/render-form action ["PUT" (str "/lead/" id)]
                             (.create-form (get-step action)
                                           {:lead (db/get-lead id)}))))
  (PUT "/lead/:id/action/:action" [id action :as request]
       (if (contains? steps (keyword action)) (do-action id action request)))
  (POST "/leads" [] create-lead)
  (GET "/leads" [] (layout/dashboard (db/get-leads)))
)
