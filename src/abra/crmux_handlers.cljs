(ns abra.crmux-handlers
  (:require [clojure.string :as string]
            [ajax.core :refer [GET POST]]
            [abra.state :as state]))

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
    (.log js/console (prn-str response))
    
    (let [url (clean-url url)
          window-to-debug (get-debug-page-details response url)
          _ (print window-to-debug)
          dev-front-end-full (str host (window-to-debug "devtoolsFrontendUrl"))]
      (.log js/console  (str "webSocketDebuggerUrl=" (window-to-debug "webSocketDebuggerUrl") ", devtoolsFrontendUrl=" dev-front-end-full))
      (swap! state/app-state assoc :debug-crmux-url dev-front-end-full)
      #_(state/set-web-socket-debugger-url (window-to-debug "webSocketDebuggerUrl")))))


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