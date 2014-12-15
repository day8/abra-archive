(ns main.state)


;; -- Main Application State ----------------------------------------------------------------------

(def app-state (atom {}))

(def default-state
  {:debug-port "9222"
   :debug-host "http://localhost:"})


(defn initialise []
  (reset! app-state default-state))

;; -- State changing ----------------------------------------------------------------------
