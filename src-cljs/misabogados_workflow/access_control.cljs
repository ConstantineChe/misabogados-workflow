(ns misabogados-workflow.access-control
  (:require [reagent.core :as r]
            [reagent.session :as session]))

(def no-role-links [
                    ;["#" "Home" :home]
                    ;["#about" "About" :about]
                    ;["#debug" "Debug" :debug]
                    ])

(def components (r/atom {:nav-links (into no-role-links [["#login" "Login" :login]
                                                         ["#signup" "Signup" :signup]])}))

(defn get-access [] @components)

(def client-dashboard [])

(defn reset-access! []
  (cond (= "client" (:role (session/get :user)))
        (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                            ["#logout" "Logout"]])})
        (= "lawyer" (:role (session/get :user)))
        (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                            ["#payments" "Pagos" :payments]
                                                            ["#logout" "Logout"]])})
        (= "operator" (:role (session/get :user)))
        (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                            ["#payments" "Pagos" :payments]
                                                            ["#logout" "Logout"]])})
        (= "finance" (:role (session/get :user)))
        (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                            ["#payments" "Pagos" :payments]
                                                            ["#logout" "Logout"]])})
        (= "admin" (:role (session/get :user)))
        (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                            ["#payments" "Pagos" :payments]
                                                            ["#logout" "Logout"]
                                                            ["#admin" "Admin" :admin]])})
        :default
        (reset! components {:nav-links (into no-role-links [["#login" "Login" :login]
                                                            ["#signup" "Signup" :signup]])})))
