(ns misabogados-workflow.settings
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [config.core :refer [env]]))

(defonce settings (atom {}))

(defonce db (atom nil))

(defn connect! []
  (reset! db (-> (:settings-database-url env)
                          mg/connect-via-uri :db)))

(defn disconnect! []
  (when-let [conn @db]
    (mg/disconnect conn)
    (reset! db nil)))

(defn reload! [] (reset! settings 
                         (mc/find-one-as-map @db "settings" {:country (or (:country env) "cl")})))

(defn init! [] (do (connect!)
                   (reload!)))



