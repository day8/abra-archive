(ns abra.core
  (:require [abra.state :as state]
            [reagent.core :as reagent]
            [reagent.ratom :as ratom]
            [abra.dialog :as dialog]
            [re-com.core :refer [input-text input-textarea 
                                 label title]]
            [re-com.buttons :refer [button info-button]]
            [re-com.box   :refer [h-box v-box box scroller gap line]]
            [re-com.tabs :refer [vertical-bar-tabs]]
            [re-com.layout :refer [v-layout]]
            [cljs.core.async :refer [<!]]
            [re-frame.handlers :refer [dispatch]]
            [re-frame.subs :refer [subscribe]]
            [abra.handlers]
            [figwheel.client :as fw]))

;; redirects any println to console.log
(enable-console-print!)

#_(fw/start {
             ;; configure a websocket url if yor are using your own server
             :websocket-url "ws://localhost:3449/figwheel-ws"
             
             ;; optional callback
             :on-jsload (fn [] 
                          (println (reagent/force-update-all)))
             
             ;; when the compiler emits warnings figwheel
             ;; blocks the loading of files.
             ;; To disable this behavior:
             :load-warninged-code true
             })

(def ipc (js/require "ipc"))

;; equivalent of:  process.versions['atom-shell']
(def atom-shell-version (aget (.-versions js/process) "atom-shell"))

(.send ipc "get-lein-repl-status")                       ;; ask for the status
(.on ipc "lein-repl-status" 
     (fn [arg]
       (dispatch [:lein-repl-status (js->clj arg)]))) 

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
                    [v-box :children [[:div nil "Abra"]
                                      [:div (str "Atom Shell Version: " 
                                                 atom-shell-version)]]]]])

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
        local-id (ratom/atom 0)]
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
                       :rows 5
                       :width "300px"]]]]
                   (when @call-frame-id 
                     (when-let [locals-tab (get @locals @call-frame-id)]
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
                                   #(dispatch [:call-frame-id %])]]]]  
                        [v-box
                         :children 
                         [[field-label "locals"]
                          [scroller
                           :h-scroll :off
                           :height "125px"
                           :child [vertical-bar-tabs
                                   :model @local-id
                                   :tabs locals-tab
                                   :on-change #(swap! local-id %)]]]]
                        [v-box
                         :children 
                         [[field-label "local value"]
                          [input-textarea
                           :model "hello"]]]])))])))

(defn clojurescript-input-output
  []
  (let [lein-repl-status (subscribe [:lein-repl-status])
        clojurescript-string (subscribe [:clojurescript-string])
        javascript-string (subscribe [:javascript-string])
        js-print-string (subscribe [:js-print-string])]
    (fn
      []
      [h-box
       :gap "5px"
       :children [[v-box
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
                               :disabled? (not @lein-repl-status)]]]
                  [v-box
                   :children [[field-label "result"]
                              [input-textarea
                               :model @js-print-string]]]
                  [v-box
                   :children [[field-label "javascript"]
                              [input-textarea
                               :model @javascript-string]]]]])))

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
                                   :label "refresh"]
                                  [gap 
                                   :size "40px"]
                                  [nrepl-state-text]]]
                      [namespace-locals]
                      [clojurescript-input-output]]]])

(defn debug-view
  []
  [v-box
   :height "100%"
   :children [
              [v-layout
               :initial-split "65%"
               :top-panel top-debug-panel
               :bottom-panel abra-debug-panel]]])

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
           "This directory is the root of your clojurescript project"]
          [v-box :children 
           [[h-box
             :gap "2px"
             :children 
             [[input-text 
               :model @project-dir]
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
          :children [[:div "You want to debug an HTML page right? 
                           Via which URL should it be loaded? "]
                     [:div "Probably something like:"]
                     [:ul [:li "file:///path/index.html"]
                      [:li (str "http://localhost:3449/index.html "
                                "(if you are running figwheel or "
                                "an external server)")]]]]]
        [input-text 
         :model debug-url
         :on-change #(dispatch [:debug-url %])]]])))

(defn details-view
  []
  [v-box
   :padding "20px 10px 0px 30px"
   :gap "10px"
   :children [
              [page-header "What Debug Session Do You Want To Launch?"]
              [project-form]
              [debug-url]   
              [button
               :class "btn-success"
               :label "Debug"
               :on-click #(dispatch [:start-debugging])]]])

(defn main-page
  []
  (let [debugging? (subscribe [:debugging?])]
    (fn [] 
      (if @debugging?
        [debug-view]
        [details-view]))))

(defn start
  []
  (dispatch [:initialise])
  ; (dispatch [:start-debugging])
  (reagent/render [main-page] (get-element-by-id "app")))
