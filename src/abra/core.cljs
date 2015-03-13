(ns abra.core
  (:require [reagent.core :as reagent]
            [abra.dialog :as dialog]
            [abra.state :as state]    ;; although not used, leave it in here, or else the subscriptions don't get pulled in.
            [re-com.core :refer [input-text input-textarea label title spinner]]
            [re-com.buttons :refer [button info-button]]
            [re-com.box   :refer [h-box v-box box scroller gap line]]
            [re-com.tabs :refer [vertical-bar-tabs]]
            [re-com.layout :refer [v-layout]]
            [re-com.modal :refer [modal-panel]]
            [cljs.core.async :refer [<!]]
            [re-frame.core :refer [dispatch]]
            [re-frame.subs :refer [subscribe]]
            [abra.handlers]
            [abra.keys :as keys]
            [figwheel.client :as fw]))

;; redirects any println to console.log
(enable-console-print!)

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

(def ipc (js/require "ipc"))

;; equivalent of:  process.versions['atom-shell']
(def atom-shell-version (aget (.-versions js/process) "atom-shell"))

;; Make sure that lein exists on this machine.
;; XXX what if they use boot instead?
(.send ipc "get-lein-repl-status")
(.on ipc "lein-repl-status" 
     (fn [arg]
       (dispatch [:lein-repl-status (js->clj arg)])
       (dispatch [:disabled (not (js->clj arg))])))

;;------------------------------------------------------------------------------

(.on ipc "translated-javascript" 
     (fn [err js-expression]
       (dispatch [:translated-javascript err js-expression]))) 

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

(defn namespace-locals
  []
  (let [namespace-string (subscribe [:namespace-string])
        locals (subscribe [:scoped-locals])
        call-frames (subscribe [:call-frames])
        call-frame-id (subscribe [:call-frame-id])
        local-id (subscribe [:local-id])
        disabled (subscribe [:disabled])]
    (fn
      []
      [h-box
       :justify :start
       :gap "20px"
       :children (concat  
                   [[v-box
                     :children 
                     [[field-label "namespace" 
                       "enter the namespace of the file inspected"]
                      [input-textarea
                       :model @namespace-string
                       :on-change #(dispatch [:namespace-string %])
                       :rows "5"
                       :width "300px"]]]]
                   (when (and (not @disabled) @call-frame-id) 
                     (when-let [locals-tab (get @locals @call-frame-id)]
                       (let [[local-map] (filter #(= (:id %) @local-id) 
                                                 locals-tab)]
                         [[v-box
                           :children 
                           [[field-label "call-frames" "the active call frames"]
                            [scroller
                             :h-scroll :off
                             :height "125px"
                             :child [vertical-bar-tabs
                                     :model @call-frame-id
                                     :tabs @call-frames
                                     :on-change 
                                     (fn [id]
                                       (dispatch [:change-call-frame-id id])
                                       (dispatch [:local-id 0]))]]]]  
                          [v-box
                           :children 
                           [[field-label "locals"]
                            [scroller
                             :h-scroll :off
                             :height "125px"
                             :child [vertical-bar-tabs
                                     :model @local-id
                                     :tabs locals-tab
                                     :on-change #(dispatch [:local-id %])]]]]
                          [v-box
                           :children 
                           [[field-label "local value"]
                            [input-textarea
                             :model (print-str (:value local-map))
                               :on-change #()]]]]))))])))

(defn clojurescript-input-output
  []
  (let [lein-repl-status (subscribe [:lein-repl-status])
        clojurescript-string (subscribe [:clojurescript-string])
        javascript-string (subscribe [:javascript-string])
        js-print-string (subscribe [:js-print-string])
        show-spinner (subscribe [:show-spinner])]
    (fn
      []
      (let [elements [[v-box
                       :children [[field-label "clojurescript"]
                                  [input-textarea
                                   :model @clojurescript-string
                                   :on-change #(dispatch 
                                                 [:clojurescript-string %])]]]
                      [v-box 
                       :children [[gap
                                   :size "20px"]
                                  [button
                                   :label "Translate"
                                   :class "btn-primary"
                                   :on-click #(dispatch [:translate])
                                   :disabled? (not @lein-repl-status)]]]]
            result-elements (if @show-spinner 
                              [[v-box 
                                :children [[gap
                                            :size "20px"]
                                           [spinner]]]]
                              (if @javascript-string 
                                [[v-box
                                  :children [[field-label "result"]
                                             [input-textarea
                                              :model @js-print-string
                                              :on-change #()]]]
                                 [v-box
                                  :children [[field-label "javascript"]
                                             [input-textarea
                                              :model @javascript-string
                                              :on-change #()]]]]
                                []))]
        [h-box
         :gap "5px"
         :children (concat elements result-elements)]))))

(defn abra-debug-panel []
  (let [debug-crmux-url (subscribe [:debug-crmux-url])]
    (fn []
      [h-box 
       :size "auto"
       :children [[:iframe.debug-iframe {:src @debug-crmux-url}]]])))

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
                                   :disabled? true]
                                  [gap 
                                   :size "40px"]
                                  [nrepl-state-text]]]
                      [namespace-locals]
                      [clojurescript-input-output]]]])

(defn debug-view
  []
  (let [disabled (subscribe [:disabled])]
    (fn
      []
      [v-box
       :height "100%"
       :children [[v-layout
                   :initial-split "65%"
                   :top-panel top-debug-panel
                   :bottom-panel abra-debug-panel]
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
          [v-box :children 
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
              [gap :size "100%"]
              [:div (str "Atom Shell Version: " atom-shell-version)]]])

(defn main-page
  []
  (let [initialised (subscribe [:initialised])]
    (when @initialised 
      (let [debugging? (subscribe [:debugging?])]
        (fn [] 
          (if @debugging?
            [debug-view]
            [session-details-view]))))))

(defn start
  []
  (dispatch [:initialise])
  (keys/bind-keys)
  ; (dispatch [:start-debugging])
  (reagent/render [main-page] (get-element-by-id "app")))
