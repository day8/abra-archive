(ns abra.core-test
  (:require-macros [cemerick.cljs.test :refer (is deftest testing done)]
                   [cljs.core.async.macros :refer [go]]
                   [abra.backend.macros :refer [<?]])
  (:require [cemerick.cljs.test :as test]
            [abra.backend.nrepl :as nrepl]
            [cljs.core.async :refer [<!, chan, put!]]))

(deftest test-tests
  (testing "helper functions"
    (is (= (nrepl/process-namespace "
                                    (ns abra.backend.nrepl
                                    (:require-macros [cljs.core.async.macros :refer [go]])  
                                    (:require [cljs.reader :as reader]
                                    [goog.string.format]
                                    [cljs.core.async :refer [<!, >!]]))") 
           [{'(quote reader) '(quote cljs.reader)} 
            {'(quote go) {:name '(quote cljs.core.async.macros/go)}, 
             '(quote <!) {:name '(quote cljs.core.async/<!)}, 
             '(quote >!) {:name '(quote cljs.core.async/>!)}}]))))

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
                                        :locals ["x"] :namespace-str "[goog.string :as gstring]"))]
            (is (= "5" result))
            (is (= "((2) + (3));\n" js-result))
            (is (= "(test.core.x + (3));\n" namespace-result))
            (is (= "cljs.core.map.call(null,(2),(3));\n" core-result))
            (is (= "(x + (3));\n" locals-result))
            (is (= "goog.string.format(x,(3));\n" require-as-result)))
          (is (thrown? js/Error (<? (nrepl/eval "(+2 3)"))))
          (let [end-res (<? (nrepl/stop-lein-repl))
                port-closed (<? (nrepl/port-open? open-port))]
            (is (= (@nrepl/state :nrepl) false))
            (is (= port-closed false)))
          (done))))))

;;; needed to use the :target :nodejs
(set! *main-cli-fn* #())