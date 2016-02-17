(ns misabogados-workflow.access-controll
  (:require [reagent.core :as r]
            [reagent.session :as session]))



(def no-role-links [["#" "Home" :home]
                    ["#about" "About" :about]
                    ["#debug" "Debug" :debug]])

(def components (r/atom {:nav-links no-role-links}))

(defn get-access [] @components)

(def client-dashboard [])

(defn reset-access! []
  (cond (= nil (:role (session/get :user)))
        (reset! components {:nav-links no-role-links})
        (= "client" (:role (session/get :user)))
        (reset! components {:nav-links (conj no-role-links ["#dashboard" "Dashboard" :dashboard])})
        (= "lawyer" (:role (session/get :user)))
        (reset! components {:nav-links (conj no-role-links ["#dashboard" "Dashboard" :dashboard])})
        (= "admin" (:role (session/get :user)))
        (reset! components {:nav-links (conj no-role-links ["#dashboard" "Dashboard" :dashboard])})))
