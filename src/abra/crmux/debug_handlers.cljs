(ns abra.crmux.debug-handlers
  (:require-macros [cljs.core.async.macros :refer [go-loop, go]])
  (:require [cljs.core.async :refer [put!, chan, <!, >!, mult, pub, sub]]
            [cljs.reader :as reader]
            [re-frame.core :refer [dispatch]]
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
                                                 "(anonymous function)"
                                                 (:functionName %2))
                                               :call-frame-id (:callFrameId %2)) 
                                            (range) call-frames))
        scope-chains (map :scopeChain call-frames)
        scope-objects (map #(hash-map :id %1 :objects %2) 
                           (range) 
                           (for [scope scope-chains]
                             (filter some? (map get-object scope))))]
    (dispatch [:scope-objects scope-objects])
    (dispatch [:call-frames call-frames-for-selection])
    ;; clear the scoped-locals in the db
    (dispatch [:clear-scoped-locals])
    (dispatch [:change-call-frame-id 0])))

(defmethod handler :Debugger.resumed
  ;; "called when the debugger resumes and you are no longer in a call frame"
  [message]
  (dispatch [:clear-scoped-locals])
  (dispatch [:clear-call-frames])
  (dispatch [:call-frame-id nil]))

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
        (if (some? result)
          result
          error-str)))))
