(ns abra.state
  (:require [reagent.core :as reagent]))


;; -- Main Application State ----------------------------------------------------------------------

(def app-state (reagent/atom {}))

(def default-state
  {:debugging?  false
   :project-dir "."
   :nrepl-state false
   :debug-url "http://www.day8.com.au"
   })


(defn initialise []
  (reset! app-state default-state))

;; -- State changing ----------------------------------------------------------------------
