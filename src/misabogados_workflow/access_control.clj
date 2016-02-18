(ns misabogados-workflow.access-control
  (:require [buddy.auth.accessrules :refer [success error]]))

(defn role-access [request role] (if (is-role? [request role])
                                   true
                                   (error "no")))

(defn is-role? [request role] (= role (-> request :session :role)))

(defn admin-access [request] (role-access request :admin))

(defn lawyer-access [request] (role-access request :lawyer))


