(ns tweetiom.timeline
  (:require [axiom-cljs.core :as ax]
            [reagent.core :as r])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))


(defn floor [x]
  (.floor js/Math x))

(defn time-range-display [params]
  (let [{:keys [time-aggregation initial-range]
         :or {time-aggregation (* 1000 60 60 24)
              initial-range 7}} params
        curr (-> (js/Date.)
                 .getTime
                 (/ time-aggregation)
                 floor)
        ranges (r/atom [[(inc (- curr initial-range)) (inc curr)]])]
    (fn [params]
      (let [{:keys [query render
                    time-aggregation
                    range-increment increment-btn-caption]
             :or {time-aggregation time-aggregation
                  range-increment 3
                  increment-btn-caption "Get older"}} params]
        [:div.time-range-display
         (apply concat
                (for [[from to] @ranges]
                  (for [elem (query from to)]
                    (render elem))))
         [:button.get-older {:on-click #(let [[from' to'] (last @ranges)
                                              to from'
                                              from (- from' range-increment)]
                                          (swap! ranges conj [from to]))}
          increment-btn-caption]]))))
