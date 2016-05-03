(ns misabogados-workflow.admin.settings
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [misabogados-workflow.ajax :refer [GET PUT]]
            [misabogados-workflow.elements :as el]
            [bouncer.core :as b]
            [inflections.core :as i]
            [bouncer.validators :as v]
            [misabogados-workflow.schema :as s]
            [misabogados-workflow.utils :as u]
            [clojure.walk :refer [keywordize-keys]]
            [secretary.core :as secretary :include-macros true])  )

;; (def setings (r/atom nil))

(defn save-settings [data]
  (PUT (str js/context "/admin/settings") {:params (update data :settings dissoc :_id)
                                           :handler #(do (u/redirect "#admin/settings"))
                                           :error-handler  #(case (:status %)
                                                              403 (js/alert "Access denied")
                                                              500 (js/alert "Internal server error")
                                                              (js/alert (str %)))}))

(defn settings-tab []
  (let [settings (r/atom nil)
        util (r/atom nil)
        options (r/atom nil)]
     (GET (str js/context "/admin/settings") {:handler #(reset! settings (keywordize-keys %))
                                              :error-handler #(case (:status %)
                                                                403 (js/alert "Access denied")
                                                                500 (js/alert "Internal server error")
                                                                (js/alert (str %)))})
     (fn [] [:div.container-fluid
             [:p (str @settings)]
             (el/create-form "Ajustos del sitio" s/settings [settings options util])
             [:div
              [:button.btn.btn-primary
               {:on-click #(save-settings @settings)}
               "Save"]]])))
