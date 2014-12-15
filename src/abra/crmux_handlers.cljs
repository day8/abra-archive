(ns abra.crmux-handlers
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [main.backend.macros :refer [<?]])
  (:require [clojure.string :as string]
            [ajax.core :refer [GET POST]]
            [abra.state :as state]
            [goog]
            [cljs.core.async :refer [put!, chan, <!, >!, mult, tap, untap]]
            [cljs.reader :as reader]))

;; redirects any println to console.log
(enable-console-print!)

(defn clean-url [url]
  "Removes leading/trailing spaces then removes trailing slash if it exists"
  
  (string/replace (string/trim url) #"/$" ""))

(defn get-debug-page-details [tabs url]
  "Takes a vector of maps (from http://localhost:9223/json) and selects the map
  which contains the data for the requested url"
  
  (let [matches-url #(= (clean-url (% "url")) url)
        matches (filter matches-url tabs)
        matchcount (count matches)]
    
    (.log js/console (str "get-debug-page-details found matches: " matchcount))
    #_(assert (= matchcount 1) (str "Expected ONE window to debug but found " matchcount))
    (if (not= matchcount 1) (js/alert (str "Expected ONE window to debug but found " matchcount)))
    (first matches)))


(defn get-debuggable-windows-handler [host url]
  (fn [response]
    (let [url (clean-url url)
          window-to-debug (get-debug-page-details response url)
          dev-front-end-full (str host (window-to-debug "devtoolsFrontendUrl"))
          websocket-url (window-to-debug "webSocketDebuggerUrl")]
      (.log js/console  (str "webSocketDebuggerUrl=" websocket-url ", devtoolsFrontendUrl=" dev-front-end-full))
      (swap! state/app-state assoc :debug-crmux-url dev-front-end-full)
      (swap! state/app-state assoc :debug-crmux-websocket )
      (create-websocket websocket-url))))


(defn get-debuggable-windows-error-handler [err]
  (let [msg (str "Error: status=" (:status err) ", message=" (:status-text (:parse-error err)))]
    
    (.log js/console  msg)
    (throw (js/Error. msg))))


(defn get-debug-window-info [json-host url]
  "Call crmux to get the list of debuggable windows. If successful, find the data for the specified
  debug url and extract the data into the app_state atom"
  
  (let [json-url (str json-host "/json")
        opts { :handler (get-debuggable-windows-handler json-host url)
              :error-handler get-debuggable-windows-error-handler
              }]
    
    (.log js/console  (str "About to GET " json-url))
    (GET json-url opts)))

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

(def js-result-in (chan))

(defmethod handler :result
  [message]
  (print "Result " message)
  (put! js-result-in message))

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

;;;(st/mirror st/ws-url)
(def ws (atom nil))

(defn on-ws-message
  [message]
  (let [data (.-data message)
        message-as-json (.parse js/JSON data)
        message-as-edn (js->clj message-as-json :keywordize-keys true)]
    (handler message-as-edn)))


(defn on-ws-error
  [error]
  (print (str "ws error:" error)))


;; watch for changes in the URL
(defn create-websocket
  [url]
  (print "create-websocket")
  (when (some? @ws)
    (.close @ws)
    (reset! ws nil))
  (if (nil? url)
    (reset! ws nil)
    (do
      (reset! ws (js/WebSocket. url))
      (aset @ws "onmessage" on-ws-message)
      (aset @ws "onerror" on-ws-error)
      (aset @ws "onclose" #(reset! ws nil)))))      ;; should onclose attempt a reconnection?

(defn ws-send [message]
  (print (.stringify js/JSON message))
  (when (nil? @ws)
    (create-websocket (:debug-crmux-websocket @state/app-state)))
  (.send @ws (.stringify js/JSON message)))   ;; XXX turn it into str

(defn ws-evaluate [expression]
  "evaluate javasript on the websocket and return a chanel for the results"
  (let [msg-id (goog/getUid expression)
        message (clj->js {"method" "Runtime.evaluate" "id" msg-id "params" 
                          {"expression" expression "returnByValue" true}})
        result (js-result-filter msg-id)]
    (ws-send message)
    result))
