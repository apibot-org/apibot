(ns apibot.views.navbar
  "The global navbar."
  (:require
    [apibot.api :as api]
    [apibot.auth0 :as auth0]
    [apibot.coll :as coll]
    [apibot.env :as env]
    [apibot.graphs :as graphs]
    [apibot.router :as router]
    [apibot.state :as state :refer [*selected-graph *project-list *selected-project]]
    [apibot.views.commons :as commons :refer [glyphicon-run]]
    [apibot.views.dialogs :as dialogs]
    [reagent.core :refer [atom cursor]]))

(defn dialog-add-project
  []
  (let [*project-name (atom "")
        validator coll/non-empty-string?
        on-submit (fn [e]
                    (let [new-project (state/add-project @*project-name)]
                      (api/save-project new-project)
                      (dialogs/hide!)
                      (router/goto-project (:id new-project))
                      (.preventDefault e)))]
    (fn []
      [dialogs/generic-dialog
       "Create a new project"
       [:form
        {:on-submit on-submit}
        [commons/form-group-bindable
         {:spec validator
          :name "Project Name"
          :auto-focus true
          :help "Enter a name for your project"}
         *project-name]]
       [:div
        [:button.btn.btn-default
         {:on-click #(dialogs/hide!)}
         "Cancel"]
        [:button.btn.btn-primary
         {:class    (if (validator @*project-name) nil "disabled")
          :on-click on-submit}
         [:span.glyphicon.glyphicon-plus]
         " Create"]]])))


(defn link-project []
  (let [*show-projects-dropdown (atom false)]
    (fn []
      [:li.dropdown
       {:class (if @*show-projects-dropdown "active")
        :on-mouse-enter #(reset! *show-projects-dropdown true)
        :on-mouse-leave #(reset! *show-projects-dropdown false)}
       [:a.dropdown-toggle
        {:role     "button"
         :on-click #(router/goto-project (:id @*selected-project))}
        (:name @*selected-project) " " [:span.caret]]

       ;; -- dropdown menu --
       [:ul.dropdown-menu
        {:style {:display (if @*show-projects-dropdown "block" "none")}}


        ;; -- Project List --
        [:li.dropdown-header "Projects"]
        (doall
          (for [project (sort-by :name @*project-list)]
            [:li
             {:key (:id project)}
             [:a
              {:role     "button"
               :on-click #(if (router/in-projects?)
                            (router/goto-project (:id project))
                            (reset! *selected-project project))}
              [:span
               {:class (if (= (:id @*selected-project) (:id project))
                         "text-primary" nil)}
               (:name project)]]]))

        [:li.divider {:role "separator"}]

        ;; -- Create a new project --
        [:li>a
         {:role     "button"
          :on-click (fn [e]
                      (reset! *show-projects-dropdown false)
                      (dialogs/show! [dialog-add-project]))}
         [:span.glyphicon.glyphicon-plus] " New Project"]

        ;; -- Project Settings --
        (when-not (router/in-projects?)
          [:li>a
           {:role     "button"
            :on-click (fn [e]
                        (router/goto-project (:id @*selected-project)))}
           [:span.glyphicon.glyphicon-wrench] " Project Settings"])]])))


(defn active-class-if-page [page]
  (if (router/current-page? page)
    "active" ""))

(defn navbar
  [*app-state]
  (let [*profile (cursor *app-state [:profile])]
    [:nav.navbar.navbar-static-top.navbar-inverse {:style {:margin-bottom "0px"}}
     [:div.container-fluid
      [:ul.nav.navbar-nav.navbar-left
       (when (= :dev env/env)
         [:li
          [:a.navbar-brand
           {:on-click (fn [e])}
           " Apibot DEV"]])

       [link-project]

       [:li {:class (active-class-if-page "#editor")}
        [:a
         {:href (str "#editor/" (:id @*selected-graph))}
         [:span.glyphicon.glyphicon-edit] " Editor"]]

       [:li {:class (active-class-if-page "#executables")}
        [:a
         {:href "#executables"}
         [:span.glyphicon.glyphicon-flash] " Executables"]]

       [:li {:class (active-class-if-page "#executions")}
        [:a
         {:role     "button"
          :on-click #(router/goto-executions)}
         [:span.glyphicon.glyphicon-th-list] " Execution History"]]]


      [:ul.nav.navbar-nav.navbar-right
       [:li
        [:a
         {:href   "http://apibot.co/docs"
          :target "_blank"}
         [:span.glyphicon.glyphicon-education] " Docs"]]

       [:li
        [:a
         {:role     "button"
          :on-click #(auth0/logout)}
         [:span.glyphicon.glyphicon-user] " Logout"]]]]]))


