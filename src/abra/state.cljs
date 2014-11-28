(ns abra.state
  (:require [reagent.core :as reagent]))


;; -- Main Application State ----------------------------------------------------------------------

(def app-state (reagent/atom {}))

(def default-state
  {:debugging?  false
   :project-dir "."
   :nrepl-state false
   :debug-url "http://www.day8.com.au"
   :namespace-string ""
   :locals-string ""
   :clojurescript-string "(+ 2 3)"
   :javascript-string ""})


(defn initialise []
  (reset! app-state default-state))

;; -- State changing ----------------------------------------------------------------------
