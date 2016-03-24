(ns misabogados-workflow.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [misabogados-workflow.layout :refer [error-page]]
            [misabogados-workflow.routes.home :refer [home-routes]]
            [misabogados-workflow.routes.registration :refer [registration-routes]]
            [misabogados-workflow.routes.session :refer [session-routes]]
            [misabogados-workflow.routes.users :refer [users-routes]]
            [misabogados-workflow.routes.payments :refer [payments-routes]]
            [misabogados-workflow.routes.payment-requests :refer [payment-requests-routes]]
            [misabogados-workflow.middleware :as middleware]
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
   home-routes
   registration-routes
   session-routes
   users-routes
   payments-routes
   payment-requests-routes
   (route/not-found
    (:body
     (error-page {:status 404
                  :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
