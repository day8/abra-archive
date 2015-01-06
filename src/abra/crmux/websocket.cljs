(ns abra.crmux.websocket
  (:require-macros [cljs.core.async.macros :refer [go-loop, go]])
  (:require [cljs.core.async :refer [put!, chan, <!, >!, mult, tap, untap]]
            [cljs.reader :as reader]
            [re-frame.handlers :refer [register dispatch]]))

;; redirects any println to console.log
(enable-console-print!)

;; -- Notification Handlers -----------------------------------------------------------------------
;;
;; JSON messages flow down the websocket from the debugger (VM).
;; Each arriving message has a "method" and some "parameters".
;; A multimethod is used to handle the different "methods"
;;

(defn extract-method-name
  "I return the method name for a chrome-Remote-debugger-protocol JSON message"
  [message]
  (let [method (:method message)
        msg-id (:id message)]
    (if (some? msg-id)
      :result
      (keyword (:method message)))))

(defmulti handler extract-method-name)

;; pipe results so they can be inspected
(def js-result-in (chan))

(defmethod handler :result
  ;; "grab a result and put it in a channel"
  [message]
  (print "Result from javasript debugger: " message)
  (put! js-result-in message))

(defmethod handler :Debugger.paused
  [message]
  (let [call-frames (get-in message [:params :callFrames])]
    (print "debugger.paused " message)
    (dispatch [:call-frames call-frames])))

(defmethod handler :default
  [message]
  #_(print (str "ignoring " (:method message))))

;; use a mult so that we can have multiple suscribers to the chan
(def js-result-mult (mult js-result-in))

(defn js-result-filter 
  "I watch the js-result channel and return when I find a message that matches 
  msg-id"
  [msg-id]
  (let [js-result (tap js-result-mult (chan))]
    (go-loop []
             (let [message (<! js-result)
                   message-id (:id message)
                   result-str (-> message :result :result :value)
                   error-str (-> message :result :exceptionDetails :text)]
               (if (= msg-id message-id)
                 (do 
                   (untap js-result-mult js-result)
                   (if (some? result-str)
                     (reader/read-string result-str)
                     error-str))
                 (recur))))))

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
  "remove the handle in the db (shoudl it attempt to reconnect?)"
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
  (let [ws (:debug-crmux-websocket @db)]
    (.send ws (.stringify js/JSON message))))   ;; XXX turn it into str

(defn ws-evaluate 
  "evaluate javasript on the websocket and pop the result onto the database"
  [db expression]
  (let [msg-id (goog/getUid expression)
        message (clj->js {"method" "Runtime.evaluate" "id" msg-id "params" 
                          {"expression" expression "returnByValue" true}})
        result (js-result-filter msg-id)]
    (ws-send db message)
    (go (swap! db assoc :js-print-string (<! result)))))