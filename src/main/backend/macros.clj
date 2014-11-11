(ns main.backend.macros)

(defn- throw-err [form]
  `(let [e# ~form] 
     (if (instance? js/Error e#) 
       (throw e#)
     e#)))

(defmacro <? [ch]
  (throw-err `(cljs.core.async/<! ~ch)))
