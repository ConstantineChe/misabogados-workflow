(ns misabogados-workflow.routes.home
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]
            [misabogados-workflow.model :refer [datas]]
            [misabogados-workflow.layout.core :as layout]
            [hiccup.form :as form]))

(defn home-page []
  (layout/blank-page "home" [:div "hi"]))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (ok (-> "docs/docs.md" io/resource slurp)))
  (GET "/wf" [] (layout/blank-page "Form" (layout/render-form "Form" ["GET" "/nowhere"]
                                                        (.traverse datas)))))
