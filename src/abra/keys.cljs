(ns abra.keys
  (:require [goog.events :as events])
  (:import [goog.events EventType]))

(def ipc (js/require "ipc"))


(defn bind-f12
  "Bind 'f12' to the toggle the debug window." 
  []
  (events/listen js/window EventType.KEYDOWN 
                 #(when (= (.-keyCode %) 123)
                    (.send ipc "toggle-dev-tools"))))

(defn bind-keys
  "Bind f12."[]
  (bind-f12))