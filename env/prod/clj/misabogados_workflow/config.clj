(ns misabogados-workflow.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[misabogados-workflow started successfully]=-"))
   :middleware identity})
