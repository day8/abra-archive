(ns test.core)

;; redirects any println to console.log
(enable-console-print!)

(defn by-id [id]
  (.getElementById js/document id))

(defn one-second-count
  "count every second"
  [counter]
  (print "counter at " counter)
  (js/setTimeout #((a-closure 1) counter) 3000))

(defn one-frame-up
  "up one more frame"
  [ofu-counter]
  (let [five (+ 5 ofu-counter)]
    (one-second-count (+ five ofu-counter))))

(defn a-closure
  [closure-var]
  (fn [ac-counter]
    (one-frame-up (+ ac-counter closure-var))))

(one-second-count 1)