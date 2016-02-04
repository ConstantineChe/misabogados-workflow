(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.model :refer [datas ->Lead map->User map->BasicInfo]]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect]]
            [camel-snake-kebab.core :refer :all]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [misabogados-workflow.db.core :as db]
            [misabogados-workflow.flow :refer [steps dataset]])
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]
           ))


(defn get-step [action]
  ((keyword action) steps))

(defn home-page []
  (layout/blank-page "home" [:div "hi"]))

(defn update-lead
  [id {:keys [params]}]
  (db/update-lead id  (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])))
  (redirect "/leads")
  )

(defn do-action [id action {:keys [params]}]
  (let [lead (assoc  (:lead (select-keys (transform-keys ->snake_case_keyword params) [:lead])) :step action)]
    (db/update-lead id #spy/p lead))
  (redirect (str "/lead/" id "/action/" action))
   )

(defn create-lead [{:keys [params]}]

  (db/create-lead (:lead (transform-keys ->snake_case_keyword params)))
  (redirect "/leads"))

(defn edit-lead [id]
  (let [lead (db/get-lead id)]
    (layout/blank-page "Form"
                       (layout/render-form "Form" ["PUT" (str "/lead/" id)]
                                           (list (.create-form (get-step "create")  {:lead (db/get-lead id)})
                                                 [:button.btn "Save"])))))




(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/lead/:id/edit" {{id :id} :params} (edit-lead id))
  (GET "/leads/create" []
       (layout/render-form
        "Form" ["POST" "/leads"]
        (list (.render datas "lead") [:button.btn "Save"])))
  (PUT "/lead/:id" [id :as request] (update-lead id request))
  (GET "/lead/:id/action/:action" {{id :id action :action} :params} (if (contains? steps (keyword action))
                                                                      (layout/render-form action ["PUT" (str "/lead/" id)]
                                                                                          (.create-form (get-step action)
                                                                                                        {:lead (db/get-lead id)}))))
  (PUT "/lead/:id/action/:action" [id action :as request] (if (contains? steps (keyword action)) (do-action id action request)))
  (POST "/leads" [] create-lead)
  (GET "/leads" [] "Index"))
