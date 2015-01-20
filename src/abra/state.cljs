(ns abra.state
  (:require-macros [reagent.ratom  :refer [reaction]])  
  (:require [reagent.core :as reagent]
            [re-frame.subs :as subs]
            [re-frame.handlers :as handlers]))

;; redirects any println to console.log
(enable-console-print!)

(def default-state
  {:debug-host "http://localhost:9223"
   :debug-crmux-url nil
   :debug-crmux-websocket nil})

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
    (handlers/dispatch [key default])))

(defn initialise 
  [db]
  (swap! db merge default-state))

(handlers/register 
  :initialise 
  initialise)

(subs/register 
  :debug-crmux-url
  (fn [db]
    (reaction 
      (or 
        (:debug-crmux-url @db)
        (:debug-host @db)))))

(reg-sub-key :debugging? false)

(reg-sub-key :debug-url "file:///home/stu/dev/Abra2/test-page/index.html")

(reg-sub-key :project-dir ".")

(reg-sub-key :clojurescript-string "(+ counter 3)")

(reg-sub-key :javascript-string "")

(reg-sub-key :namespace-string "(ns abra.core
                               (:require [abra.state :as state]
                               [reagent.core :as reagent]
                               [abra.dialog :as dialog]
                               [re-com.core :refer [input-text input-textarea 
                                                     button hyperlink label 
                               spinner progress-bar checkbox radio-button 
                               title slider]]
                               [re-com.box :refer [h-box v-box box 
                                                   gap line]]))")

(reg-sub-key :locals-string "x\ny\nz")

(reg-sub-key :js-print-string "")

(reg-sub-key :lein-repl-status false)

(reg-sub-key :call-frame-id 0)

(reg-sub-key :call-frames [{:id 0 :label ""}])

(reg-sub-key :scoped-locals {0 [] 1 []})
