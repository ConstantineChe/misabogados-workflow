(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET PUT POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.model :refer [datas ->Lead map->User map->BasicInfo]]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]
            [ring.util.response :refer [redirect]]
            [misabogados-workflow.db.core :as db])
  (:import [misabogados-workflow.model.Lead]
           [misabogados-workflow.model.User]
           [misabogados-workflow.model.BasicInfo]
           ))


(defn home-page []
  (layout/blank-page "home" [:div "hi"]))

(defn update-lead [id {:keys [params]}]
  (db/update-lead id (select-keys params [:etc :name :status])))

(defn create-lead [{:keys [params]}]

  (db/create-lead (:lead params))
  ;; (db/create-lead params)
  (redirect "/leads"))

(defn edit-lead [id]
  (let [lead (db/get-lead id)]
    (layout/blank-page "Form" 
                       (layout/render-form "Form" ["PUT" (str "/lead/" id)] 
                                           (list (.traverse (->Lead (map->User (:user lead)) 
                                                                    (map->BasicInfo (:basic-info lead))))
                                                 [:button.btn "Save"])))))


(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/lead/:id/edit" {{id :id} :params} (edit-lead id))
  (GET "/leads/create" [] 
       (layout/blank-page "Form" (layout/render-form "Form" ["POST" "/leads"] (list (.traverse datas (fn [name] (str "[lead]" name))) [:button.btn "Save"]))))
  (PUT "/lead/:id" [id] update-lead)
  (POST "/leads" [] create-lead)
  (GET "/leads" [] "Index"))
