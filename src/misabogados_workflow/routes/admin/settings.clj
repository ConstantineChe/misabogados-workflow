(ns misabogados-workflow.routes.admin.settings
  (:require [compojure.core :refer [defroutes GET PUT] :as c]
            [ring.util.response :refer [redirect response]]
            [monger.operators :refer :all]
            [misabogados-workflow.util :as util]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.access-control :as ac]
            [buddy.auth.accessrules :refer [restrict]]))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not autherized, " value)
          :role (-> request :session :role)}})

(defn get-settings [request]
  (response {:settings {:country "cl"}}))

(defn update-settings [request]
  (response {:status :updated}))

(defroutes settings-admin
  (GET "/admin/settings" [] (restrict get-settings
                                      {:handler ac/admin-access
                                       :on-error access-error-handler}))
  (PUT "/admin/settings" [] (restrict update-settings
                                      {:handler ac/admin-access
                                       :on-error access-error-handler})))
