(ns tweetiom.timeline-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.timeline :as tl]
            [tweetiom.tweets :as tweets]
            [tweetiom.routing :as route]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]
            [secretary.core :as secretary]))

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
;; - :wrap - a function that takes the sequence of UI elements rendered by the render function, and returns the UI component to be presented.

;; In the following example, we will use reverse of range as our query function.
(deftest time-range-display-1
  (let [params {:query (comp reverse range)
                :render (fn [item]
                          [:li item])
                :wrap (fn [seq]
                        [:ul.my-list
                         seq])}
        ui-func (tl/time-range-display params)
        ui (ui-func params)
        ;; The expected items are the seven last day numbers
        a-day (* 1000 60 60 24)
        today (.floor js/Math (/ (.getTime (js/Date.)) a-day))
        ;; We want seven days ending today (inclusive)
        expected (reverse (range (- today 6) (inc today)))]
    (is (= (rq/query ui :ul.my-list :li) expected))
    ;; The "Get older" button is a member of the list, and has a :key attribute
    (is (= (rq/query ui :ul.my-list :button.get-older:key) [:get-older-btn]))
    ;; If we click the "Get older" button, we should see 3 more elements in the range
    (let [[get-older] (rq/query ui :ul.my-list :button.get-older:on-click)]
      (get-older))
    (let [ui (ui-func params)
          expected (reverse (range (- today 9) (inc today)))]
      (is (= (rq/find ui :li) expected)))))

;;;;;; Timeline ;;;;;;;;;
;; The timeline function displays a time-range-display, displaying timeline items for the logged-in user, based on the timeline-query.
(deftest timeline-1
  (let [host (ax/mock-connection "alice")
        ui (tl/timeline host)
        tl-mock (ax/query-mock host :tweetiom/timeline)]
    ;; The UI is a time-range-display with attributes.
    ;; Its :wrap attribute builds a :div of class .timeline
    (let [[wrap] (rq/find ui {:elem tl/time-range-display :attr :wrap})
          wrapped (wrap :foo)]
      (is (= (rq/find wrapped :div.timeline) [:foo])))
    ;; The :query attribute is a function that, given a day range, performs a timeline query for that range and the given user
    (let [[query] (rq/find ui {:elem tl/time-range-display :attr :query})]
      (is (= (query 200 300) []))
      (tl-mock ["alice" 200 300] ["bob" [:text "hello"] 1000])
      (tl-mock ["alice" 200 300] ["charlie" [:text "world"] 2000])
      ;; The :render function renders calls tweets/tweet-viewer to display query results
      (let [[render] (rq/find ui {:elem tl/time-range-display :attr :render})
            items (query 200 300)
            ui' (map render items)
            tweets (rq/find ui' {:elem tweets/tweet-viewer})]
        (is (= (map :tweet tweets) [[:text "world"] [:text "hello"]])) ;; Tweets are displayed in descending time order
        ;; Each element is annotated with a :key which is a tuple [:author :ts]
        (let [rendered (render {:tweet :foo :author "bob" :ts 1234})]
          (is (= (-> rendered meta :key) ["bob" 1234])))))))

;;;;;; Timeline Page ;;;;;;;;;
;; We define the timeline page by providing a :timeline method to the render-page multimethod.
;; The timeline takes no parameters.
;; It displays a tweet editor and a timeline.
(deftest timeline-page-1
  (let [host {:is-host true}
        ui (route/render-page [:timeline] host)]
    ;; Tweet editor
    (is (= (rq/find ui {:elem tweets/tweet-editor :attr :is-host}) [true]))
    ;; Timeline
    (is (= (rq/find ui {:elem tl/timeline :attr :is-host}) [true]))))

;; The page is routed to /timeline
(deftest timeline-page-2
  (reset! route/page nil)
  (secretary/dispatch! "/timeline")
  (is (= @route/page [:timeline])))
