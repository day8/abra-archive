(ns abra.core
  (:require [abra.state :as state]
            [reagent.core :as reagent]
            [abra.dialog :as dialog]
            [re-com.core  :refer [input-text text-area button hyperlink label 
                                  spinner progress-bar checkbox radio-button 
                                  title slider]]
            [re-com.box   :refer [h-box v-box box gap line]]))

;; redirects any println to console.log
(enable-console-print!)

(def ipc (js/require "ipc"))

;; equivalent of:  process.versions['atom-shell']
(def atom-shell-version (aget (.-versions js/process) "atom-shell"))

;;-- lein Status ------------------------------------------------------------------------------------------------------
;; Listen for problems with lein

(def lein-status (reagent/atom {:error false}))                   ;; true is good, false is bad
(.send ipc "get-lein-status")                                    ;; ask for the status
(.on ipc "lein-status" #(reset! lein-status (js->clj %1)))    ;;

(def lein-repl-status (reagent/atom {:error false}))                   ;; true is good, false is bad
(.send ipc "get-lein-repl-status")                                    ;; ask for the status
(.on ipc "lein-repl-status" (fn [arg]
                              (reset! lein-repl-status (js->clj arg))))    ;;

(.on ipc "translated-javascript" (fn [arg]
                                   (swap! state/app-state assoc :javascript-string arg)
                                   (.send ipc "get-lein-repl-status")))    ;;

(defn tell-user-about-lein-problems
  []
  [:div "It doesn't look as if you have lein installed on this machine"])


;;--------------------------------------------------------------------------------------------------------------------

(defn get-element-by-id
  [id]
  (.getElementById js/document id))


(defn nrepl-state-text []
  [:p "Nrepl state -- " (if @lein-repl-status
                          "running"
                          "stopped")])

(defn field-label
  [text]
  [label 
   :label text
   :style {:font-variant "small-caps"}]
  )
(defn debug-view
  []
  [v-box
   :width "100%"
   :children [
              [page-header "Start Debugging"]
              [nrepl-state-text]
              [v-box 
               :gap "20px"
               :children [[button
                           :label "STOP"
                           :on-click  #(swap! state/app-state assoc :debugging? false)
                           :class    "btn-danger"]
                          [h-box
                           :justify :start
                           :gap "20px"
                           :children [[v-box
                                       :children [[field-label "namespace"]
                                                  [text-area
                                                   :model (:namespace-string @state/app-state)
                                                   :on-change #(swap! state/app-state
                                                                      assoc :namespace-string
                                                                      %)]]]
                                      [v-box
                                       :children [[field-label "locals"]
                                                  [text-area
                                                   :model (:locals-string @state/app-state)
                                                   :on-change #(swap! state/app-state
                                                                      assoc :locals-string
                                                                      %)]]]]]
                          [h-box
                           :gap "5px"
                           :children [[v-box
                                       :children [[field-label "clojurescript"]
                                                  [text-area
                                                   :model (:clojurescript-string
                                                            @state/app-state)
                                                   :on-change #(swap! state/app-state
                                                                      assoc :clojurescript-string
                                                                      %)]]]
                                      [v-box 
                                       :children [[gap
                                                   :size "20px"]
                                                  [button
                                                   :label "Translate"
                                                   :class "btn-primary"
                                                   :on-click #(translate)]]]
                                      [v-box
                                       :children [[field-label "javascript"]
                                                  [text-area
                                                   :model (:javascript-string @state/app-state)]]]
                                      [v-box
                                       :children [[field-label "javascript result"]
                                                  [text-area]]]]]]]]])

(defn page-header
  [header]
  [h-box :children [[h-box :size "80%"
                     :children [[:h3 header]]]
                    [v-box :children [[:div nil "Abra"]
                                      [:div (str "Atom Shell Version: " atom-shell-version)]
                                      ]]]])

(defn project-form
  []
  [h-box 
   :children [[:div "Root directory   "]
              [v-box :children [[input-text 
                                 :model (:project-dir @state/app-state)]
                                [:div "This directory is the root of your clojurescript project"]]]
              [:input.btn.btn-success
               {:type "button"
                :value "Browse"
                ;; :disabled (if (stadebugging? @state/app-state) "disabled" "")
                :on-click  (fn [] 
                             (dialog/open {:title "Open Project.clj Directory" 
                                           :properties ["openDirectory"] 
                                           :defaultPath  "c:\\"
                                           :filters [{:name "Project Files" :extensions ["clj"]}]}
                                          (fn [[project-dir]] 
                                            (swap! state/app-state assoc :project-dir project-dir))))}]]])

(defn debug-url
  []
  [h-box 
   :children [[:div "Debug URL   "]
              [v-box 
               :children [[input-text 
                           :model (:debug-url @state/app-state)
                           :on-change #(swap! state/app-state assoc :debug-url %)]
                          [:div "You want to debug an HTML page right? 
                                Via which URL should it be loaded? 
                                Probably something like:"]
                          [:ul [:li "file:///path/to/my/project/folder/index.html"]
                           [:li "http://localhost:3449/index.html  (if you are running figwheel or and external server)"]]]]]])

(defn start-debugging 
  "Start the lein repl and open the debugger view"
  []
  (swap! state/app-state assoc :debugging? true)
  (.send ipc "start-lein-repl" (:project-dir @state/app-state)))

(defn stop-debugging 
  "Stop the lein repl and close the debugger view"
  []
  (swap! state/app-state assoc :debugging? false)
  (.send ipc "stop-lein-repl"))

(defn details-view
  []
  [:div
   [page-header "What Debug Session Do You Want To Launch?"]
   [project-form]
   [debug-url]
   (when (:error @lein-status) [tell-user-about-lein-problems])
   
   
   [:input.btn.btn-success
    {:type "button"
     :value "Debug"
     :on-click start-debugging}]
   [:input.btn.btn-success
    {:type "button"
     :value "Message"
     ;; :disabled (if (stadebugging? @state/app-state) "disabled" "")
     :on-click  #(dialog/message {:type "info" :message "Didn't work" :buttons ["Cancel" "Ok"]})}]])

(defn translate 
  "translates the clojurescript on this page"
  []
  (.send ipc "translate-clojurescript" (:clojurescript-string @state/app-state)))
;;

(defn main-page
  []
  (if (:debugging? @state/app-state)
    [debug-view]
    [details-view]))


(defn start
  []
  (state/initialise)
  (reagent/render-component [main-page] (get-element-by-id "app")))
