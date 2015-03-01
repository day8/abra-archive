(ns abra.crmux.websocket
  (:require-macros [cljs.core.async.macros :refer [go-loop, go]])
  (:require [cljs.core.async :refer [put!, chan, <!, >!, mult, pub, sub]]
            [cljs.reader :as reader]
            [re-frame.handlers :refer [register dispatch]]
            [re-frame.db :refer [app-db]]
            [abra.crmux.debug-handlers :refer [handler js-result-filter]]))

;; redirects any println to console.log
(enable-console-print!)

;; -- Web Socket ----------------------------------------------------------------------------------
;;
;; The following web-socket is attached to the target page's debugger (VM).
;; It acts as the two-way transport for 'messages':
;;    - instructing the debugger  (eg: please evaluate expression X in the current callframe)
;;    - recieving notifications from the VM  (eg: now paused at a breakpoint)
;; Arriving messages are routed to "handlers" which, in turn, update panel state.
;;
;; Protocol references:
;;   - https://developer.chrome.com/devtools/docs/protocol/1.1/debugger
;;   - https://developer.chrome.com/devtools/docs/debugger-protocol
;;
;; My tools:
;;    - format json:  http://jsonlint.com/
;;    - write markdown docs:  http://dillinger.io/
;;

(defn on-ws-message
  [message]
  (let [data (.-data message)
        message-as-json (.parse js/JSON data)
        message-as-edn (js->clj message-as-json :keywordize-keys true)]
    (handler message-as-edn)))


(defn on-ws-error
  [error]
  (print (str "ws error:" error)))

(defn on-ws-close
  "remove the handle in the db (should it attempt to reconnect?)"
  [db]
  (swap! db assoc :debug-crmux-websocket nil))

(defn create-websocket
  "creates a websocket connection and adds it to the db"
  [db url]
  (let [ws (:debug-crmux-websocket @db)]
    (print "create-websocket")
    (when (some? ws)
      (.close ws))
    (if (nil? url)
      (swap! db assoc :debug-crmux-websocket nil)
      (let [new-ws (js/WebSocket. url)]
        (swap! db assoc :debug-crmux-websocket new-ws)
        (aset new-ws "onmessage" on-ws-message)
        (aset new-ws "onerror" on-ws-error)
        (aset new-ws "onclose" #(on-ws-error db))))))

(defn ws-send
  "sends a message to the websocket" 
  [db message]
  (when-let [ws (:debug-crmux-websocket @db)]
    (.send ws (.stringify js/JSON message))))   ;; XXX turn it into str

(defn ws-evaluate 
  "evaluate javascript on the websocket and pop the result onto the database"
  [db expression]
  (let [msg-id (goog/getUid expression)
        call-frame-id (:call-frame-id @db)
        call-frames (:call-frames @db)
        call-frame (first (filter #(= call-frame-id (:id %)) call-frames))
        call-frame-debugger-id (:call-frame-id call-frame)
        debugger-method (if call-frame-debugger-id 
                          "Debugger.evaluateOnCallFrame"
                          "Runtime.evaluate")
        message (clj->js {"method" debugger-method "id" msg-id "params" 
                          {"expression" expression "returnByValue" true
                           "callFrameId" call-frame-debugger-id}})
        result (js-result-filter msg-id)]
    (ws-send db message)
    (go (let [result (<! result)
              value (:value result)
              value (if value
                      value
                      (:description result))]
          (swap! db assoc :js-print-string value)))))


(defn ws-getProperties 
  "get the properties from the websocket"
  [db [_ object-id call-frame-id]]
  (let [msg-id  (goog/getUid object-id)
        message (clj->js {"method" "Runtime.getProperties" "id" msg-id "params" 
                          {"objectId" object-id 
                           "ownProperties" false 
                           "accessorPropertiesOnly" false}})
        result  (js-result-filter msg-id)]
    (ws-send db message)
    (go 
      (let [result (<! result)]
        (doseq [variable-map result] 
          (dispatch [:add-scoped-local call-frame-id variable-map]))))))

(register :crmux.ws-getProperties ws-getProperties)