(ns abra.core-test
  (:require-macros [cemerick.cljs.test :refer (is deftest testing done)]
                   [cljs.core.async.macros :refer [go]]
                   [main.backend.macros :refer [<?]])
  (:require [cemerick.cljs.test :as test]
            [main.backend.nrepl :as nrepl]
            [cljs.core.async :refer [<!, chan, put!]]))

(deftest test-tests
  (testing "process-namespace"
    (is (= (nrepl/process-namespace 
             "(ns abra.backend.nrepl
             (:require-macros [cljs.core.async.macros :refer [go]])  
             (:require [cljs.reader :as reader]
             [goog.string.format]
             [cljs.core.async :refer [<!, >!]]))") 
           {:as-map {'(quote reader) '(quote cljs.reader)} 
            :refer-map {'(quote go) {:name '(quote cljs.core.async.macros/go)}, 
             '(quote <!) {:name '(quote cljs.core.async/<!)}, 
             '(quote >!) {:name '(quote cljs.core.async/>!)}}}))
    (is (= (nrepl/process-namespace 
             "(:require-macros [cljs.core.async.macros :refer [go go-loop]])") 
           {:as-map {} 
            :refer-map {'(quote go) {:name '(quote cljs.core.async.macros/go)}, 
             '(quote go-loop) {:name '(quote cljs.core.async.macros/go-loop)}}}))
    (is (= (nrepl/process-namespace 
             "[cljs.core.async :as async :refer [<!, >!]]") 
           {:as-map {'(quote async) '(quote cljs.core.async)} 
            :refer-map {'(quote <!) {:name '(quote cljs.core.async/<!)}, 
             '(quote >!) {:name '(quote cljs.core.async/>!)}}}))
    (is (= (nrepl/process-namespace 
             "[jamesmacaulay.async-tools.core :as tools]))") 
           {:as-map {'(quote tools) '(quote jamesmacaulay.async-tools.core)} 
            :refer-map {}}))
    (is (= (nrepl/process-namespace 
             "[jamesmacaulay.async-tools.core :as tools?]))") 
           {:as-map {'(quote tools?) '(quote jamesmacaulay.async-tools.core)} 
            :refer-map {}}))
    (is (= (nrepl/process-namespace 
             "(ns jamesmacaulay.zelkova.signal
             (:refer-clojure :exclude [merge count])
             (:require [cljs.core :as core]
             [clojure.zip :as zip]
             [cljs.core.async :as async :refer [chan,<! >!]]
             [cljs.core.async.impl.protocols :as impl]
             [cljs.core.async.impl.channels :as channels]
             [jamesmacaulay.async-tools.core :as tools]
             [alandipert.kahn :as kahn])
             (:require-macros [cljs.core.async.macros :refer [go go-loop]]))") 
           {:as-map {'(quote core) '(quote cljs.core), 
             '(quote zip) '(quote clojure.zip), 
             '(quote async) '(quote cljs.core.async), 
             '(quote impl) '(quote cljs.core.async.impl.protocols), 
             '(quote channels) '(quote cljs.core.async.impl.channels), 
             '(quote tools) '(quote jamesmacaulay.async-tools.core), 
             '(quote kahn) '(quote alandipert.kahn)} 
            :refer-map {'(quote chan) {:name '(quote cljs.core.async/chan)}, 
             '(quote <!) {:name '(quote cljs.core.async/<!)}, 
             '(quote >!) {:name '(quote cljs.core.async/>!)}, 
             '(quote go) {:name '(quote cljs.core.async.macros/go)}, 
             '(quote go-loop) {:name '(quote cljs.core.async.macros/go-loop)}}}))
    (is (= (nrepl/find-namespace "(ns test.test)") "test.test"))
    (is (= (nrepl/find-namespace "(ns test.test?)") "test.test?"))))

(deftest ^:async test-nrepl-startup-shutdown
  (testing "Test nrpel start up and shutdown and connection"
    (let [port 7889] ;; change the port number so the tests can run at the same time
      (is (= (@nrepl/state :nrepl) false))
      ;start the server
      (go
        (let [open-port (<? (nrepl/start-lein-repl {:port port}))
              port-open (<? (nrepl/port-open? open-port))]
          (is (= (@nrepl/state :nrepl) true))
          (is (= port-open true))
          (let [result (<? (nrepl/eval "(+ 2 3)"))
                js-result (<? (nrepl/cljs->js "(+ 2 3)"))
                namespace-result (<? (nrepl/cljs->js "(+ x 3)" :namespace "test.core"))
                core-result (<? (nrepl/cljs->js "(map 2 3)"))
                locals-result (<? (nrepl/cljs->js "(+ x 3)" :namespace "test.core" 
                                                  :locals ["x"]))
                require-as-result (<? (nrepl/cljs->js 
                                        "(gstring/format x 3)" :namespace "test.core"
                                        :locals ["x"] :namespace-str "[goog.string :as gstring]"))
                require-as-result-namespace (<? (nrepl/cljs->js 
                                        "(gstring/format x 3)"
                                        :namespace-str "(ns test.core [goog.string :as gstring])"))
                require-as-result-namespace2 (<? (nrepl/cljs->js 
                                        "(gstring/format x 3)"
                                        :namespace "test.core"
                                        :namespace-str "(ns ignore.me [goog.string :as gstring])"))]
            (is (= "5" result))
            (is (= "((2) + (3));\n" js-result))
            (is (= "(test.core.x + (3));\n" namespace-result))
            (is (= "cljs.core.map.call(null,(2),(3));\n" core-result))
            (is (= "(x + (3));\n" locals-result))
            (is (= "goog.string.format(x,(3));\n" require-as-result))
            (is (= "goog.string.format(test.core.x,(3));\n" require-as-result-namespace))
            (is (= "goog.string.format(test.core.x,(3));\n" require-as-result-namespace2)))
          (is (thrown? js/Error (<? (nrepl/eval "(+2 3)"))))
          (let [end-res (<? (nrepl/stop-lein-repl))
                port-closed (<? (nrepl/port-open? open-port))]
            (is (= (@nrepl/state :nrepl) false))
            (is (= port-closed false)))
          (done))))))

;;; needed to use the :target :nodejs
(set! *main-cli-fn* #())