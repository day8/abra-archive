(ns main.core
 (:require [main.lein-check :as lein-check]
           [main.backend.nrepl :as nrepl]))

"I am the initial js file which atom executes.
 I run in the, so called, browser context (nodejs)
 I bootstrap the application and kick off the GUI (Browser Window)."

;; redirects any println to console.log
(enable-console-print!)

(def app            (js/require "app"))
(def ipc            (js/require "ipc"))
(def fs             (js/require "fs"))
(def browser-window (js/require "browser-window"))
(def crash-reporter (js/require "crash-reporter"))

;; check that lein exists on the user's machine
(lein-check/run)

;; must happen before the app "ready" event occurs
;; TODO: how do we detect failure ???  The port may already be in use
(-> app .-commandLine (.appendSwitch "remote-debugging-port" "9222"))

;; store the window object, otherwise it will be garbage collected and closed
(def main-window (atom nil))


(def abra-html
  (let [html (str  js/__dirname "/../../abra.html")]   ;; __dirname is that of the main.js
    (if (.existsSync fs html)
      (str "file:///" html)
      (.log js/console (str "HTML file not found: " html)))))


(defn init-browser
  []
	(reset! main-window (browser-window. #js {:width 800 :height 600}))
	(.loadUrl @main-window abra-html)
  (.toggleDevTools @main-window)             ;;  TODO: condition this on a developer environment variable
	(.on @main-window "closed" #(reset! main-window nil)))


(.start crash-reporter)
(.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
(.on app "ready" #(init-browser))