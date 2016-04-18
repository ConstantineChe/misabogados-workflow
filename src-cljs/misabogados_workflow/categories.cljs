(ns misabogados-workflow.categories
    (:require [reagent.core :as r]
              [misabogados-workflow.utils :as u]
              [reagent.session :as session]
              [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
              [misabogados-workflow.elements :as el]
              [bouncer.core :as b]
              [bouncer.validators :as v]
              [clojure.walk :refer [keywordize-keys]]
              [secretary.core :as secretary :include-macros true]))


(defn categories-table
  "Create table from categories list."
  [categories]
  [:table.table.table-hover.table-striped.panel-body
   [:thead
    [:th "Id"]
    [:th "Name"]
    [:th "Slug"]
    [:th "Showed by default"]
    [:th "Persons"]
    [:th "Enterprises"]
    [:th "Actions"]]
   [:tbody
    (for [category categories]
      [:tr {:key (:_id category)}
       [:td (:_id category)]
       [:td (:name category)]
       [:td (:slug category)]
       [:td (if (:showed_by_default category) "Yes" "No")]
       [:td (if (:persons category) "Yes" "No")]
       [:td (if (:enterprises category) "Yes" "No")]
       [:td [:a {:href (str "#admin/categories/" (:_id category) "/edit")} [:button.btn.btn-default "edit"]]
        [:button.btn.btn-default "delete"]]])]]
  )


(defn categories
  "Categories page component."
  []
  (let [categories (r/atom nil)]
    (GET (str js/context "/admin/categories") {:handler #(reset! categories (keywordize-keys %))
                                               :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))})
    (fn []
      [:div.container
       [:h1 "Categories"]
       [:button.btn.btn-primary "New category"]
       (categories-table @categories)])))


(defn edit-category
  "Edit category page component."
  []
  (let [id (session/get :current-category-id)
        category (r/atom nil)]
    (GET (str js/context "/admin/categories/" id) {:handler #(reset! category (keywordize-keys %))
                                                   :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))})
    (fn []
      [:div.container
       [:h1 "Edit categoty"]
       id [:br]
       @category]))
  )


(def pages
  {:admin/categories #'categories
   :admin/categories-edit #'edit-category})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/admin/categories" []
  (session/put! :page :admin/categories))

(secretary/defroute "/admin/categories/:id/edit" {id :id}
  (session/put! :page :admin/categories-edit)
  (session/put! :current-category-id id))
