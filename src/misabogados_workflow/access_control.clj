(ns misabogados-workflow.access-control
  (:require [buddy.auth.accessrules :refer [success error]]
            [misabogados-workflow.test.util :refer [*test-role*]]))

(defn is-role? [request role] (if *test-role* (= role *test-role*) (= role (-> request :session :role))))

(defn role-access [request role] (if (is-role? request role)
                                   true
                                   (error (str (name role) " access required"))))

(defn admin-access [request] (role-access request :admin))

(defn lawyer-access [request] (role-access request :lawyer))
