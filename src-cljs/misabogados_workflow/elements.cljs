(ns misabogados-workflow.elements)

(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav {:key title}
   [:a.navbar-brand
    {:class (when (= page (session/get :page)) "active")

     :href uri
     :on-click #(reset! collapsed? true)}
    title]])
