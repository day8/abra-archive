(ns main.core)

"I am the initial js file which atom executes.
 I bootstrap the process and kick off the GUI."

(def app (js/require "app"))
(def fs (js/require "fs"))
(def browser-window (js/require "browser-window"))
(def crash-reporter (js/require "crash-reporter"))

(def main-window (atom nil))

(defn start-html []
  (let [html (str "file://" js/__dirname "/../../abra.html")]   ;; __dirname is that of the main.js
    (if (.exists fs html)
      html
      (.log js/console (str "HTML file not found: " start-html)))))

;;(->> app  .commandLine (.appendSwitch 'remote-debugging-port', '9222'))

(defn init-browser []
	(reset! main-window (browser-window. (clj->js {:width 800 :height 600})))
	(.loadUrl @main-window (start-html)
	(.on @main-window "closed" #(reset! main-window nil)))

(.start crash-reporter)
(.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
(.on app "ready" #(init-browser))
