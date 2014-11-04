(defproject abra "0.1.0-SNAPSHOT"
  :description "A ClojureScript debugging tool"
  :url ""

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [cljs-ajax "0.2.4"]
                 [reagent "0.4.2"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [com.cemerick/clojurescript.test "0.3.1"]]


  :cljsbuild {
              :builds [{:id "main"
                        :source-paths ["src/main"]
                        :compiler {
                                   :output-to  "run/js/compiled/main.js"
                                   :source-map "run/js/compiled/main.js.map"
                                   :output-dir "run/js/compiled/main"
                                   :optimizations :simple
                                   :pretty-print true}}

                       {:id "abra"
                        :source-paths ["src/abra"]
                        :compiler {
                                   :output-to  "run/js/compiled/abra.js"
                                   :source-map "run/js/compiled/abra.js.map"
                                   :output-dir "run/js/compiled/abra"
                                   :optimizations :none
                                   :pretty-print true}}]}


  :source-paths ["src" "test"]
  :test-paths ["test"]

  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test"]}
  )
