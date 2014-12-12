(ns main.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [main.backend.macros :refer [<?]])  
  (:require [main.lein-check :as lein-check]
            [main.backend.nrepl :as nrepl]
            [main.state :as state]))

"I am the initial js file which atom executes.
I run in the, so called, browser context (nodejs)
I bootstrap the application and kick off the GUI (Browser Window)."

;; redirects any println to console.log
(enable-console-print!)

(state/initialise)

(def ipc            (js/require "ipc"))
(def fs             (js/require "fs"))
(def browser-window (js/require "browser-window"))
(def crash-reporter (js/require "crash-reporter"))
(def app            (js/require "app"))
(def crmux (js/require (str js/__dirname "/../crmux/crmux.js")))

;; check that lein exists on the user's machine
#_(lein-check/run)

;; must happen before the app "ready" event occurs
;; TODO check if this port is actually available
(-> app .-commandLine 
    (.appendSwitch "remote-debugging-port" 
                   (:debug-port @state/app-state)))

;; store the window objects, otherwise it will be garbage collected and closed
(def main-window (atom nil))
(def debug-window (atom nil))


(def abra-html
  (let [html (str js/__dirname "/../../abra.html")]   ;; __dirname is that of the main.js
    (if (.existsSync fs html)
      (str "file:///" html)
      (.log js/console (str "HTML file not found: " html)))))


(defn init-browser
  []
  (reset! main-window (browser-window. #js {:width 800 :height 600}))
  (.loadUrl @main-window abra-html)
  (.start_crmux_server crmux)
  #_(.toggleDevTools @main-window)             ;;  TODO: condition this on a developer environment variable
  (.on @main-window "closed" #(reset! main-window nil)))


(.start crash-reporter)
(.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
(.on app "ready" #(init-browser))

;; a render client might ask for a url to be opened
(.on ipc "open-url"
     (fn [event, debug-url]
       (.log js/console debug-url)
       (reset! debug-window (browser-window. #js {:width 800 :height 600}))
       (.loadUrl @debug-window debug-url)
       #_(.toggleDevTools @debug-window)
       (.on @debug-window "closed" #(reset! main-window nil))))