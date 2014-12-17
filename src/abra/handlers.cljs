(ns abra.handlers
  (:require [re-frame.handlers :refer [register]]
            [re-frame.subs :refer [subscribe]]
            [abra.crmux-handlers :as crmux-handlers]))

;; redirects any println to console.log
(enable-console-print!)

(def ipc (js/require "ipc"))

(defn start-debugging 
  "Start the lein repl and open the debugger view"
  [db _]
  (let [url (:debug-url @db)
        debug-host (:debug-host @db)]
    (.send ipc "open-url" url)
    (.send ipc "start-lein-repl" (:project-dir @db))
    (crmux-handlers/get-debug-window-info debug-host url)
    (swap! db assoc :debugging? true)))

(register 
  :start-debugging 
  start-debugging)

(defn stop-debugging 
  "Stop the lein repl and close the debugger view"
  [db _]
  (when (:lein-repl-status @db)
    (.send ipc "stop-lein-repl"))
  (swap! db assoc :debugging? false))

(register 
  :stop-debugging
  stop-debugging)

(defn translate 
  "translates the clojurescript on this page"
  [db _]
  (let [clojurescript-string (:clojurescript-string @db)
        namespace-string (:namespace-string @db)
        locals (.split (:locals-string @db) #"\s")]
    (.send ipc "translate-clojurescript" 
           clojurescript-string 
           namespace-string 
           locals)))

(register 
  :translate
  translate)
