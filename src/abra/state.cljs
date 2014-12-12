(ns abra.state
  (:require [reagent.core :as reagent]))


;; -- Main Application State ----------------------------------------------------------------------

(def app-state (reagent/atom {}))

(def default-state
  {:debugging?  false
   :project-dir "."
   :nrepl-state false
   :debug-url "http://www.day8.com.au"
   :debug-host "http://localhost:9223"
   :namespace-string "(ns abra.core
  (:require [abra.state :as state]
            [reagent.core :as reagent]
            [abra.dialog :as dialog]
            [re-com.core  :refer [input-text input-textarea button hyperlink label 
                                  spinner progress-bar checkbox radio-button 
                                  title slider]]
            [re-com.box   :refer [h-box v-box box gap line]]))"
   :locals-string "x\ny\nz"
   :clojurescript-string "(+ x state/y input-text)"
   :javascript-string ""
   :debug-crmux-url nil})


(defn initialise []
  (reset! app-state default-state))

;; -- State changing ----------------------------------------------------------------------
