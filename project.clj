(defproject abra "0.1.0-SNAPSHOT"
  :description "A ClojureScript debugging tool"
  :url ""
  
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2277"]
                 [cljs-ajax "0.2.4"]
                 [reagent "0.4.2"]
                 [re-com "0.1.6"]]
  
  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-git-deps "0.0.1-SNAPSHOT"]
            [com.cemerick/clojurescript.test "0.3.1"]]
  
  :profiles {:dev {:plugins [[com.cemerick/clojurescript.test "0.3.1"]]}}
  
  :git-dependencies [["https://github.com/gilbertw1/cljs-asynchronize.git"]]
  
  :jvm-opts         ["-Xmx1g" "-XX:+UseConcMarkSweepGC"] ;; cljsbuild eats memory
  
  :cljsbuild {:builds [{:id "main"
                        :source-paths ["src/main" ".lein-git-deps/cljs-asynchronize/src"]
                        :compiler {
                                   :output-to  "run/js/compiled/main.js"
                                   :source-map "run/js/compiled/main.js.map"
                                   :output-dir "run/js/compiled/main"
                                   :optimizations :simple
                                   :pretty-print true
                                   :closure-warnings {:check-useless-code :off}}}
                       
                       {:id "abra"
                        :source-paths ["src/abra"]
                        :compiler {
                                   :output-to  "run/js/compiled/abra.js"
                                   :source-map "run/js/compiled/abra.js.map"
                                   :output-dir "run/js/compiled/abra"
                                   :optimizations :none
                                   :pretty-print true}}
                       
                       {:id "test-node" 
                        :source-paths ["src/main/backend" "test" ".lein-git-deps/cljs-asynchronize/src"]
                        ; :notify-command ["node" "run/test/bin/runner-node.js" 
                        ;                     "run/js/compiled/test"
                        ;                     "run/js/compiled/test_node.js"]
                        :compiler {:output-to "run/js/compiled/test_node.js"
                                   :output-dir "run/js/compiled/test"
                                   :target :nodejs
                                   :optimizations :none 
                                   :pretty-print true}}]
              
              :test-commands {"node-tests" ["node" "run/test/bin/runner-node.js" 
                                            "run/js/compiled/test"
                                            "run/js/compiled/test_node.js"]}}
  
  :source-paths ["src" "test"]
  :test-paths ["test"]
  
  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test-node"]})
