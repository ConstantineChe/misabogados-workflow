(ns misabogados-workflow.db.indexes
  (:require [misabogados-workflow.db.core :refer [db]]
            [monger.collection :as mc]))

(defn setup-indexes []
  (println "=======Seting Up Indexes======")
  (when @db
    (mc/ensure-index @db "users" (array-map :email 1) {:unique true})
    (mc/ensure-index @db "payment_requests" (array-map :lawyer 1))))
