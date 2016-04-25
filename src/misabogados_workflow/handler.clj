(ns misabogados-workflow.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [misabogados-workflow.layout :refer [error-page]]
            [misabogados-workflow.routes.home :refer [home-routes]]
            [misabogados-workflow.routes.registration :refer [registration-routes]]
            [misabogados-workflow.routes.session :refer [session-routes]]
            [misabogados-workflow.routes.users :refer [users-routes]]
            [misabogados-workflow.routes.payments :refer [payments-routes payments-integration-routes]]
            [misabogados-workflow.routes.payment-requests :refer [payment-requests-routes]]
            [misabogados-workflow.routes.leads :refer [leads-routes]]
            [misabogados-workflow.routes.admin.categories :refer [categories-admin]]
            [misabogados-workflow.routes.admin.lawyers :refer [lawyers-admin]]
            [misabogados-workflow.middleware :as middleware]
            [misabogados-workflow.settings :as settings]
            [misabogados-workflow.db.indexes :as indexes]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [misabogados-workflow.db.core :as db]
            [config.core :refer [env]]
            [misabogados-workflow.config :refer [defaults]]
            [mount.core :as mount]
            [luminus.logger :as logger]))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (logger/init env)
  (db/connect!)
  (indexes/setup-indexes)
  (settings/init!)
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  ((:init defaults)))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (log/info "misabogados-workflow is shutting down...")
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (log/info "shutdown complete!"))

(def app-routes
  (routes
   (wrap-routes #'categories-admin middleware/wrap-csrf)
   (wrap-routes #'lawyers-admin middleware/wrap-csrf)
   (wrap-routes #'home-routes middleware/wrap-csrf)
   (wrap-routes #'registration-routes middleware/wrap-csrf)
   (wrap-routes #'session-routes middleware/wrap-csrf)
   (wrap-routes #'users-routes middleware/wrap-csrf)
   (wrap-routes #'payments-routes middleware/wrap-csrf)
   (wrap-routes #'payment-requests-routes middleware/wrap-csrf)
   (wrap-routes #'leads-routes middleware/wrap-csrf)
   payments-integration-routes
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
