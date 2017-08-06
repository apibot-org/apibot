(ns apibot.mixpanel
  (:require
    [apibot.env :as env]))

(def mixpanel js/mixpanel)

(.init mixpanel env/mixpanel-token)

(defn track
  "Tracks the given event with the given data.

  Example:

  (track :ev-registration-click {:email \"foo\"})"
  ([event-name data]
   (.track mixpanel
           (name event-name)
           (clj->js data)))
  ([event-name]
   (track event-name {})))

(defn trackfn
  [event-name func]
  (fn [& args]
    (track event-name)
    (apply func args)))

(defn set-user-id! [user-id]
  (println "Setting user ID to " user-id)
  (.identify mixpanel user-id))

(defn set!
  "Sets arbitrary data for the current user.
  Make sure you have first called set-user-id!

  Example:

  (set-user-id! \"some-user-id\")
  (set! {\"$first_name\" \"Joe\"
         \"$last_name\" \"Doe\"
         \"$created\" \"2013-04-01T09:02:00\"
         \"$email\" \"joe.doe@example.com\"}
  "
  [data]
  (-> mixpanel .-people (.set (clj->js data))))
