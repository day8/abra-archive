(ns test.core)

;; redirects any println to console.log
(enable-console-print!)

(defn by-id [id]
  (.getElementById js/document id))

(defn one-second-count
  "count every second"
  [counter]
  (print "counter at " counter)
  (js/setTimeout #(one-second-count (inc counter)) 1000))

(one-second-count 1)