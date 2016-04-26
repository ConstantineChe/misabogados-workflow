(ns misabogados-workflow.admin.categories
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [misabogados-workflow.elements :as el]
            [bouncer.core :as b]
            [inflections.core :as i]
            [bouncer.validators :as v]
            [misabogados-workflow.utils :as u]
            [misabogados-workflow.schema :as s :include-macros true]
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
                                                 :handler #(do (u/redirect "#admin/categories")
                                                               )
                                                 :error-handler  #(case (:status %)
                                                                    403 (js/alert "Access denied")
                                                                    500 (js/alert "Internal server error")
                                                                    (js/alert (str %)))}))

(defn create-category [data]
  (POST (str js/context "/admin/categories") {:params data
                                               :handler #(do (u/redirect "#admin/categories")
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
  (el/data-table categories
                 ["Id" "Name" "Slug" "Showed by default" "Persons" "Enterprises" "Actions"]
                 [:_id :name :slug
                  #(if (% :showed_by_default) "Yes" "No")
                  #(if (% :persons) "Yes" "No")
                  #(if (% :enterprises) "Yes" "No")
                  (fn [category] (list [:a {:href (str "#admin/categories/" (:_id category) "/edit")
                                           :key :edit}
                                       [:button.btn.btn-default "edit"]]
                                      [:button.btn.btn-default
                                       {:key :delete
                                        :on-click #(delete-category (:_id category) (:name category))}
                                       "delete"]))])
  )


(defn categories-tab
  "Categories page component."
  []
  (get-categories)
  (fn []
    [:div
     [:legend "Categories"]
     [:a {:href "#admin/categories/new"} [:button.btn.btn-primary "New category"]]
     (categories-table @categories)]))


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
       (el/create-form "Edit category" s/category [category options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(save-category id @category)}
         "Save"]
        [:button.btn.btn-default
         {:on-click #(u/redirect "#admin/categories")}
         "Cancel"]]])))

(defn new-category
  "New category page component."
  []
  (let [category (el/prepare-atom s/category (r/atom nil))
        util (r/atom nil)
        options (r/atom nil)]
    (fn []
      [:div.container-fluid
       (el/create-form "New category" s/category [category options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(create-category @category)}
         "Create"]
        [:button.btn.btn-default
         {:on-click #(u/redirect "#admin/categories")}
         "Cancel"]]]))
  )



(def pages
  {:admin/categories-edit #'edit-category
   :admin/categories-new #'new-category})


(secretary/defroute "/admin/categories/new" []
  (session/put! :page :admin/categories-new))

(secretary/defroute "/admin/categories/:id/edit" {id :id}
  (session/put! :page :admin/categories-edit)
  (session/put! :current-category-id id))
