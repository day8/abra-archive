(defproject abra "0.1.1-SNAPSHOT"
  :description "A ClojureScript debugging tool"
  :url "https://github.com/Day8/Abra2/"
  
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2913"]
                 [org.clojure/core.async    "0.1.346.0-17112a-alpha"]
                 [cljs-ajax "0.3.10"]
                 [reagent "0.5.0-alpha3"]
                 [re-com "0.1.6"]
                 [re-frame "0.1.6"]
                 [cljs-asynchronize "0.1.1-SNAPSHOT"]
                 [figwheel "0.2.2-SNAPSHOT"]
                 [ring/ring-core "1.3.2"]
                 [alandipert/storage-atom "1.2.4"]]
  
  :node-dependencies [[ws "~0.4.31"]
                      [bl "~0.4.2"]
                      [colors "~0.6.2"]
                      [nrepl-client "git+https://github.com/stumitchell/node-nrepl-client.git"]
                      [nrepl.js "~0.0.1"]
                      [portscanner "~1.0.0"]
                      [atom-shell "1.2.1"]]  
  
  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-ancient "0.6.2"]
            [com.cemerick/clojurescript.test "0.3.3"]
            [lein-figwheel "0.2.2-SNAPSHOT"]
            [lein-npm "0.5.0"]]
  
  :npm-root "run"
  
  ;;:main "start-atom.js"
  
  :nodejs {:main "js/compiled/main.js"
           :scripts {:start "node start-atom.js"}} 
 
  :profiles {:dev {:plugins [[com.cemerick/clojurescript.test "0.3.1"]]}}
  
  :jvm-opts         ["-Xmx1g" "-XX:+UseConcMarkSweepGC"] ;; cljsbuild eats memory
  
  :cljsbuild 
  {:builds [{:id "main"
             :source-paths ["src/main"]
             :compiler {
                        :output-to  "run/js/compiled/main.js"
                        :source-map "run/js/compiled/main.js.map"
                        :output-dir "run/js/compiled/main"
                        :target :nodejs
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
            
            {:id "fig-abra"
             :source-paths ["src/abra"]
             :compiler {
                        :output-to  "resources/public/js/compiled/abra.js"
                        :source-map "resources/public/js/compiled/abra.js.map"
                        :output-dir "resources/public/js/compiled/abra"
                        :optimizations :none
                        :pretty-print true}}
            
            
            {:id "test-node" 
             :source-paths ["src/main/backend" "test"]
             ; :notify-command ["node" "run/test/bin/runner-node.js" 
             ;                     "run/js/compiled/test"
             ;                     "run/js/compiled/test_node.js"]
             :compiler {:output-to "run/js/compiled/test_node.js"
                        :output-dir "run/js/compiled/test"
                        :target :nodejs
                        :hashbang false
                        :optimizations :none 
                        :pretty-print true}}
            
            {:id "test-page"
             :source-paths ["src/test-page"]
             :compiler {:output-to "test-page/js/test-page.js"
                        :source-map "test-page/js/test-page.js.map"
                        :output-dir "test-page/js"
                        :optimizations :none
                        :pretty-print true}}]
   
   :test-commands {"node-tests" ["node" "run/test/bin/runner-node.js" 
                                 "run/js/compiled/test"
                                 "run/js/compiled/test_node.js"]}}
  
  :figwheel {:http-server-root "public"
             :server-port 3449
             :repl false}  
  
  :source-paths ["src"]
  :test-paths ["test"]
  
  :aliases {"build"       ["do" "clean," "cljsbuild" "once" "main" "abra" "test-page"]
            "auto-build"  ["do" "clean," "cljsbuild" "auto" "main" "abra" "test-page"]
            "auto-test"   ["do" "clean," "cljsbuild" "auto" "test-node"]
            "run"         ["npm" "run" "start"]}
  
  :clean-targets ^{:protect false} ["run/js/compiled" 
                                    "resources/public/js/compiled"])
