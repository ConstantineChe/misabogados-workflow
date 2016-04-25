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
  (case (:role (session/get :user))
    "client" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                 ["#logout" "Logout"]])})
     "lawyer" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                  ["#payments" "Pagos" :payments]
                                                                  ["#logout" "Logout"]])})
     "operator" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                    ["#payments" "Pagos" :payments]
                                                                    ["#logout" "Logout"]])})
     "finance" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                   ["#payments" "Pagos" :payments]
                                                                   ["#logout" "Logout"]])})
     "admin" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                 ["#payments" "Pagos" :payments]
                                                                 ["#admin" "Admin" :admin]
                                                                 ["#logout" "Logout"]])})
     (reset! components {:nav-links (into no-role-links [["#login" "Login" :login]
                                                         ["#signup" "Signup" :signup]])})))
