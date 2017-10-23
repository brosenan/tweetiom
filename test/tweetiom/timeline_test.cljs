(ns tweetiom.timeline-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.timeline :as tl]
            [reagent-query.core :as rq]))

;;;;;; Time Range Display ;;;;;;;;;
;; time-range-display is a component that displays elements based on a time-based query, in descending order, from the newest to the oldest.
;; It paginates the data so that it only retrieves the newest elements, and retrieves older elements on demand.

;; time-range-display takes a map as its single argument, and returns a function that renders the UI.
;; The map should have the following fields:
;; - :time-aggregation - the time aggregation unit we use, in milliseconds. The default is (* 1000 60 60 24), representing a day.
;; - :initial-range - the number of time aggregation units (e.g., days) to be initially displayed. The default is 7.
;; - :range-increment - the number of time aggregation units to add on each incrementation. Defaults to 3.
;; - :increment-btn-caption - the caption on the button that requests older results. Defaults to "Get older"
;; - :query - a function that takes a tuple [from to] of time aggregation unit numbers and returns a sequence of items to display.
;; - :render - a function that takes elements returned by :query, and renders them.

;; In the following example, we will use reverse of range as our query function.
(deftest time-range-display-1
  (let [params {:query (comp reverse range)
                :render (fn [item]
                          [:li item])}
        ui-func (tl/time-range-display params)
        ui (ui-func params)
        ;; The expected items are the seven last day numbers
        a-day (* 1000 60 60 24)
        today (.floor js/Math (/ (.getTime (js/Date.)) a-day))
        ;; We want seven days ending today (inclusive)
        expected (reverse (range (- today 6) (inc today)))]
    (is (= (rq/find ui :li) expected))
    ;; If we click the "Get older" button, we should see 3 more elements in the range
    (let [[get-older] (rq/find ui :button.get-older:on-click)]
      (get-older))
    (let [ui (ui-func params)
          expected (reverse (range (- today 9) (inc today)))]
      (is (= (rq/find ui :li) expected)))))

