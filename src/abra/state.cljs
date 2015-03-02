(ns abra.state
  (:require-macros [reagent.ratom :refer [reaction]])  
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [register-sub
                                   register-handler
                                   register-pure-handler
                                   path]] 
            [re-frame.db :refer [app-db]]
            [alandipert.storage-atom :refer [local-storage]]))

;; redirects any println to console.log
(enable-console-print!)

(def default-state
  {:debug-host "http://localhost:9223"
   :debug-crmux-url nil
   :debug-crmux-websocket nil
   :initialised true})

(def persistent-db (local-storage 
                     (atom {:project-dir "."
                            :debug-url 
                            "file:///home/stu/dev/Abra2/test-page/index.html"}) 
                     ::persistent-db))

(defn reg-sub-key
  "given a key register and subscribe to it with
  simple getters and setters" 
  [key & [default]]
  (register-sub 
    key 
    (fn 
      [db] 
      (reaction (get @db key))))
  (register-pure-handler
    key
    (path [key])
    (fn
      [old-value [_ value]]
      value))
  (when (some? default)
    (swap! app-db assoc key default)))

(defn initialise 
  [db]
  (-> db 
      (merge default-state)
      (merge @persistent-db)))

(register-pure-handler 
  :initialise 
  initialise)

(register-sub
  :debug-crmux-url
  (fn [db [_]]
    (reaction 
      (or 
        (:debug-crmux-url @db)
        (:debug-host @db)))))

(reg-sub-key :debugging? false)

(register-sub
  :project-dir 
  (fn [db [_]]
    (reaction (:project-dir @db))))

(defn persistent-path
  "This middleware will persist the changes in the handler into
  local-storage"
  [p]
  (fn middleware
    [handler]
    ((path p)
     (fn new-handler
       [db v]
       (let [result (handler db v)]
         (swap! persistent-db assoc-in p result)
         result)))))

(register-pure-handler
  :project-dir
  (persistent-path [:project-dir])
  (fn [old-project-dir [_ value]]
    value))

(register-sub
  :debug-url 
  (fn [db [_]]
    (reaction (:debug-url @db))))

(register-handler
  :debug-url
  (fn [db [_ value]]
    (doseq [_db [persistent-db db]]
      (swap! _db assoc :debug-url value))))

(reg-sub-key :initialised)

(reg-sub-key :clojurescript-string "(+ counter 3)")

(reg-sub-key :javascript-string nil)

(reg-sub-key :namespace-string "(ns test.core)")

(reg-sub-key :locals-string "x\ny\nz")

(reg-sub-key :js-print-string "")

(reg-sub-key :lein-repl-status false)

(reg-sub-key :call-frame-id nil)

(reg-sub-key :call-frames [])

(reg-sub-key :scope-objects nil)

(reg-sub-key :scoped-locals {})

(reg-sub-key :local-id 0)

(reg-sub-key :show-spinner false)