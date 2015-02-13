(ns abra.handlers
  (:require [re-frame.handlers :refer [register]]
            [re-frame.subs :refer [subscribe]]
            [abra.crmux.handlers :as crmux-handlers]
            [abra.crmux.websocket :as crmux-websocket]))

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
  (.send ipc "close-url")
  (swap! db assoc :debugging? false))

(register 
  :stop-debugging
  stop-debugging)

(defn translate 
  "translates the clojurescript on this page"
  [db _]
  (let [clojurescript-string (:clojurescript-string @db)
        namespace-string (:namespace-string @db)
        call-frame-id (:call-frame-id @db)
        locals-map (get-in @db [:scoped-locals call-frame-id])
        locals (clj->js (map :label locals-map))]
    (.send ipc "translate-clojurescript" 
           clojurescript-string 
           namespace-string 
           locals)))

(register 
  :translate
  translate)

(register 
  :translated-javascript
  (fn [db [_ err js-expression]]
    (if err 
      (do
        (swap! db assoc :javascript-string (print-str err))
        (swap! db assoc :js-print-string ""))
      (let [js-print-string (str "cljs.core.prn_str.call(null,"
                                 (clojure.string/join 
                                   (drop-last js-expression)) ");")]
        (crmux-websocket/ws-evaluate db js-print-string)
        (swap! db assoc :javascript-string js-expression)
        (.send ipc "get-lein-repl-status")))))

;; clear the scoped-locals dictionary
(register 
  :clear-scoped-locals
  (fn [db [_]]
    (swap! db assoc :scoped-locals {})))

;; add a scoped local to the db
(register 
  :add-scoped-local
  (fn [db [_ scope-id local-name]]
    (let [locals (get (:scoped-locals @db) scope-id [])]
      (swap! db assoc-in [:scoped-locals scope-id] 
             (conj locals {:label local-name :id (count locals)})))))

;; clear the call-frames dictionary
(register 
  :clear-call-frames
  (fn [db [_]]
    (swap! db assoc :call-frames [])))
