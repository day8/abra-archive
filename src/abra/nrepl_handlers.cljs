(ns abra.nrepl-handlers
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [abra.backend.macros :refer [<?]])
  (:require [re-frame.core :refer [register-handler
                                   dispatch
                                   path]]
            [abra.crmux.handlers :as crmux-handlers]
            [abra.crmux.websocket :refer [ws-evaluate]]
            [clojure.string :refer [replace]]
            [abra.backend.nrepl :as nrepl]))

(def ipc (js/require "ipc"))

(defn start-lein-repl
  [project-path]
  (when (not (:nrepl @nrepl/state))
    (go
      (let [port (<? (nrepl/start-lein-repl 
                       {:project-path project-path}))
            open? (:nrepl @nrepl/state)]
        (dispatch [:lein-repl-status open?])
        (dispatch [:disabled (not open?)])))))

(defn start-debugging 
  "Start the lein repl and open the debugger view"
  [db _]
  (let [url (:debug-url db)
        debug-host (:debug-host db)]
    (.send ipc "open-url" url)
    (start-lein-repl (:project-dir db))
    (crmux-handlers/get-debug-window-info debug-host url)
    (assoc db :debugging? true)))

(register-handler 
  :start-debugging
  start-debugging)

(defn stop-debugging 
  "Stop the lein repl and close the debugger view"
  [db _]
  (when (:lein-repl-status db)
    ;; ask for lein to stop a repl
    (go
      (let [result (<? (nrepl/stop-lein-repl))
            open? (:nrepl @nrepl/state)]
        (dispatch [:lein-repl-status open?]))))
  (.send ipc "close-url")
  (assoc db :debugging? false))

(register-handler 
  :stop-debugging
  stop-debugging)


(defn translate 
  "translates the clojurescript on this page"
  [db _]
  (let [statement (:clojurescript-string db)
        namespace-string (:namespace-string db)
        call-frame-id (:call-frame-id db)
        locals-map (get-in db [:scoped-locals call-frame-id])
        locals (clj->js (keys locals-map))
        command-history (:command-history db)]
    (go
      (try 
        (let [result (<? (nrepl/cljs->js 
                           statement 
                           :namespace-str namespace-string 
                           :locals locals))]
          (dispatch [:translated-javascript nil result]))
        (catch js/Error e
          (dispatch [:translated-javascript "Clojurescript error" nil]) 
          e)))
    (dispatch [:command-history (vec (take-last 
                                       15 
                                       (conj command-history statement)))])
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
                                     (drop-last js-expression)) ");")
              open? (:nrepl @nrepl/state)]
          (ws-evaluate db js-print-string call-frame-id 
                       #(dispatch [:js-print-string %]))
          (dispatch [:lein-repl-status open?])
          (-> db
              (assoc :javascript-string js-expression)
              (assoc :show-spinner false)))))))
