;; I contain code which checks if lein is installed on the user's machine.
;;
;; Notes:
;;   - The test is to run  "lein --version" and check the output
;;   - outcome is i"broadcast" via ipc on channel "lein-status-is"
;;     The value broadcast in this IPC send is the map in "lein-stats" below.
;;   - browser clients can also send the message "send-lein-status" to trigger 
;;     a re-broadcast
;;
;;

(ns main.lein-check
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [main.backend.macros :refer [<?]])
  (:require [main.backend.nrepl :as nrepl]))
;;   (:require [main.core :as core]))

(enable-console-print!)

(def ipc           (js/require "ipc"))
(def child-process (js/require "child_process"))

(.on ipc "get-lein-repl-status"
     (fn [event arg]
       (.send (.-sender event) "lein-repl-status" 
              (clj->js (:nrepl @nrepl/state)))))

;; a render client might ask for lein to start a repl
(.on ipc "start-lein-repl"
     (fn [event project-path]
       (when (not (:nrepl @nrepl/state))
         (go
           (let [port (<? (nrepl/start-lein-repl 
                            {:project-path project-path}))]
             (.send (.-sender event) "lein-repl-status" 
                    (clj->js (:nrepl @nrepl/state))))))))

;; a render client might ask for lein to stop a repl
(.on ipc "stop-lein-repl"
     (fn [event]
       (go
         (let [result (<? (nrepl/stop-lein-repl))]
           (.send (.-sender event) "lein-repl-status" 
                  (clj->js (:nrepl @nrepl/state)))))))

(.on ipc "translate-clojurescript"
     (fn [event statement namespace-string locals]
       (go
         (let [result (<? (nrepl/cljs->js 
                            statement 
                            :namespace-str namespace-string 
                            :locals locals))]
           (.send (.-sender event) "translated-javascript" result)))))