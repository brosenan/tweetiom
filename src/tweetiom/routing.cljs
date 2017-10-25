(ns tweetiom.routing
  (:refer-clojure :exclude [uuid?])
  (:require [reagent.core :as r]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History))

;; The content of this file is based on a recipe from the reagent-cookbook:  https://github.com/reagent-project/reagent-cookbook/tree/master/recipes/add-routing

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(def page (r/atom []))

(comment (secretary/set-config! :prefix "#"))

(defn navigate [& args]
  (reset! page args))

(defmulti render-page first)

(defn route [ctx]
  (render-page @page ctx))
