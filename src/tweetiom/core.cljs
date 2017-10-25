(ns tweetiom.core
  (:refer-clojure :exclude [uuid?])
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as reagent])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]
                   [secretary.core :refer [defroute]])
  (:import goog.History))

;; Remove this before going to production...
(enable-console-print!)
;; 'host' is the object connecting this app to the host.

(defn some-other-func [a i]
  [:input {:value (get @a i)
           :on-change #(swap! a assoc i (.-target.value %))}])

(defn some-func []
  (let [x (r/atom [])]
    (fn [a b c]
      [:ul (doall (for [i (range c)]
                    ^{:key i} [some-other-func x i]))
       [:div {:key "something special"} (pr-str @x)]])))

(defn app []
  [some-func 1 2 3])

(let [elem (js/document.getElementById "app")]
  (when elem
    (r/render [app] elem)))


