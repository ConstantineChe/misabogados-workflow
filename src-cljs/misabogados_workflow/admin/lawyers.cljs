(ns misabogados-workflow.admin.lawyers
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET POST PUT DELETE]]
            [misabogados-workflow.elements :as el]
            [bouncer.core :as b]
            [inflections.core :as i]
            [bouncer.validators :as v]
            [misabogados-workflow.schema :as s]
            [misabogados_workflow.utils :as u]
            [clojure.walk :refer [keywordize-keys]]
            [secretary.core :as secretary :include-macros true])  )


(def lawyers (r/atom nil))

(defn get-lawyers []
  (GET (str js/context "/admin/lawyers") {:handler #(reset! lawyers (keywordize-keys %))
                                          :error-handler #(case (:status %)
                                                            403 (js/alert "Access denied")
                                                            500 (js/alert "Internal server error")
                                                            (js/alert (str %)))}))                                                                                                                                                                                                                                          (defn save-lawyer [id data]
  (PUT (str js/context "/admin/lawyers/" id) {:params (update data :lawyer dissoc :_id)
                                                 :handler #(do (u/redirect "#admin/lawyers")
                                                               )
                                                 :error-handler  #(case (:status %)
                                                                    403 (js/alert "Access denied")
                                                                    500 (js/alert "Internal server error")
                                                                    (js/alert (str %)))}))

(defn create-lawyer [data]
  (POST (str js/context "/admin/lawyers") {:params data
                                               :handler #(do (u/redirect "#admin/lawyers")
                                                             )
                                               :error-handler  #(case (:status %)
                                                                  403 (js/alert "Access denied")
                                                                  500 (js/alert "Internal server error")
                                                                  (js/alert (str %)))}))

(defn delete-lawyer [id name]
  (DELETE (str js/context "/admin/lawyers/" id) {:handler #(do (get-lawyers)
                                                                  (js/alert (str "lawyer " name " deleted.")))
                                                    :error-handler  #(case (:status %)
                                                                    403 (js/alert "Access denied")
                                                                    500 (js/alert "Internal server error")
                                                                    (js/alert (str %)))}))


(defn lawyers-tab []
  (get-lawyers)
  (fn []
    [:div
     [:legend "Lawyer profiles"]
     [:a {:href "#admin/lawyers/new"} [:button.btn.btn-primary "New Lawyer"]]
     (el/data-table @lawyers ["Id" "Name" "Email" "Phone" "Address" "Actions"]
                    [:_id :name :email :phone :address
                     (fn [lawyer] (list [:a {:href (str "#admin/lawyers/" (:_id lawyer) "/edit")
                                           :key :edit}
                                       [:button.btn.btn-default "edit"]]
                                      [:button.btn.btn-default
                                       {:key :delete
                                        :on-click #(delete-lawyer (:_id lawyer) (:name lawyer))}
                                       "delete"]))])]))


(defn edit-lawyer
  "Edit lawyer page component."
  []
  (let [id (session/get :current-lawyer-id)
        lawyer (r/atom nil)
        util (r/atom nil)
        options (r/atom nil)]
    (GET (str js/context "/admin/lawyers/" id) {:handler #(reset! lawyer {:lawyer (keywordize-keys %)})
                                                   :error-handler #(case (:status %)
                                                           403 (js/alert "Access denied")
                                                           500 (js/alert "Internal server error")
                                                           (js/alert (str %)))})
    (fn []
      [:div.container-fluid
       (el/create-form "Edit lawyer" s/lawyer [lawyer options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(save-lawyer id @lawyer)}
         "Save"]
        [:button.btn.btn-default
         {:on-click #(u/redirect "#admin/lawyers")}
         "Cancel"]]])))

(defn new-lawyer
  "New lawyer page component."
  []
  (let [lawyer (el/prepare-atom s/lawyer (r/atom nil))
        util (r/atom nil)
        options (r/atom nil)]
    (fn []
      [:div.container-fluid
             (str @lawyer)

       (el/create-form "New lawyer" s/lawyer [lawyer options util])
       [:div
        [:button.btn.btn-primary
          {:on-click #(create-lawyer @lawyer)}
         "Create"]
        [:button.btn.btn-default
         {:on-click #(u/redirect "#admin/lawyers")}
         "Cancel"]]]))
  )


(def pages
  {:admin/lawyers-edit #'edit-lawyer
   :admin/lawyers-new #'new-lawyer})


(secretary/defroute "/admin/lawyers/new" {id :id}
  (session/put! :page :admin/lawyers-new)
  )
(secretary/defroute "/admin/lawyers/:id/edit" {id :id}
  (session/put! :page :admin/lawyers-edit)
  (session/put! :current-lawyer-id id)  )
