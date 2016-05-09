(ns misabogados-workflow.scheduler
   (:require [clojurewerkz.quartzite.scheduler :as qs]
             [clojurewerkz.quartzite.triggers :as t]
             [clojurewerkz.quartzite.jobs :as j :refer [defjob]]
             [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
             [monger.collection :as mc]
             [monger.operators :refer :all]
             [misabogados-workflow.db.core :refer [db oid]]
             [misabogados-workflow.email :refer [send-email]]
             [clj-time.local :as l]
             [clj-time.core :as time]))


(defn schedule-email [time attributes]
  (mc/insert @db "schedule" {:type "email" :scheduled-to time :scheduled-at (l/local-now) :executed false
                             :attributes attributes}))



(defjob Emails
  [ctx]
  (let [current-time (l/local-now)
        schedules (mc/find-maps @db "schedule" {:scheduled-to {$lte current-time}
                                                :executed false
                                                :type "email"})]
    (dorun (for [email schedules]
             (do (send-email (:attributes email))
                 (mc/update-by-id @db "schedule" (:_id email) {$set {:executed true}}))))))

(defn init-mail-scheduler!
  "Initialize email scheduler job"
  []
  (let [s   (-> (qs/initialize) qs/start)
        job (j/build
             (j/of-type Emails)
             (j/with-identity (j/key "jobs.emails.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (cron-schedule "15 * * ? * *"))))]
    (qs/schedule s job trigger))
  )
