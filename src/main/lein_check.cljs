;; I contain code which checks if lein is installed on the user's machine.
;;
;; Notes:
;;   - The test is to run  "lein --version" and check the output
;;   - outcome is i"broadcast" via ipc on channel "lein-status-is"
;;     The value broadcast in this IPC send is the map in "lein-stats" below.
;;   - browser clients can also send the message "send-lein-status" to trigger a re-broadcast
;;
;;

(ns main.lein-check
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [main.backend.macros :refer [<?]])
  (:require [main.backend.nrepl :as nrepl]))
;;   (:require [main.core :as core]))

(def ipc           (js/require "ipc"))
(def child-process (js/require "child_process"))


;; keeps current knowledge
(def lein-status (atom {:error false  :version-str nil}))

;; a render client might ask for lein status on this channel
(.on ipc "get-lein-status"
     (fn [event arg]
       (.send (.-sender event) "lein-status-is" true )))

(.on ipc "get-lein-repl-status"
     (fn [event arg]
       (.send (.-sender event) "lein-repl-status" (clj->js (:nrepl @nrepl/state)))))

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
     (fn [event statement]
       (go
         (let [result (<? (nrepl/cljs->js statement))]
           (.send (.-sender event) "translated-javascript" result)))))

(defn callback
  [error stdout stderr]
  (reset! lein-status   {:run true :error error :version-str stdout} )
  (if error (swap!  lein-status :error true))
  (if stderr (swap!  lein-status :error true))
  (if stdout (reset!  lein-status {:error false :version-str  ""}))
  #_(send-lein-status))



;; XXXX problem ... if I supply an error then nothing get through to the callback
(defn run
  []
  (.exec child-process "lein --version"  callback))
