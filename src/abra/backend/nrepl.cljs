;;;; Node backend calls that must be separate from the 
;;;; UI calls as they must be tested on the node runner
(ns abra.backend.nrepl
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cljs-asynchronize.macros :refer [asynchronize]]
                   [abra.backend.macros :refer [<?]])  
  (:require [cljs.reader :as reader]
            [clojure.string :as string]
            [goog.string :as gstring] 
            [goog.string.format]
            [cljs.core.async :refer [put!, chan, <!, >!]]))

(enable-console-print!)

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
  "(defn compile-form-seq
    \"Compile a sequence of forms to a JavaScript source string.\"
    [forms env]
    (env/ensure
    (compiler/with-core-cljs nil
      (fn []
        (with-out-str
            (doseq [form forms]
              (compiler/emit (analyzer/analyze env form))))))))
   (compile-form-seq ['%s]
    {:ns {:name \"%s\" :requires %s} :locals %s})")

(defn- position   
  "Takes a vector 'v' and an 'item' within 'v'.  
  Returns the zero-based index of 'item' in 'v'.
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

(defn- find-namespace
  "finds the namespace from a ns command"
  [namespace-str]
  (last 
    (.match namespace-str #"ns ([\w\.\+\-\?\!]*)")))

(defn- process-namespace
  "Looks at a namespace command and strips out :refer and :as in the
  require statements so that the symbols can be added to the 
  locals  and :ns :requires dictionaries"
  [namespace-str]
  (let [namespace-forms (reader/read-string namespace-str)
        as-map (find-as namespace-forms)
        refer-map (find-refer namespace-forms)]
    {:as-map as-map 
     :refer-map refer-map}))

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
                                   "startTimeout" 60000) 
                           ...))
       (swap! state assoc :nrepl true)
       (swap! state assoc :port port)
       (swap! state assoc :server-state server-state)
       ; send off an eval to warm up the nrepl
       (local-eval "(+ 1 1)")
       port))))

(defn stop-lein-repl 
 "Asynchronously stops a lein repl that had previously been started by 
 start-lein-repl
  
    Returns: (chan)
      closes this channel on completion" 
  []
  ;; can't use asynchronise as the callback is not the correct interface
  (let [result (chan)
        server-state (:server-state @state)]
    (.stop node-server server-state
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
     (go 
       (try 
         (let [port (<! (start-lein-repl options))]
           (<? (local-eval statement)))
         (catch js/Error e 
           e))))))

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
                        namespace nil locals []
                        namespace-str ""}
                   :as options}]
     (go 
       (try 
         (let [locals (flatten (into [] (for [name locals] 
                                 [(reader/read-string name)
                                 (reader/read-string
                                   (string/replace name #"_" "-"))])))
               locals (into {} (for [name locals]
                                 [`'~name {:name `'~name}]))
               {:keys [as-map refer-map]} (process-namespace namespace-str)
               locals (merge locals refer-map)
               namespace (or namespace (find-namespace namespace-str))
               eval-str (gstring/format convert-cljs-str statement
                                        namespace as-map locals)
               repl-str (str private-namespace-str eval-str)
               result (<? (eval repl-str options))
               evaled-result (reader/read-string result)] 
           (if evaled-result
             (last (.match evaled-result #"([\s\S]*;)"))
             "clojure error"))
         (catch js/Error e 
           e)))))
