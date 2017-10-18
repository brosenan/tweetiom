(ns tweetiom.tweets-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.tweets :as tweets]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

;; The tweet-editor function returns a ui function that renders an :input box and a "Tweet" :button.
(deftest tweet-editor-1
  (let [host (ax/mock-connection "alice")
        ui (tweets/tweet-editor host)]
    (is (= (rq/find (ui host) :input:value) [""]))
    (is (= (rq/find (ui host) :button) ["Tweet"]))))

;; The :input box's :on-change callback updates its :value
(deftest tweet-editor-2
  (let [host (ax/mock-connection "alice")
        ui (tweets/tweet-editor host)]
    ;; If we invoke the :on-change callback we cause the tweet-text atom to change
    (let [[on-change] (rq/find (ui host) :input:on-change)]
      (on-change (rq/mock-change-event "some-text"))
      (is (= (rq/find (ui host) :input:value) ["some-text"])))))

;; Clicking the Tweet :button creates a new tweet in the tweet-view
(deftest tweets-view-3
  (let [host (-> (ax/mock-connection "alice")
                 (assoc :time (constantly 12345)))]
    (let [ui (tweets/tweet-editor host)
          [on-change] (rq/find (ui host) :input:on-change)
          [on-click] (rq/find (ui host) :button:on-click)]
      (on-change (rq/mock-change-event "the-tweet-to-submit"))
      (on-click)
      (let [tweets (tweets/tweet-view host "alice")]
        ;; The tweet itself is the term [:tweet T], where T is the tweet's text
        (is (= (map :tweet tweets) [[:tweet "the-tweet-to-submit"]]))
        ;; The tweet's :ts field is a timestamp
        (is (= (map :ts tweets) [12345]))))))
