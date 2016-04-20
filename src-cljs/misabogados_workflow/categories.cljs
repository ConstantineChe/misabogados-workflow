(ns misabogados-workflow.categories
  (:require [reagent.core :as r]
            [misabogados-workflow.utils :as u]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [misabogados-workflow.elements :as el]
            [bouncer.core :as b]
            [inflections.core :as i]
            [bouncer.validators :as v]
            [misabogados-workflow.schema :as s]
            [clojure.walk :refer [keywordize-keys]]
            [secretary.core :as secretary :include-macros true]))

(def categories (r/atom nil))

(defn get-categories []
  (GET (str js/context "/admin/categories") {:handler #(reset! categories (keywordize-keys %))
                                               :error-handler #(case (:status %)
                                                                 403 (js/alert "Access denied")
                                                                 500 (js/alert "Internal server error")
                                                                 (js/alert (str %)))}))

(defn save-category [id data]
  (PUT (str js/context "/admin/categories/" id) {:params (update data :category dissoc :_id)
                                                 :handler #(do (aset js/window "location" "#admin/categories")
                                                               )
                                                 :error-handler  #(case (:status %)
                                                                    403 (js/alert "Access denied")
                                                                    500 (js/alert "Internal server error")
                                                                    (js/alert (str %)))}))

(defn create-category [data]
  (POST (str js/context "/admin/categories") {:params data
                                               :handler #(do (aset js/window "location" "#admin/categories")
                                                             )
                                               :error-handler  #(case (:status %)
                                                                  403 (js/alert "Access denied")
                                                                  500 (js/alert "Internal server error")
                                                                  (js/alert (str %)))}))

(defn delete-category [id name]
  (DELETE (str js/context "/admin/categories/" id) {:handler #(do (get-categories)
                                                                  (js/alert (str "category " name " deleted.")))
                                                    :error-handler  #(case (:status %)
                                                                    403 (js/alert "Access denied")
                                                                    500 (js/alert "Internal server error")
                                                                    (js/alert (str %)))}))


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
        [:button.btn.btn-default {:on-click #(delete-category (:_id category) (:name category))} "delete"]]])]]
  )


(defn categories-page
  "Categories page component."
  []
  (let []
    (get-categories)
    (fn []
      [:div.container-fluid
       [:h1 "Admin Dashboard"]
       [:div.btn-group
        [:a {:href "#admin"} [:button.btn {:class (if (= (session/get :page) :admin)
                                                               "btn-primary" "btn-default")} "Manage users"]]
        [:a {:href "#admin/categories"} [:button.btn {:class (if (= (session/get :page) :admin/categories)
                                                                "btn-primary" "btn-default")} "Manage categories"]]]
       [:legend "Categories"]
       [:a {:href "#admin/categories/new"} [:button.btn.btn-primary "New category"]]
       (categories-table @categories)])))


(defn edit-category
  "Edit category page component."
  []
  (let [id (session/get :current-category-id)
        category (r/atom nil)
        util (r/atom nil)
        options (r/atom nil)]
    (GET (str js/context "/admin/categories/" id) {:handler #(reset! category {:category (keywordize-keys %)})
                                                   :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))})
    (fn []
      [:div.container-fluid
       (el/create-form "Edit category" s/category-schema-expanded [category options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(save-category id @category)}
         "Save"]
        [:button.btn.btn-default
         {:on-click #(aset js/window "location" "#admin/categories")}
         "Cancel"]]])))

(defn new-category
  "New category page component."
  []
  (let [category (el/prepare-atom s/category-schema-expanded (r/atom nil))
        util (r/atom nil)
        options (r/atom nil)]
    (fn []
      [:div.container-fluid
             (str @category)

       (el/create-form "New category" s/category-schema-expanded [category options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(create-category @category)}
         "Create"]
        [:button.btn.btn-default
         {:on-click #(aset js/window "location" "#admin/categories")}
         "Cancel"]]]))
  )



(def pages
  {:admin/categories #'categories-page
   :admin/categories-edit #'edit-category
   :admin/categories-new #'new-category})

(defn page []
  [(pages (session/get :page))])

(secretary/defroute "/admin/categories" []
  (session/put! :page :admin/categories))

(secretary/defroute "/admin/categories/new" []
  (session/put! :page :admin/categories-new))

(secretary/defroute "/admin/categories/:id/edit" {id :id}
  (session/put! :page :admin/categories-edit)
  (session/put! :current-category-id id))
