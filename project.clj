(defproject abra "0.1.0-SNAPSHOT"
  :description "A ClojureScript debugging tool"
  :url ""

  :dependencies     [[org.clojure/clojure "1.6.0"] ;; 1.5.1
                     [org.clojure/clojurescript "0.0-2268" :exclusions [org.apache.ant/ant]] ;; 2173
                     [cljs-ajax "0.2.4"]
                     [reagent "0.4.2"]]

  :plugins [[lein-cljsbuild "1.0.3"]]


  :cljsbuild {
              :builds [{:id "main"
                        :source-paths ["src/atom_main"]
                        :compiler {
                                   :output-to  "deploy/js/compiled/main.js"
                                   :output-dir "deploy/js/compiled/main"
                                   :optimizations :simple
                                   :pretty-print true}}

                       {:id "abra"
                        :source-paths ["src/abra"]    ;; ".lein-git-deps/reagent-components/src"
                        :compiler {
                                   :output-to  "deploy/js/compiled/abra.js"
                                   :source-map "deploy/js/compiled/abra.js.map"
                                   :output-dir "deploy/js/compiled/abra"
                                   :optimizations :none
                                   :pretty-print true}}

                       #_{:id "test"
                          :source-paths ["src/mwireader" "test"]
                          :compiler {
                                     :output-to "testable.js"
                                     :output-dir "out_test"
                                     :optimizations :whitespace  ;; do not use 'none' or 'whitespace' - you will get a warning 'Node.js can be only used with simple/advanced optimizations, not with none/whitespace.'
                                     :pretty-print true
                                     :source-map "testable_map.js"
                                     }}]})


;; XXX Correct .gitignore
