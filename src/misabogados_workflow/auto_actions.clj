(ns misabogados-workflow.auto-actions
  (:require
   [misabogados-workflow.db.core :refer [db oid]]
   [monger.collection :as mc]
   [monger.operators :refer :all]
   [misabogados-workflow.scheduler :refer [schedule-email]]
   [clj-time.core :as t]
   [clj-time.local :as l]
   [clj-time.format :as f])
  (:import [org.apache.commons.lang3 LocaleUtils]))

(def date-time-formatter (f/with-locale (f/formatter "d 'de' MMMM 'de' yyyy H:mm") (LocaleUtils/toLocale "es_US")))

(f/unparse date-time-formatter (l/local-now))

(defn change-lawyer [lead]
  (let [id (:_id lead)]
    (prn (mc/update @db "leads" {:_id id} {$unset {:matches ""}}))))

(defn schedule-meeting [lead]
  (let [meeting (get-in lead [:matches 0 :meetings 0])
        lawyer-id (get-in lead [:matches 0 :lawyer_id])
        lawyer (mc/find-one-as-map @db "lawyers" {:_id (oid lawyer-id)})
        client (mc/find-one-as-map @db "clients" {:_id (oid (:client_id lead))})
        schedule-to (t/minus (l/to-local-date-time (:time meeting)) (t/hours 8))
        meeting-time-formatted (f/unparse date-time-formatter (l/to-local-date-time (:time meeting)))]
    (schedule-email schedule-to {:email (:email client)
                                 :template "meeting-notification-email.html"
                                 :subject "Meeting"
                                 :data {:name (:name client)
                                        :time meeting-time-formatted
                                        :lawyer (:name lawyer)}})))

;;TODO if person, next action — refer, if enterprise, refer-enterprise or smth

(defn person-enterprise-switch [lead]
  :refer)
