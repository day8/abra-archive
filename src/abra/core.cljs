(ns abra.core
  (:require [abra.state :as state]
            [reagent.core :as reagent]))


(defn get-element-by-id [id]
  (.getElementById js/document id))


(defn page[]
  [:div nil "Hello Abra"])


;; kick off the application
(state/initialise)
(reagent/render-component [page] (get-element-by-id "app"))
