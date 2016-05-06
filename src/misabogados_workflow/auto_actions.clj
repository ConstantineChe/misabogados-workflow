(ns misabogados-workflow.auto-actions
  (:require
   [misabogados-workflow.db.core :refer [db oid]]
   [monger.collection :as mc]
   [monger.operators :refer :all])  )

(defn change-lawyer [lead]
          (let [id (:_id lead)]
            (prn (mc/update @db "leads" {:_id id} {$unset {:matches ""}}))))
