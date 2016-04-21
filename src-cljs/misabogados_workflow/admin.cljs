(ns misabogados-workflow.admin
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [misabogados-workflow.admin.users :refer [users]]
            [misabogados-workflow.admin.categories :refer [categories-page]]
            [secretary.core :as secretary :include-macros true]))


(def tabs
  {:users #'users
   :categories #'categories-page})

(defn tab []
  [(tabs (session/get-in [:admin :tab]))])



(defn admin []
  (fn []
    (when-not (session/get-in [:admin :tab]) (session/assoc-in! [:admin :tab] :users))
    [:div.container-fluid
     [:div.row
      [:div.sidebar.col-sm-3.col-md-2
       [:ul.nav.nav-sidebar
        [:li {::class (if (= (session/get-in [:admin :tab]) :users) "active")}
         [:a {:href "#admin/users"} "Manage users"]]
        [:li {:class (if (= (session/get-in [:admin :tab]) :categories) "active")}
         [:a {:href "#admin/categories"} "Manage categories"]]]]
      [:div.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.main
       [:h1 "Admin Dashboard"]
       [tab]]]
     ]))

(secretary/defroute "/admin/categories" []
  (do (session/put! :page :admin)
      (session/assoc-in! [:admin :tab] :categories)))

(secretary/defroute "/admin/users" []
  (do (session/put! :page :admin)
      (session/assoc-in! [:admin :tab] :users)))
