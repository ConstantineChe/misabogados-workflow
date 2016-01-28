(ns misabogados-workflow.app
  (:require [misabogados-workflow.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
