(ns apibot.router
  "Implements a simple non-url based router, because the friggin
  accountant/secretary crap doesn't work on electron.

  The model is very simple. Every 'page' is represented as with a simbol
  and a function. The symbol identifies the function. The function can have
  any number of arguments (although most functions will take no arguments).

  To register a page call register! e.g.:
  ```
  (register! :main-page (fn [user-name] [:p \"My name is \" user-name]))
  ```

  To set the current page call dispatch! e.g.:
  ```
  (dispatch! :main-page \"Billy The Kid\")
  ```
  "
  (:require
    [secretary.core :as secretary]
    [clojure.string :refer [starts-with?]]))

;; ---- Route Helpers ----

(defn goto [url]
  (println "Goto: " url)
  (set! (-> js/window .-location .-hash)) url)

(defn current-page? [page]
  (starts-with? (-> js/window .-location .-hash) page))

(defn goto-editor []
  (goto "#editor"))
