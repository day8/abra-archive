;;;; Node backend calls that must be separate from the 
;;;; UI calls as they must be tested on the node runner
(ns main.backend.nrepl
  (:require-macros [cljs.core.async.macros :refer [go]]
                     [cljs-asynchronize.macros :refer [asynchronize]])  
  (:require [cljs.reader :as reader]
            [clojure.string :as string]
            [goog.string :as gstring] 
            [goog.string.format]
            [cljs.core.async :refer [put!, chan, <!, >!]]))

(def state (atom {:nrepl false :port nil}))

(def ^:private node-server (js/require "nrepl-client/src/nrepl-server"))
; "node-nrepl-client": "~0.2.1",
(def ^:private node-client (js/require "nrepl.js"))
; "nrepl.js": "~0.0.1",
(def ^:private port-scanner (js/require "portscanner"))
; "portscanner": "~1.0.0"

(defn port-open?
  "Asynchronously determines if a port is open for connections, 
  used to find out if the nrepl has been started

    Args:
      port (int): the port that will be tested
  
    Returns: (chan)
      Boolean. is the port open::
        true -- the port is in use
        false -- the port is not in use"
  [port]
  (asynchronize
    (def result (.checkPortStatus port-scanner port "127.0.0.1" ...))
    (if (= result "open")
      true
      false)))

(defn- next-open-port
  "Asynchronously returns the next available port above port,
  used to find an available port if the choosen port is already used
  
    Args:
      port (int): the port that will be tested
  
    Returns: (chan)
      int. the next available (not used) port"
  [port]
  (go 
    (let [port-open (<! (port-open? port))]
      (if port-open
        (<! (next-open-port (inc port)))
        port))))

(defn- js->seq
  "Converts a javascript array to a sequence"
  [sequence]
  (try
    (seq sequence)
    (catch :default e
      (for [i (range (alength sequence))] 
        (aget sequence i)))))

(defn- local-eval
  [command]
  (asynchronize
    (try  
      (def connection (.connect node-client (:port @state) ...)) 
      (def result (.eval connection command ...))
      #_(.log js/console (print-str result))
      (last (js->seq result))
    (catch js/Error e 
      e))))

(def ^:private private-namespace-str 
  "(ns my-compile.core
  (:require
  [cljs.analyzer :as analyzer]
  [cljs.compiler :as compiler]
  [cljs.env :as env]))
  ")

(def ^:private convert-cljs-str
  "(with-out-str 
   (env/ensure
   (compiler/with-core-cljs
   (compiler/emit 
   (analyzer/analyze 
   {:ns {:name \"%s\" :requires %s} :locals %s} '%s)))))")

(defn- position   
  "Takes a vector 'v' and an 'item' within 'v'.  Returns the zero-based index of 'item' in 'v'.
  Returns nil if item not found."
  [v item]
  (let [index-fn (fn [index i] (when (= i item) index))]
    (first (keep-indexed index-fn v))))

(defn- find-as [form]
  (if (coll? form) 
    (if (vector? form)
      (if (some #{:as} form)
        (let [namespace-name (first form)
              as-index (inc (position form :as))
              as-form (nth form as-index)]
          {`'~as-form `'~namespace-name})
        {})
      (into {} (for [f form]
                 (find-as f))))
  {}))

(defn- find-refer [form]
  (if (coll? form) 
    (if (vector? form)
      (if (some #{:refer} form)
        (let [namespace-name (first form)
              refer-index (inc (position form :refer))
              refer-form (nth form refer-index)]
          (into {} (for [s refer-form]
                     {`'~s {:name `'~(symbol namespace-name s)}})))
        {})
      (into {} (for [f form]
                 (find-refer f))))
    {}))

(defn- process-namespace
  "Looks at a namespace command and strips out :refer and :as in the
  require statements so that the symbols can be added to the 
  locals  and :ns :requires dictionaries"
  [namespace-str]
  (let [namespace-forms (reader/read-string namespace-str)
        as-map (find-as namespace-forms)
        refer-map (find-refer namespace-forms)]
    [as-map refer-map]))

(defn start-lein-repl
  "Asynchronously starts a lein repl so that clojure code can be 
  executed
  
    Options (map):
      port (int): the port that the repl will open
      project-path (str): the path to the project.clj file that the 
                        repl will use, so that require statements 
                         will work
  
    Returns: (chan)
      int. the port that the repl has opened" 
  ([] (start-lein-repl {}))
  ([{:keys [project-path port]
     :or {project-path "."
          port 7888}}]
   ;;) starts an lein repl using node-nrepl-client
   (asynchronize
     (let [port (<! (next-open-port port))] 
       (def server-state (.start 
                           node-server 
                           (js-obj "port" port 
                                   "projectPath" project-path
                                   "startTimeout" 30000) 
                           ...))
       (swap! state assoc :nrepl true)
       (swap! state assoc :port port)
       (swap! state assoc :server-state server-state)
       port))))

(defn stop-lein-repl 
 "Asynchronously stops a lein repl that had previously been started by 
 start-lein-repl
  
    Returns: (chan)
      closes this channel on completion" 
  []
  ;; can't use asynchronise as the callback is not the correct interface
  (let [result (chan)] 
    (.stop node-server (:server-state @state)
           (fn [] 
             (swap! state assoc :nrepl false)
             (put! result true)))
    result))

(defn eval
  "Asynchronously, evaluates a cljoure statement on a repl,
  will also open a repl if one have not been started

    Args:
      statement (str): the clojure statement to be evaluated

    Options (map):
      namespace (str): the namespace that will be prepended to any
                      variables in the statement
      locals (seq of str): the local variables in the statement that 
                          will not pe placed under a namespace
      port (int): the port that the repl will open
      project-path (str): the path to the project.clj file that the 
                        repl will use, so that require statements 
                        will work
  
    Returns: (chan)
      str. the results of the clojure statement"
  ([statement] (eval statement {}))
  ([statement options]
   #_(println statement)
   (if (:nrepl @state)
     (local-eval statement)
     (go (let [port (<! (start-lein-repl options))]
           (local-eval statement))))))

(defn cljs->js 
  "Asynchronously converts clojurescript into javascript using a repl,
  Also will open a repl if it has not ben started already
  
    Args:
      statement (str): the clojurescript statement to be converted

    Named Options: 
      namespace (str): the namespace that will be prepended to any
                      variables in the statement
      locals (seq of str): the local variables in the statement that 
                          will not pe placed under a namespace
      port (int): the port that the repl will open
      project-path (str): the path to the project.clj file that the 
                        repl will use, so that require statements 
                         will work
  
    Returns: (chan)
      str. the cljs statement converted into javascript"
  ([statement & {:keys [project-path port namespace locals
                        namespace-str]
               :or {project-path "." port 7888
                    namespace "" locals []
                    namespace-str ""}
               :as options}]
   (go 
     (let [locals (into [] (for [name locals] 
                             (reader/read-string name)))
           locals (into {} (for [name locals]
                             [`'~name {:name `'~name}]))
           [as-map refer_map] (process-namespace namespace-str)
           locals (merge locals refer_map)
           eval-str (gstring/format convert-cljs-str namespace
                                         as-map locals statement)
           repl-str (str private-namespace-str eval-str)
           result (<! (eval repl-str options))]
       (reader/read-string result)))))
