(defproject abra "0.1.8-SNAPSHOT"
  :description "A ClojureScript debugging tool"
  :url "https://github.com/Day8/Abra2/"


  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3269"]
                 [org.clojure/core.async    "0.2.374"]
                 [cljs-ajax "0.5.1"]
                 [re-com "0.6.2"]
                 [re-frame "0.5.0"]
                 [cljs-asynchronize "0.1.1-SNAPSHOT"]
                 [figwheel "0.4.1"]
                 [ring/ring-core "1.4.0"]
                 [alandipert/storage-atom "1.2.4"]]

  :node-dependencies [[ws "~0.4.31"]
                      [bl "~0.4.2"]
                      [colors "~0.6.2"]
                      [nrepl-client "git+https://github.com/stumitchell/node-nrepl-client.git"]
                      [nrepl.js "~0.0.1"]
                      [portscanner "~1.0.0"]
                      [electron-prebuilt "~0.34.3"]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-ancient "0.6.2"]
            [com.cemerick/clojurescript.test "0.3.3"]
            [lein-figwheel "0.2.2-SNAPSHOT"]
            [lein-npm "0.5.0"]]

  :npm-root "run"

  ;;:main "start-atom.js"

  :nodejs {:main "js/compiled/main.js"
           :scripts {:start "node start-electron.js"}}

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
             :compiler {:main "abra.core"
                        :asset-path "compiled/abra"
                        :output-to  "run/compiled/abra.js"
                        :source-map true
                        :output-dir "run/compiled/abra"
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
             :source-paths ["src/abra/backend" "test"]
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
                        :pretty-print true
                        }}]

   ; node run/test/bin/runner-node.js run/js/compiled/test run/js/compiled/test_node.js
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
            "run"         ["do" "cljsbuild" "once" "main" "abra," "npm" "run" "start"]
            "node-tests"  ["do" "clean," "cljsbuild" "test" "node-tests"]}

  :clean-targets ^{:protect false} ["run/js/compiled"
                                    "run/compiled"
                                    "resources/public/js/compiled"
                                    "test-page/js"])
