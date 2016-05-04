(ns misabogados-workflow.settings
  (:require [monger.core :as mg]
            [monger.operators :refer :all]
            [monger.collection :as mc]
            [config.core :refer [env]]))

(defonce settings (atom {}))

(defonce db (atom nil))

(defn get-country []
  (or (:country env) "cl"))

(defn connect! []
  (reset! db (-> (:settings-database-url env)
                 mg/connect-via-uri :db)))

(defn disconnect! []
  (when-let [conn @db]
    (mg/disconnect conn)
    (reset! db nil)))

(defn reload! [] 
  (let [data (mc/find-one-as-map @db "settings" {:country (get-country)})] 
    (reset! settings data)))

(defn save! [data] 
  (mc/update @db "settings" 
             {:country (get-country)} 
             {$set (dissoc data :country)})
  (reload!))

(defn init! [] (do (connect!)
                   (reload!)))



