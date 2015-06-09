(ns abra.handlers
  (:require [re-frame.core :refer [register-handler
                                   dispatch
                                   path]]
            [abra.crmux.websocket :refer [ws-evaluate]]
            [cljs.pprint :as pprint]
            [cljs.reader :refer [read-string *default-data-reader-fn*]]
            [clojure.string :refer [replace]]))

;; redirects any println to console.log
(enable-console-print!)

(def ipc (js/require "ipc"))

(reset! *default-data-reader-fn* 
        (fn [tag value]
          (str tag value)))

;; clear the scoped-locals dictionary
(defn clear-scoped-locals
  [scoped-locals [_]]
  {})

(register-handler 
  :clear-scoped-locals
  (path :scoped-locals)
  clear-scoped-locals)

(defn remove-js-functions
  "remove js functions from a string"
  [string]
  (replace 
    string 
    #"#\<(function [\s\S]*?\})\>" "\"(fn ...)\""))

;; add a scoped local to the db
(register-handler 
  :add-scoped-local
  (path [:scoped-locals])
  (fn [scoped-locals [_ scope-id variable-map]]
    (let [locals (scoped-locals scope-id {})
          local-name (:name variable-map)
          value (str "INTERMEDIATE::" (prn-str (:value variable-map)))
          old_id (get-in locals [local-name :id] (count locals))]
      (assoc scoped-locals scope-id 
        (assoc locals local-name {:label local-name 
                                  :id old_id
                                  :value value})))))

;; update a scoped local to the db with a value from the debugger
(register-handler 
  :update-scoped-local
  (path [:scoped-locals])
  (fn [scoped-locals [_ scope-id old-id local-name value]]
    (let [locals (scoped-locals scope-id {})
          pprint-value (try 
                         (pprint/write (read-string 
                                         (remove-js-functions value))
                                       :stream nil
                                       :pretty true
                                       :right-margin 35)
                         (catch :default e
                           (print e)
                           value))]
      (assoc scoped-locals scope-id 
        (assoc locals local-name {:label local-name :id old-id
                                  :value pprint-value})))))

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
    (dispatch [:reset-local-id])
    (-> db
        (assoc :scoped-locals {})
        (assoc :call-frame-id call-frame-id))))

(defn change-local-id
  [db [_ local-id]]
  (let [scope-id (:call-frame-id db)
        call-frames (:call-frames db)
        call-frame (first (filter #(= scope-id (:id %)) call-frames))
        call-frame-id (:call-frame-id call-frame)
        locals-map (get-in db [:scoped-locals scope-id])
        local (some #(when (= (:id %) local-id) %) (vals locals-map))
        name (:label local)
        expression (str "cljs.core.prn_str(" name ")")]
    (when local-id 
      (ws-evaluate db expression call-frame-id
                   #(dispatch [:update-scoped-local 
                               scope-id local-id name %])))
    (-> db
        (assoc :local-id local-id))))

(register-handler
  :change-local-id
  change-local-id)

(register-handler
  :reset-local-id
  (fn [db _]
    (change-local-id db [_ nil])))

;; refresh the page to be debugged
(register-handler
  :refresh-page
  (fn [db _]
    (.send ipc "refresh-page")
    (-> db    
        (assoc :call-frames [])
        (assoc :scoped-locals {})
        (assoc :local-id 0))))