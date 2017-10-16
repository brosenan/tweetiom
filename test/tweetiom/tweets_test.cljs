(ns tweetiom.tweets-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.tweets :as tweets]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

;; The tweet editor contains an :input box and a "Tweet" :button.
(deftest tweet-editor-1
  (let [host (ax/mock-connection "alice")
        ui (tweets/tweet-editor host)]
    (is (= (rq/find ui :input:value) [""]))
    (is (= (rq/find ui :button) ["Tweet"]))))

;; The value of the :input box is bound with the tweet-text atom
(deftest tweet-editor-2
  (let [host (ax/mock-connection "alice")]
    (reset! tweets/tweet-text "some-text")
    (is (= (-> (tweets/tweet-editor host)
               (rq/find :input:value)) ["some-text"]))
    ;; If we invoke the :on-change callback we cause the tweet-text atom to change
    (let [[on-change] (-> (tweets/tweet-editor host)
                          (rq/find :input:on-change))]
      (on-change (rq/mock-change-event "some-other-text"))
      (is (= @tweets/tweet-text "some-other-text")))))

;; Clicking the Tweet :button creates a new tweet in the tweet-view
(deftest tweets-view-3
  (let [host (-> (ax/mock-connection "alice")
                 (assoc :time (constantly 12345)))]
    (let [[on-click] (-> (tweets/tweet-editor host)
                         (rq/find :button:on-click))]
      (reset! tweets/tweet-text "the-tweet-to-submit")
      (on-click)
      (let [tweets (tweets/tweet-view host "alice")]
        ;; The tweet itself is the term [:tweet T], where T is the tweet's text
        (is (= (map :tweet tweets) [[:tweet "the-tweet-to-submit"]]))
        ;; The tweet's :ts field is a timestamp
        (is (= (map :ts tweets) [12345]))))))
