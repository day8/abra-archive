(ns main.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [abra.backend.macros :refer [<?]])  
  (:require [abra.backend.nrepl :as nrepl]
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
(def dialog         (js/require "dialog"))
(def crmux          (js/require (str js/__dirname "/../crmux/crmux.js")))


;; must happen before the app "ready" event occurs
;; TODO check if this port is actually available
(-> app .-commandLine 
    (.appendSwitch "remote-debugging-port" 
                   (:debug-port @state/app-state)))

;; store the window objects, otherwise it will be garbage collected and closed
(def main-window (atom nil))
(def debug-window (atom nil))



; (def abra-html "http://localhost:3449/abra.html") ;;figwheel
(def abra-html
  (let [html (str js/__dirname "/../../abra.html")]   ;; __dirname is that of the main.js
    (if (.existsSync fs html)
      (str "file:///" html)
      (.log js/console (str "HTML file not found: " html)))))

(defn show-message-exit
  [msg]
  "show a message and exit"
  (.showMessageBox dialog @main-window #js {:message msg 
                                            :type "warning"
                                            :buttons #js ["Close"]})
  (.close @main-window))

(def shutdownForRealHasHappened (atom false))

(defn shutdownForReal
  [] 
  (print "starting to shut down for real")
  
  ; ;; save current window information
  ; windowInformation.maximized = mainWindow.isMaximized();
  ; windowInformation.position = mainWindow.getPosition();
  ; windowInformation.size = mainWindow.getSize();
  
  ; // it's not super-important that this succeeds
  ; // https://github.com/oakmac/cuttle/issues/74
  ; try {
  ;   winston.info("saving window information", windowInformationFile);
  ;   fs.writeFileSync(windowInformationFile, JSON.stringify(windowInformation));
  ; }
  ; catch (e) {
  ;   // do nothing
  ;   winston.warn("couldn't save window information");
  ; }
  
  ;; toggle the shutdown for real flag and close the window
  (reset! shutdownForRealHasHappened true)
  (.close @main-window))

(.on ipc "shutdown-for-real" shutdownForReal)

;; NOTE: copied this directly from the atom-shell docs
;; is this really necessary?
(defn onWindowClosed
  [] 
  ;; dereference the window object
  (reset! main-window nil))

(defn onWindowClose
  [evt] 
  (print "window trying to close")
  
  ;; prevent window close if we have not shut down for real
  (when (not @shutdownForRealHasHappened) 
    (.preventDefault evt)
    
    ;; send shutdown signal to the window
    (print "sending client shutdown attempt")
    (.send (.-webContents @main-window) "shutdown-attempt")))

(defn init-browser
  []
  ;; websecurity is removed for figwheel
  (reset! main-window (browser-window. 
                        #js {:width 1200 :height 900 
                             :web-preferences #js {:web-security false}}))
  (.loadUrl @main-window abra-html)
  ; (.toggleDevTools @main-window)
  (go 
    ;; check that no other chrome instances have their remote debugging turned on
    (let [port-open (<? (nrepl/port-open? 9223))]
      (if port-open 
        (show-message-exit 
          "The crmux port is in use, Abra may be running in another window")
        (.start_crmux_server crmux))))
  (.on @main-window "close" onWindowClose)
  (.on @main-window "closed" onWindowClosed))


(.start crash-reporter)
(.on app "window-all-closed" 
     #(when-not (= js/process.platform "darwin") (.quit app)))
(.on app "ready" #(init-browser))

;; a render client might ask for a url to be opened
(.on ipc "open-url"
     (fn [event debug-url]
       (print "Opening " debug-url)
       (reset! debug-window 
               (browser-window. 
                 #js {:width 800 :height 600 :x 0 :y 0}))
       (.loadUrl @debug-window debug-url)
       #_(.toggleDevTools @debug-window)
       (.on @debug-window "closed" #(reset! debug-window nil))
       (.focus @main-window)))

(.on ipc "refresh-page"
     (fn [event]
       (.reload @debug-window)))

(.on ipc "close-url"
     (fn [event]
       (when @debug-window 
         (print "Closing the debug window")
         (.close @debug-window))))

(.on ipc "toggle-dev-tools"
     (fn [event]
       (print "Open the chrome debugger")
       (.toggleDevTools @main-window)))

;;; needed to use the :target :nodejs
(set! *main-cli-fn* #())