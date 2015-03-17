(ns main.transit
  (:require [cognitect.transit :as t]))

(enable-console-print!)

(def transit-read-marshal (t/reader :json))
(defn transit-read
  [x]
  (t/read transit-read-marshal x))

(def transit-write-marshal (t/writer :json))
(defn transit-write
  [x]
  (t/write transit-write-marshal x))

(defrecord Point [x y])

(defn caching-point-handler
  ([] (caching-point-handler (atom {})))
  ([cache]
   (t/write-handler 
     (fn [v, h] (if (get @cache v) 
                  "cache" 
                  "point")) 
     (fn [v, h] (let [id (get @cache v)] 
                  (if (nil? id)
                    (do 
                      (swap! cache #(assoc % v (count %)))
                      [v.x v.y])
                    id))))))

(def PointHandler 
  (t/write-handler (fn [v, h] "point") (fn [v, h] [v.x v.y])))

(def writer (t/writer "json" {:handlers {Point PointHandler}}))

(defn write [x]
  (t/write writer x))

(defn read [x]
  (t/read (t/reader "json" 
                    {:handlers {"point" 
                                (fn [v] (Point. (aget v 0) (aget v 1)))}})
          x))

(defn c-writer 
  []
  (t/writer "json" {:handlers {Point (caching-point-handler)}}))

(defn c-write [x]
  (t/write (c-writer) x))

(defn c-read [x]
  (let [cache (atom {})] 
    (t/read (t/reader "json" 
                      {:handlers {"point" 
                                  (fn [v]
                                    (let [point (Point. (aget v 0) (aget v 1))]
                                      (swap! cache #(assoc % (count %) point))
                                      point))
                                  "cache"
                                  (fn [v]
                                    (get @cache v))}})
            x)))



;; implement a caching write for maps
; (def map-handler 
;   (fn 
;     [cache]
;     (this-as this
;              (set! (.-cache this) cache))))

; (set! (.. map-handler -prototype -tag) 
;       (fn
;         [v,h]
;         (this-as this
;                  (if ((.. this -cache -toId get) v)
;                    "cache"
;                    "map"))))

; (set! (.. map-handler -prototype -rep) 
;       (fn
;         [v,h]
;         (this-as this
;                  (let [id ((.. this -cache -toId get) v)]
;                    (if (not id
;                      v
;                      id)))))

; (defn caching-write 
;   [obj]
;   (let [cache {:toId (t/map) :curId 1}
;         writer (t/writer "json" {:handlers (t/map hash-map (map-handler))})]
;     )
;   )

