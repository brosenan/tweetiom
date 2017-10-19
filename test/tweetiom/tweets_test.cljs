(ns tweetiom.tweets-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.tweets :as tweets]
            [tweetiom.users :as users]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

;;;;;; Tweet Editor ;;;;;;;;;

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
(deftest tweets-editor-3
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

;;;;;; Tweet Viewer ;;;;;;;;;

;; Given a record containing a tweet, with the following fields:
;;  :author - the user creating the tweet
;;  :ts - the tweet's timestamp
;;  :tweet - a vector representing the tweet, where the first element is a keyword identifying the kind of tweet this is;
;; tweet-viewer returns a UI container containing the tweet's content, and tweet-action-pane displaying actions related to this tweet.
(deftest tweet-viewer-1
  (let [host (ax/mock-connection "alice")
        ui (tweets/tweet-viewer host {:author "bob"
                                      :ts 1234
                                      :tweet [:some-tweet-type 1 2 3]})]
    (is (= (rq/find ui :.tweet-container) [[tweets/tweet-display [:some-tweet-type 1 2 3]]]))
    (is (= (rq/find ui :.tweet-action-pane-container) [[tweets/action-pane host "bob" 1234]]))))


;;;;;; Action Pane ;;;;;;;;;
;; The action pane is a toolbar containing buttons that perform tweet-related operations: Reply, retweet, like and share (link).
;; If the user viewing the tweet is also its author, there is also a delete button.
;; The action pane also contains a :div that is filled with contents based on the selected action.
;; For example, if we retweet or reply, the :div is filled with an input box and confirmation/cancelation buttons.
;; If we share, the :div is filled with an input box containing the link and a dismiss button,
;; delete operations open a confirmation dialog, and "like" does not open anything and just "likes".

;;;;;; Tweet Display ;;;;;;;;;
;; tweet-display is a multimethod that allows different kinds of tweets to be rendered differently.

;; A :text tweet is displayed as a :span with the class .tweet-text
(deftest tweet-display-text
  (let [ui (tweets/tweet-display [:text "foo bar"])]
    (is (= (rq/find ui :span.tweet-text) ["foo bar"]))))

;; A :reply tweet is rendered with a :span similar to a :text tweet,
;; but in addition has a .tweet-details :span with the text "in reply to..." with the name of the user to which we reply
(deftest tweet-display-reply
  (let [ui (tweets/tweet-display [:reply ["bob" 1234] "bar foo"])]
    (is (= (rq/find ui :span.tweet-text) ["bar foo"]))
    (is (= (rq/find ui :span.tweet-details) ["In reply to " [tweets/tweet-link "bob" 1234 "this tweet"] " by " [users/user-link "bob"]]))))
