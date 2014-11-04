(ns abra.core
  (:require [abra.state :as state]
            [reagent.core :as reagent]
            [abra.dialog :as dialog]))


(def remote (js/require "remote"))
(def ipc (js/require "ipc"))


;; equivalent of:  process.versions['atom-shell']
(def atom-shell-version (aget (.-versions js/process) "atom-shell"))

;;-- lein Status ------------------------------------------------------------------------------------------------------
;; Listen for problems with lein

(def lein-status (reagent/atom {:error false}))                   ;; true is good, false is bad
(.send ipc "send-lein-status")                                    ;; ask for the status
(.on ipc "lein-status-is" #(reset! lein-status  (js->clj %1)))    ;;

(defn tell-user-about-lein-problems
  []
  [:div "It doesn't look as if you have lein installed on this machine"])


;;--------------------------------------------------------------------------------------------------------------------

(defn get-element-by-id
  [id]
  (.getElementById js/document id))



(defn debug-view
  []
  [:div nil "Debug-View"] )


(defn project-cljs
  []
  )


(defn project-form
  []
  [:div
    ])



(defn details-view
  []
  [:div
   [:div nil "Abra"]
   (when (:error @lein-status) [tell-user-about-lein-problems])
   [:div (str "Atom Shell Version: " atom-shell-version)]
   [:input.btn.btn-success
    {:type "button"
     :value "open"
     ;; :disabled (if (stadebugging? @state/app-state) "disabled" "")
     :on-click  #(dialog/open {:title "Open Project.clj File" :properties ['openDirectory' :defaultPath  "c:\\"] })}]

   [:input.btn.btn-success
    {:type "button"
     :value "Debug"
     ;; :disabled (if (stadebugging? @state/app-state) "disabled" "")
     :on-click  #(swap! state/app-state assoc :debugging? true)}]

   [:input.btn.btn-success
    {:type "button"
     :value "Message"
     ;; :disabled (if (stadebugging? @state/app-state) "disabled" "")
     :on-click  #(dialog/message {:type "info" :message "Didn't work" :buttons ["Cancel" "Ok"]})}]])

;;


(defn main-page
  []
  [:div
   (if (:debugging? @state/app-state)
     [debug-view]
     [details-view]
    )])


(defn start
  []
  (state/initialise)
  (reagent/render-component [main-page] (get-element-by-id "app")))
