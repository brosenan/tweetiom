(ns tweetiom.core
  (:refer-clojure :exclude [uuid?])
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent]
            [tweetiom.routing :as route])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]
                   [secretary.core :refer [defroute]])
  (:import goog.History))

(enable-console-print!)

(defonce host (ax/default-connection r/atom))

(let [elem (js/document.getElementById "app")]
  (when elem
    (r/render [route/render-page host] elem)))

