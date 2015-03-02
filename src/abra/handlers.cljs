(ns abra.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch]]
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

(register-handler 
  :start-debugging 
  start-debugging)

(defn stop-debugging 
  "Stop the lein repl and close the debugger view"
  [db _]
  (when (:lein-repl-status @db)
    (.send ipc "stop-lein-repl"))
  (.send ipc "close-url")
  (swap! db assoc :debugging? false))

(register-handler 
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
    (swap! db assoc :show-spinner true)
    (.send ipc "translate-clojurescript" 
           clojurescript-string 
           namespace-string 
           locals)))

(register-handler 
  :translate
  translate)

(register-handler 
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
        (swap! db assoc :show-spinner false)
        (.send ipc "get-lein-repl-status")))))

;; clear the scoped-locals dictionary
(defn clear-scoped-locals
  [db [_]]
  (swap! db assoc :scoped-locals {}))

(register-handler 
  :clear-scoped-locals
  clear-scoped-locals)

;; add a scoped local to the db
(register-handler 
  :add-scoped-local
  (fn [db [_ scope-id variable-map]]
    (let [locals (get (:scoped-locals @db) scope-id [])
          local-name (:name variable-map)
          value (:value variable-map)]
      (swap! db assoc-in [:scoped-locals scope-id] 
             (conj locals {:label local-name :id (count locals)
                           :value value})))))

;; clear the call-frames dictionary
(defn clear-call-frames
  [db [_]]
  (swap! db assoc :call-frames []))

(register-handler 
  :clear-call-frames
  clear-call-frames)

(register-handler
  :change-call-frame-id
  (fn [db [_ call-frame-id]]
    (swap! db assoc :call-frame-id call-frame-id)
    (clear-scoped-locals db [])
    (let [scope-objects (:scope-objects @db)]
      (doseq [{:keys [id objects]} scope-objects]
        (when (= id call-frame-id) 
          (doseq [o objects] 
            (print o id)
            (dispatch [:crmux.ws-getProperties o id])))))))

;; refresh the page to be debugged
(register-handler
  :refresh-page
  (fn [db _]
    (clear-scoped-locals db _)
    (clear-call-frames db _)
    (swap! db assoc :local-id 0)
    (.send ipc "refresh-page")))
