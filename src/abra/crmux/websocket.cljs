(ns abra.crmux.websocket
  (:require-macros [cljs.core.async.macros :refer [go-loop, go]])
  (:require [cljs.core.async :refer [put!, chan, <!, >!, mult, pub, sub]]
            [cljs.reader :as reader]
            [re-frame.handlers :refer [register dispatch]]
            [re-frame.db :refer [app-db]]))

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
  #_(print "Result from javascript debugger: " message, js-result-in)
  (put! js-result-in message))

(defn get-object 
  [scope]
  (let [{{object-id :objectId class-name :className} :object} scope]
    (when (= class-name "Object")
      object-id)))

(defmethod handler :Debugger.paused
  [message]
  (let [{{call-frames :callFrames} :params} message
        call-frames-for-selection (vec (map #(hash-map 
                                               :id %1 
                                               :label 
                                               (if (empty? (:functionName %2))
                                                 "Anonymous"
                                                 (:functionName %2))
                                               :call-frame-id (:callFrameId %2)) 
                                            (range) call-frames))
        scope-chains (map :scopeChain call-frames)
        scope-objects (map #(hash-map :id %1 :objects %2) 
                           (range) 
                           (for [scope scope-chains]
                             (filter some? (map get-object scope))))]
    ;;(print "debugger.paused " message)
    ;;(print "call-frame-id" call-frame-id)
    (print "call-frames" call-frames)
    ;;(print "scope-chains" scope-chains)
    ;;(print "call-frames-for-selection" call-frames-for-selection)
    (print "scope-objects" scope-objects)
    (dispatch [:call-frames call-frames-for-selection])
    ;; clear the scoped-locals in the db
    (dispatch [:clear-scoped-locals])
    ;; add the locals for each call frame to the db
    (doseq [{:keys [id objects]} scope-objects]
      (print objects)
      (doseq [o objects] 
        (ws-getProperties app-db o id)))
    ))

(defmethod handler :default
  [message]
  #_(print (str "ignoring " (:method message))))

;; use a mult so that we can have multiple suscribers to the chan
(def js-result-pub (pub js-result-in :id))

(defn js-result-filter 
  "I watch the js-result channel and return when I find a message that matches 
  msg-id"
  [msg-id]
  (let [js-result (sub js-result-pub msg-id (chan))]
    (go
      (let [message (<! js-result)
            message-id (:id message)
            result (-> message :result :result)
            error-str (-> message :result :exceptionDetails :text)] 
        #_(print "js-result-filter" result, error-str)
        (if (some? result)
          result
          error-str)))))

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
  "evaluate javasript on the websocket and pop the result onto the database"
  [db expression]
  (let [msg-id (goog/getUid expression)
        call-frame-id (:call-frame-id @db)
        call-frames (:call-frames @db)
        call-frame (first (filter #(= call-frame-id (:id %)) call-frames))
        call-frame-debugger-id (:call-frame-id call-frame)
        message (clj->js {"method" "Debugger.evaluateOnCallFrame" "id" msg-id "params" 
                          {"expression" expression "returnByValue" true
                           "callFrameId" call-frame-debugger-id}})
        result (js-result-filter msg-id)]
    (ws-send db message)
    (go (let [result (<! result)
              value (:value result)]
          (swap! db assoc :js-print-string 
                 (reader/read-string value))))))


(defn ws-getProperties 
  "get the properties from the websocket"
  [db object-id call-frame-id]
  (let [msg-id  (goog/getUid object-id)
        message (clj->js {"method" "Runtime.getProperties" "id" msg-id "params" 
                          {"objectId" object-id 
                           "ownProperties" false 
                           "accessorPropertiesOnly" false}})
        result  (js-result-filter msg-id)]
    (ws-send db message)
    (go 
      (let [result (<! result)
            [{local-name :name}] result]
        #_(print "ws-getProperties" call-frame-id local-name)
        (when local-name 
          (dispatch [:add-scoped-local call-frame-id local-name]))))))