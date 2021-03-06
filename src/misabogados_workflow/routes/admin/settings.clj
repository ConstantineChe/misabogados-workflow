(ns misabogados-workflow.routes.admin.settings
  (:require [compojure.core :refer [defroutes GET PUT] :as c]
            [ring.util.response :refer [redirect response]]
            [misabogados-workflow.util :as util]
            [misabogados-workflow.settings :as sttngs]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.access-control :as ac]
            [buddy.auth.accessrules :refer [restrict]]))

(defn access-error-handler [request value]
  {:status 403
   :header {}
   :body {:error (str "not authorized, " value)
          :role (-> request :session :role)}})

(defn get-settings [request]
  (response @sttngs/settings))

(defn update-settings [request]
  (sttngs/save! (get-in request [:params :settings]))
  (response {:status :updated}))

(defroutes settings-admin
  (GET "/admin/settings" [] (restrict get-settings
                                      {:handler ac/admin-access
                                       :on-error access-error-handler}))
  (PUT "/admin/settings" [] (restrict update-settings
                                      {:handler ac/admin-access
                                       :on-error access-error-handler})))
