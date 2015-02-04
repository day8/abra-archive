(ns abra.crmux.handlers
  (:require [clojure.string :as string]
            [ajax.core :refer [GET]]
            [goog]
            [cljs.core.async :refer [put!, chan, <!, >!, mult, tap, untap]]
            [re-frame.handlers :refer [register dispatch]]
            [abra.crmux.websocket :refer [create-websocket]]))

;; redirects any println to console.log
(enable-console-print!)

(defn clean-url 
  "Removes leading/trailing spaces 
   then removes trailing slash if it exists
   Changes slashs if they are windows ones"
  [url]
  (-> url
    string/trim
    (string/replace #"/$" "")
    (string/replace "\\" "/")))

(defn get-debug-page-details 
  "Takes a vector of maps (from http://localhost:9223/json) and selects the map
  which contains the data for the requested url" 
  [tabs url]
  (let [matches-url #(= (clean-url (% "url")) url)
        matches (filter matches-url tabs)
        matchcount (count matches)]
    (print "get-debug-page-details found matches: " matchcount)
    (when-not (= matchcount 1) 
      (js/alert (str "Expected ONE window to debug but found " matchcount)))
    (first matches)))


(defn new-crmux-url 
  "Digests the chrome debugger main page to find out what the url for the
  web page to be debugged is , and what the websocket url is, add these to
  the db"
  [db [_ host url response]]
  (let [url (clean-url url)
        window-to-debug (get-debug-page-details response url)
        dev-front-end-full (str host (window-to-debug "devtoolsFrontendUrl"))
        websocket-url (window-to-debug "webSocketDebuggerUrl")]
    (print  "webSocketDebuggerUrl=" websocket-url 
           ", devtoolsFrontendUrl=" dev-front-end-full)
    (create-websocket db websocket-url)
    (swap! db assoc :debug-crmux-url dev-front-end-full)))

(register :crmux-handlers.new-crmux-url new-crmux-url)

(defn get-debuggable-windows-error-handler 
  "called when there is a problem getting the chrome debugger main window"
  [err]
  (let [msg (str "Error: status=" (:status err) 
                 ", message=" (:status-text (:parse-error err)))]
    (print msg)
    (throw (js/Error. msg))))

(defn get-debug-window-info 
  "Call crmux to get the list of debuggable windows."  
  [json-host url]
  (let [json-url (str json-host "/json")
        opts {:handler #(dispatch 
                          [:crmux-handlers.new-crmux-url json-host url %])
              :error-handler get-debuggable-windows-error-handler}]
    
    (print "About to GET " json-url)
    (GET json-url opts)))
