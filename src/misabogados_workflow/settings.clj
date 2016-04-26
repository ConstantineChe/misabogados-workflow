(ns misabogados-workflow.settings
  (:require [misabogados-workflow.db.core :as db]
            [monger.collection :as mc]
            [config.core :refer [env]]))

(def settings (atom {}))

(defn init! [] (reset! settings 
                       (mc/find-one-as-map @db/db "settings" {:country (or (:country env) "Chile")})))
