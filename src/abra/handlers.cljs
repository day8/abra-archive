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
        locals (clj->js (keys locals-map))]
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
    (let [call-frame-id (:call-frame-id db)
          call-frames (:call-frames db)
          call-frame (first (filter #(= call-frame-id (:id %)) call-frames))
          call-frame-id (:call-frame-id call-frame)]
      (if err 
        (-> db
            (assoc :javascript-string (print-str err))
            (assoc :js-print-string ""))
        (let [js-print-string (str "cljs.core.prn_str.call(null,"
                                   (clojure.string/join 
                                     (drop-last js-expression)) ");")]
          (crmux-websocket/ws-evaluate db js-print-string call-frame-id 
                                       #(dispatch [:js-print-string %]))
          (.send ipc "get-lein-repl-status")
          (-> db
              (assoc :javascript-string js-expression)
              (assoc :show-spinner false)))))))

;; clear the scoped-locals dictionary
(defn clear-scoped-locals
  [scoped-locals [_]]
  {})

(register-handler 
  :clear-scoped-locals
  (path :scoped-locals)
  clear-scoped-locals)

;; add a scoped local to the db
(register-handler 
  :add-scoped-local
  (path [:scoped-locals])
  (fn [scoped-locals [_ scope-id variable-map]]
    (let [locals (scoped-locals scope-id {})
          local-name (:name variable-map)
          value (:value variable-map)]
      (assoc scoped-locals scope-id 
                (assoc locals local-name {:label local-name :id (count locals)
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
