(ns misabogados-workflow.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [misabogados-workflow.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[misabogados-workflow started successfully using the development profile]=-"))
   :middleware wrap-dev})
