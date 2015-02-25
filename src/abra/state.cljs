(ns abra.state
  (:require-macros [reagent.ratom :refer [reaction]])  
  (:require [reagent.core :as reagent]
            [re-frame.subs :as subs]
            [re-frame.handlers :as handlers] 
            [re-frame.db :refer [app-db]]
            [alandipert.storage-atom :refer [local-storage]]))

;; redirects any println to console.log
(enable-console-print!)

(def default-state
  {:debug-host "http://localhost:9223"
   :debug-crmux-url nil
   :debug-crmux-websocket nil})

(def persistent-db (local-storage 
                     (atom {:project-dir "."
                            :debug-url 
                            "file:///home/stu/dev/Abra2/test-page/index.html"}) 
                     ::persistent-db))

(defn reg-sub-key
  "given a key register and subscribe to it with
  simple getters and setters" 
  [key & [default]]
  (subs/register 
    key 
    (fn 
      [db] 
      (reaction (get @db key))))
  (handlers/register
    key
    (fn
      [db [_ value]]
      (swap! db assoc key value)))
  (when (some? default)
    (swap! app-db assoc key default)))

(defn initialise 
  [db]
  (swap! db merge default-state)
  (swap! db merge @persistent-db))

(handlers/register 
  :initialise 
  initialise)

(subs/register 
  :debug-crmux-url
  (fn [db [_]]
    (reaction 
      (or 
        (:debug-crmux-url @db)
        (:debug-host @db)))))

(reg-sub-key :debugging? false)

(subs/register
  :project-dir 
  (fn [db [_]]
    (reaction (:project-dir @db))))

(handlers/register
  :project-dir
  (fn [db [_ value]]
    (doseq [_db [persistent-db db]]
      (swap! _db assoc :project-dir value))))

(subs/register
  :debug-url 
  (fn [db [_]]
    (reaction (:debug-url @db))))

(handlers/register
  :debug-url
  (fn [db [_ value]]
    (doseq [_db [persistent-db db]]
      (swap! _db assoc :debug-url value))))

(reg-sub-key :clojurescript-string "(+ counter 3)")

(reg-sub-key :javascript-string "")

(reg-sub-key :namespace-string "(ns test.core)")

(reg-sub-key :locals-string "x\ny\nz")

(reg-sub-key :js-print-string "")

(reg-sub-key :lein-repl-status false)

(reg-sub-key :call-frame-id nil)

(reg-sub-key :call-frames [])

(reg-sub-key :scope-objects nil)

(reg-sub-key :scoped-locals {})

(reg-sub-key :local-id 0)