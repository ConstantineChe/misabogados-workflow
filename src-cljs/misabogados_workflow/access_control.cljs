(ns misabogados-workflow.access-control
  (:require [reagent.core :as r]
            [reagent.session :as session]))

(def no-role-links [
                    ;["#" "Home" :home]
                    ;["#about" "About" :about]
                    ;["#debug" "Debug" :debug]
                    ])

(def components (r/atom {:nav-links-right (into no-role-links [["#login" "Login" :login]
                                                               ["#signup" "Signup" :signup]])}))

(defn get-access [] @components)

(def client-dashboard [])

(defn reset-access! []
  (case (:role (session/get :user))
    "client" (reset! components {:nav-links (into no-role-links [])
                                 :nav-links-right [["#logout" "Logout"]]})

     "lawyer" (reset! components {:nav-links (into no-role-links [
                                                                  ["#payments" "Pagos" :payments]])
                                  :nav-links-right (let [logout ["#logout" "Logout"]]
                                                         (if-let [own-profile (session/get :own-profile)] 
                                                           [[(str "/" own-profile) "Tu perfil"] logout]
                                                           [logout]) 
                                                         )})

     "operator" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                    ])
                                    :nav-links-right [["#logout" "Logout"]]})

     "finance" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                   ["#payments" "Pagos" :payments]])
                                   :nav-links-right [["#logout" "Logout"]]})

     "admin" (reset! components {:nav-links (into no-role-links [["#dashboard" "Dashboard" :dashboard]
                                                                 ["#payments" "Pagos" :payments]
                                                                 ["#admin" "Admin" :admin]])
                                 :nav-links-right [["#logout" "Logout"]]})
     (reset! components {:nav-links-right (into no-role-links [["#login" "Login" :login]
                                                         ["#signup" "Signup" :signup]])})))
