(ns abra.dialog)


;;-- Access To Atom's Dialogs ---------------------------------------------------------------------
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
  [{:keys [properties] :as options}]
  (.showOpenDialog dialog
                   (clj->js {:title "Mike"
                             :properties ["openDirectory"]
                             :filters [{ :name "Project Files" :extensions ["clj"] }]})))


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
