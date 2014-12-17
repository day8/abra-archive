(ns abra.dialog)


;;-- Access To Atom's Dialogs -------------------------------------------------
;;
;; For reference see:  https://github.com/atom/atom-shell/blob/master/docs/api/dialog.md
;;


(def remote (js/require "remote"))
(def dialog (.require remote "dialog"))



(defn getCurrentWindow
  []
  (.getCurrentWindow remote))



;; showOpenDialog
;;
;;
(defn open
  ([options] (open options nil))
  ([{:keys [properties, title, filters] 
      :or {properties []
           title "Open File"
           filters []}
      :as options} callback]
  (.showOpenDialog dialog (clj->js options) callback)))


;; showSaveDialog
;; returns the path chosen by the user, otherwise nil  (undefined?)
;;
(defn save
  [options]
  (.showSaveDialog dialog (getCurrentWindow) (clj->js options)))


;; showMessageBox
;;
;; (dialog/open {:type "info" :message "Didn't work" :buttons ["Cancel Ok"]})
;;
(defn message
  [options]
  (.showMessageBox dialog (getCurrentWindow) (clj->js options)))
