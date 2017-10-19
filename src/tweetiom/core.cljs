(ns tweetiom.core
  (:refer-clojure :exclude [uuid?])
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

;; Remove this before going to production...
(enable-console-print!)
;; 'host' is the object connecting this app to the host.

(defn some-func [a b c]
  (let [x (r/atom 0)]
    (fn [a b c]
      [:span {:on-click #(swap! x inc)} a b c @x])))

(defn app []
  (let [host (ax/default-connection r/atom)]
    [some-func 1 2 3]))

(let [elem (js/document.getElementById "app")]
  (when elem
    (r/render [app] elem)))


