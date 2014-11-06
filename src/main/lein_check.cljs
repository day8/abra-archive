;; I contain code which checks if lein is installed on the user's machine.
;;
;; Notes:
;;   - The test is to run  "lein --version" and check the output
;;   - outcome is i"broadcast" via ipc on channel "lein-status-is"
;;     The value broadcast in this IPC send is the map in "lein-stats" below.
;;   - browser clients can also send the message "lein-status-is" to trigger a re-broadcast
;;
;;

(ns main.lein-check)
;;   (:require [main.core :as core]))

(def ipc           (js/require "ipc"))
(def child-process (js/require "child_process"))


;; keeps current knowledge
(def lein-status (atom {:error false  :version-str nil}))


;; The problem is that ipc doesn't have a method called send ....
;; The only way for browser to send is to use the
(defn send-lein-status
  []
  "I broadcast current status on an ipc channe, presumeably to listening render clients"
  (.send ipc "lein-status-is" (clj->js @lein-status)))

;; a render client might ask for lein status on this channel
(.on ipc "send-lein-status"
     (fn [event arg]
       (.send (.-sender event) "lein-status-is" XXXX )))

(defn callback
  [error stdout stderr]
  (reset! lein-status   {:run true :error error :version-str stdout} )
  (if error (swap!  lein-status :error true))
  (if stderr (swap!  lein-status :error true))
  (if stdout (reset!  lein-status {:error false :version-str  ""}))

  (println "stdout" stdout)
  (println "stderr" stderr)
  #_(send-lein-status))



;; XXXX problem ... if I supply an error then nothing get through to the callback
(defn run
  []
  (.exec child-process "lein --version"  callback))