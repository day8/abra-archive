(ns abra.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch
                                   path]]
            [abra.crmux.handlers :as crmux-handlers]
            [abra.crmux.websocket :as crmux-websocket]))

;; redirects any println to console.log
(enable-console-print!)

(def ipc (js/require "ipc"))

(defn start-debugging 
  "Start the lein repl and open the debugger view"
  [db _]
  (let [url (:debug-url db)
        debug-host (:debug-host db)]
    (.send ipc "open-url" url)
    (.send ipc "start-lein-repl" (:project-dir db))
    (crmux-handlers/get-debug-window-info debug-host url)
    (assoc db :debugging? true)))

(register-handler 
  :start-debugging
  start-debugging)

(defn stop-debugging 
  "Stop the lein repl and close the debugger view"
  [db _]
  (when (:lein-repl-status db)
    (.send ipc "stop-lein-repl"))
  (.send ipc "close-url")
  (assoc db :debugging? false))

(register-handler 
  :stop-debugging
  stop-debugging)

(defn translate 
  "translates the clojurescript on this page"
  [db _]
  (let [clojurescript-string (:clojurescript-string db)
        namespace-string (:namespace-string db)
        call-frame-id (:call-frame-id db)
        locals-map (get-in db [:scoped-locals call-frame-id])
        locals (clj->js (map :label locals-map))]
    (.send ipc "translate-clojurescript" 
           clojurescript-string 
           namespace-string 
           locals)
    (assoc db :show-spinner true)))

(register-handler 
  :translate
  translate)

(register-handler 
  :translated-javascript
  (fn [db [_ err js-expression]]
    (if err 
      (-> db
          (assoc :javascript-string (print-str err))
          (assoc :js-print-string ""))
      (let [js-print-string (str "cljs.core.prn_str.call(null,"
                                 (clojure.string/join 
                                   (drop-last js-expression)) ");")]
        (crmux-websocket/evaluate-js-string db js-print-string)
        (.send ipc "get-lein-repl-status")
        (-> db
            (assoc :javascript-string js-expression)
            (assoc :show-spinner false))))))

;; clear the scoped-locals dictionary
(defn clear-scoped-locals
  [scoped-locals [_]]
  {})

(register-handler 
  :clear-scoped-locals
  (path :scoped-locals)
  clear-scoped-locals)

;; find out the value of a variable in the debugger
(register-handler
  :find-variable-val
  (fn [db [_ local-name local-id scope-id]]
    (crmux-websocket/evaluate-variable-val db local-name local-id scope-id)
    db))

(register-handler
  :add-local-value
  (fn [db [_ local-name local-id scope-id value]]
    (let [locals ((:scoped-locals db) scope-id)
          local-map {:label local-name
                     :id local-id
                     :value value}
          new-locals (map (fn [old-local]
                            (if (= (:label old-local) local-name)
                              local-map
                              old-local))
                          locals)]
      (assoc-in db [:scoped-locals scope-id] new-locals))))

;; add a scoped local to the db
(register-handler 
  :add-scoped-local
  (fn [db [_ scope-id variable-map]]
    (let [locals (get (:scoped-locals db) scope-id [])
          local-name (:name variable-map)
          local-id (count locals)
          value (:value variable-map)]
      (dispatch [:find-variable-val local-name local-id scope-id])
      (assoc-in db [:scoped-locals scope-id] 
                (conj locals {:label local-name :id local-id
                              :value value})))))

;; clear the call-frames dictionary
(defn clear-call-frames
  [call-frames [_]]
  [])

(register-handler 
  :clear-call-frames
  (path :call-frames)
  clear-call-frames)

(register-handler
  :change-call-frame-id
  (fn [db [_ call-frame-id]]
    (let [scope-objects (:scope-objects db)]
      (doseq [{:keys [id objects]} scope-objects]
        (when (= id call-frame-id) 
          (doseq [o objects] 
            (print o id)
            (dispatch [:crmux.ws-getProperties o id])))))
    (-> db
        (assoc :scoped-locals {})
        (assoc :call-frame-id call-frame-id))))

;; refresh the page to be debugged
(register-handler
  :refresh-page
  (fn [db _]
    (.send ipc "refresh-page")
    (-> db    
      (assoc :call-frames [])
      (assoc :scoped-locals {})
      (assoc :local-id 0))))
