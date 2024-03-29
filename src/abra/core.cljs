(ns abra.core
  (:require-macros [re-com.core :refer [handler-fn]])
  (:require [reagent.core :as reagent]
            [abra.dialog :as dialog]
            [abra.state :as state]    ;; although not used, leave it in here, or else the subscriptions don't get pulled in.
            [re-com.core :refer [input-text input-textarea label title 
                                 throbber
                                 button info-button
                                 h-box v-box box scroller gap line
                                 vertical-bar-tabs
                                 v-split
                                 modal-panel
                                 checkbox
                                 selection-list
                                 md-icon-button
                                 hyperlink
                                 popover-anchor-wrapper
                                 popover-content-wrapper]]
            [cljs.core.async :refer [<!]]
            [re-frame.core :refer [dispatch]]
            [re-frame.subs :refer [subscribe]]
            [abra.handlers]
            [abra.nrepl-handlers]
            [abra.keys :as keys]
            [figwheel.client :as fw]))

;; redirects any println to console.log
(enable-console-print!)
(def clipboard (.require (js/require "remote") "clipboard"))

; (fw/start {
;              ;; configure a websocket url if yor are using your own server
;              :websocket-url "ws://localhost:3449/figwheel-ws"

;              ;; optional callback
;              :on-jsload (fn [] 
;                           (println (reagent/force-update-all)))

;              ;; when the compiler emits warnings figwheel
;              ;; blocks the loading of files.
;              ;; To disable this behavior:
;              :load-warninged-code true
;              })

;; equivalent of:  process.versions['atom-shell']
(def atom-shell-version (aget (.-versions js/process) "atom-shell"))

;;------------------------------------------------------------------------------

(defn get-element-by-id
  [id]
  (.getElementById js/document id))

(defn page-header
  [header]
  [h-box :children [[h-box :size "80%"
                     :children [[title 
                                 :label header]]]
                    ]])

(defn nrepl-state-text 
  []
  (let [lein-repl-status (subscribe [:lein-repl-status])]
    (fn []
      [:p "Nrepl state -- " 
       (if @lein-repl-status
         "running"
         "stopped")])))

(defn field-label
  ;takes the field label and a text or hiccup help text
  ([text]
   (field-label text nil))
  ([text info]
   [h-box 
    :children (concat 
                [[label 
                  :label text
                  :style {:font-variant "small-caps"}]]
                (when info
                  [[gap :size "5px"] 
                   [info-button
                    :info (if string? info 
                            [:div info]
                            info)]]))]))

(defn is-model-in-tab?
  "checks if there is an id in the tabs that matches model"
  [model tabs]
  (some #(= (:id %) model) tabs))

(defn- label-clicked
  [selections item-id required?]
  ; toggle selected item
  (if (and required? (selections item-id))
    selections  ;; prevent unselect
    (if (selections item-id) #{} #{item-id})))

(defn- as-label
  [item id-fn selections on-change disabled? label-fn required? as-exclusions?]
  (let [item-id (id-fn item)]
    [box
     :class "list-group-item compact"
     :style (when (selections item-id) {:background-color "#428BCA"}) ; same as dropdown selection
     :attr {:on-click (handler-fn (when-not disabled?
                                    (on-change (label-clicked selections item-id required?))))}
     :child [label
             ;:disabled? disabled?
             :style (re-com.selection-list/label-style (selections item-id) as-exclusions? "white")
             :label (label-fn item)]]))

(defn history-locals
  []
  (let [locals (subscribe [:scoped-locals])
        call-frames (subscribe [:call-frames])
        call-frame-id (subscribe [:call-frame-id])
        local-id (subscribe [:local-id])
        disabled (subscribe [:disabled])
        command-history (subscribe [:command-history])
        command-id (reagent/atom nil)]
    (fn
      []
      [h-box
       :justify :start
       :gap "20px"
       :children (concat  
                   [[v-box
                     :children 
                     [[field-label "Command history" 
                       "previous repl commands"]
                      [selection-list
                       :model #{}
                       :choices (into [] 
                                      (for [c @command-history]
                                        {:id c}))
                       :label-fn :id
                       :item-renderer as-label
                       :on-change (fn [id]
                                    (let [cljs-string (first id)]
                                      (dispatch [:clojurescript-string 
                                                 cljs-string])))
                       :multi-select? false
                       :required? false
                       :width "300px"
                       :height "300px"]]]]
                   (when (and (not @disabled) (is-model-in-tab? @call-frame-id @call-frames)) 
                     (when-let [locals-tab (sort-by :label (vals (get @locals @call-frame-id)))]
                       (let [[local-map] (filter #(= (:id %) @local-id) locals-tab)]
                         [[v-box
                           :children 
                           [[field-label "call-frames" "the active call frames"]
                            [scroller
                             :h-scroll :off
                             :height "300px"
                             :child [selection-list
                                     :width "250px"
                                     :model #{@call-frame-id}
                                     :choices @call-frames
                                     :label-fn :label
                                     :item-renderer as-label
                                     :multi-select? false
                                     :required? true
                                     :on-change 
                                     #(dispatch [:change-call-frame-id (first %)])]]]]  
                          (when (seq locals-tab) 
                            [v-box
                             :children 
                             [[field-label "locals"]
                              [scroller
                               :h-scroll :off
                               :height "300px"
                               :child [selection-list
                                       :width "250px"
                                       :model #{@local-id}
                                       :label-fn :label
                                       :item-renderer as-label
                                       :choices locals-tab
                                       :multi-select? false
                                       :on-change #(dispatch [:change-local-id 
                                                              (first %)])]]]]) 
                          (when (and @local-id (:value local-map))
                            [v-box
                             :children 
                             [[field-label "local value"]
                              [input-textarea
                               :model (:value local-map)
                               :on-change #()
                               :height "300px"]]])]))))])))

(defn clojurescript-input-output
  []
  (let [lein-repl-status (subscribe [:lein-repl-status])
        clojurescript-string (subscribe [:clojurescript-string])
        javascript-string (subscribe [:javascript-string])
        js-print-string (subscribe [:js-print-string])
        show-spinner (subscribe [:show-spinner])
        namespace-string (subscribe [:namespace-string])
        showing-namespace? (reagent/atom false)]
    (fn
      []
      (let [elements [[v-box
                       :children [[field-label "clojurescript"]
                                  [input-textarea
                                   :model @clojurescript-string
                                   :width "300px"
                                   :height "100px"
                                   :on-change #(dispatch
                                                 [:clojurescript-string %])
                                   :attr {:on-input 
                                          (fn [event]
                                            (let [value (.-value 
                                                          (.-target event))]
                                              (dispatch [:clojurescript-string 
                                                         value])))}]]]
                      [v-box
                       :align :center
                       :children [[:div {:class "md-forward rc-icon-larger"
                                          :style {:color "lightgrey"}}]
                                  [gap 
                                   :size "5px"]
                                  [popover-anchor-wrapper
                                   :showing? showing-namespace?
                                   :position :above-center
                                   :anchor [hyperlink 
                                            :label "(ns ...)"
                                            :on-click  
                                            #(swap! showing-namespace? not)]
                                   :popover 
                                   [popover-content-wrapper
                                    :showing? showing-namespace?
                                    :position :above-center
                                    :title 
                                    "Namespace of the file inspected"
                                    ; :width "300px"
                                    ; :height "200px"
                                    :body 
                                    [(fn [] 
                                      [input-textarea
                                       :model @namespace-string
                                       :placeholder 
                                       "(ns my.namespace\n  (:require [my.require]))"
                                       :on-change #(dispatch 
                                                     [:namespace-string %])
                                       :rows "5"
                                       :width "350px"
                                       :height "300px"])]]]
                                  [button
                                   :label "eval"
                                   :class "btn-primary"
                                   :on-click #(dispatch [:translate])
                                   :disabled? (not @lein-repl-status)]]]]
            result-elements (if @show-spinner 
                              [[v-box 
                                :children [[gap
                                            :size "20px"]
                                           [throbber]]]]
                                [[v-box
                                  :children 
                                  [[h-box 
                                    :justify :between
                                    :children 
                                    [[field-label "result"]
                                     [h-box 
                                      :gap "5px"
                                      :children 
                                      [[field-label "copy js"]
                                       [md-icon-button
                                        :md-icon-name "md-content-copy"
                                        :on-click (fn []
                                                    (.writeText 
                                                      clipboard
                                                      @javascript-string))]]]]]
                                   [input-textarea
                                    :model @js-print-string
                                    :width "300px"
                                    :height "100px"
                                    :on-change #()]]]]
                                [])]
        [h-box
         :gap "10px"
         :children (concat elements result-elements)]))))

(defn abra-debug-panel []
  (let [debug-crmux-url (subscribe [:debug-crmux-url])]
    (fn []
      [h-box 
       :size "auto"
       :children [[:iframe.debug-iframe {:src @debug-crmux-url
                                         :seamless "seamless"}]]])))

(defn top-debug-panel
  []
  [scroller 
   :child [v-box 
           :gap "20px"
           ;;:size "auto"
           :children [[h-box 
                       ; :align :center
                       :gap "5px"
                       :children [
                                  [button
                                   :label "STOP"
                                   :on-click  #(dispatch [:stop-debugging])
                                   :class    "btn-danger"]
                                  [button
                                   :label "refresh"
                                   :on-click #(dispatch [:refresh-page])
                                   :disabled? false]
                                  [gap 
                                   :size "40px"]
                                  [nrepl-state-text]]]
                      [history-locals]
                      [clojurescript-input-output]]]])

(defn debug-view
  []
  (let [disabled (subscribe [:disabled])]
    (fn
      []
      [v-box
       :height "100%"
       :children [[v-split
                   :initial-split "60%"
                   :panel-1 [top-debug-panel]
                   :panel-2 [abra-debug-panel]]
                  (when @disabled 
                    [modal-panel
                     :child [:div "Please wait for nrepl to start"]])]])))

(defn project-form
  []
  (let [project-dir (subscribe [:project-dir])]
    (fn 
      []
      [h-box 
       :gap "10px"
       :children 
       [
        [v-box 
         :children 
         [[field-label "project directory" 
           [:span "This is the directory which contains the " [:span.info-bold "project.clj"]  " or "  [:span.info-bold "build.boot"] " for your ClojureScript project"]]
          [v-box
           :children
           [[h-box
             :gap "2px"
             :children 
             [[input-text 
               :model @project-dir
               :on-change #(dispatch [:project-dir %])]
              [button
               :label "Browse"
               :on-click  
               #(dialog/open 
                  {:title "Open Project.clj Directory" 
                   :properties ["openDirectory"] 
                   :defaultPath  "c:\\"
                   :filters [{:name "Project Files" 
                              :extensions ["clj"]}]}
                  (fn [[project-dir]] 
                    (dispatch 
                      [:project-dir project-dir])))]]]]]]]]])))

(defn debug-url
  []
  (let [debug-url (subscribe [:debug-url])]
    (fn 
      []
      [v-box 
       :children 
       [[field-label "debug url" 
         [v-box
          ; :width "400px"
          :children [[:p "You want to debug an HTML page right?"]
                     [:p "Via which URL should this page be loaded? "]
                     [:div "Probably something like:"]
                     [:ul
                      [:li "file:///path/index.html"]
                      [:li "http://localhost:3449/index.html "
                       [:br]
                       "(if you are running figwheel or an external server)"]]]]]
        [input-text 
         :model @debug-url
         :on-change #(dispatch [:debug-url %])]]])))

(defn show-project-form-view
  []
  (let [show-project-form (subscribe [:show-project-form])]
    (fn
      []
      [h-box
       :gap "4px"
       :children [[label
                   :label "Display this window on startup?"]
                  [checkbox
                   :model @show-project-form
                   :on-change #(dispatch [:show-project-form %])]]])))

(defn session-details-view
  []
  [v-box
   :padding "20px 10px 0px 30px"
   :gap "10px"
   :height "100%"
   :children [[page-header "What Debug Session Do You Want To Launch?"]
              [gap :size "10px"]
              [project-form]
              [debug-url]
              [gap :size "20px"]
              [button
               :class "btn-success"
               :label "Debug"
               :on-click #(dispatch [:start-debugging])]
              [show-project-form-view]
              [gap :size "100%"]
              [:div (str "Atom Shell Version: " atom-shell-version)]]])

(defn main-page
  []
  (let [initialised (subscribe [:initialised])
        debugging? (subscribe [:debugging?])
        show-session-details (subscribe [:show-project-form])]
    (when @initialised
      (fn []
        (if @debugging?
          [debug-view]
          [session-details-view])))))

(defn start
  []
  (dispatch [:initialise])
  (keys/bind-keys)
  ; (dispatch [:start-debugging])
  (reagent/render [main-page] (get-element-by-id "app")))
