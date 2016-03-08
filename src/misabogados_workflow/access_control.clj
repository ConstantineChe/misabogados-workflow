(ns misabogados-workflow.access-control
  (:require [buddy.auth.accessrules :refer [success error]]))

(defn is-role? [request role] (= role (-> request :session :role)))

(defn role-access [request role] (if (is-role? request role)
                                   true
                                   (error (str (name role) " access required"))))

(defn admin-access [request] (role-access request :admin))

(defn operator-access [request] (role-access request :operator))

(defn lawyer-access [request] (role-access request :lawyer))
