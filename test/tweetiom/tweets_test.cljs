(ns tweetiom.tweets-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.tweets :as tweets]
            [tweetiom.users :as users]
            [tweetiom.panel :as panel]
            [tweetiom.like :as like]
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
        (is (= (map :tweet tweets) [[:text "the-tweet-to-submit"]]))
        ;; The tweet's :ts field is a timestamp
        (is (= (map :ts tweets) [12345])))
      ;; It clears the field
      (is (= (rq/find (ui host) :input:value) [""])))))

;;;;;; Tweet Viewer ;;;;;;;;;

;; Given a record containing a tweet, with the following fields:
;;  :author - the user creating the tweet
;;  :ts - the tweet's timestamp
;;  :tweet - a vector representing the tweet, where the first element is a keyword identifying the kind of tweet this is;
;; tweet-viewer returns a UI container containing the tweet's content, and tweet-action-pane displaying actions related to this tweet.
(deftest tweet-viewer-1
  (let [host (ax/mock-connection "alice")
        record {:author "bob"
                :ts 1234
                :tweet [:some-tweet-type 1 2 3]}
        ui (tweets/tweet-viewer host record)]
    (is (= (rq/find ui :.tweet-container) [[tweets/tweet-display [:some-tweet-type 1 2 3]]]))
    (is (= (rq/find ui :.tweet-action-pane-container) [[tweets/action-pane host record]]))))


;;;;;; Action Pane ;;;;;;;;;
;; The action pane is a toolbar containing buttons that perform tweet-related operations: Reply, retweet and like.
;; The action pane also contains a :div that is filled with contents based on the selected action.
;; For example, if we retweet or reply, the :div is filled with an input box and confirmation/cancelation buttons.
(deftest action-pane-1
  (let [host (-> (ax/mock-connection "alice")
                 (assoc :time (constantly 5555)))
        record {:author "bob"
                :ts 1234
                :tweet [:text "foo bar"]}
        ui-func (tweets/action-pane host record)
        ui (ui-func host record)
        [config] (rq/query ui {:elem panel/action-pane})]
    (is (seq? config))
    (is (= (count config) 3))
    (let [[[reply-btn reply-func]
           [retweet-btn retweet-func]
           [like-btn like-func]] config
          dialog (atom nil)]
      ;; Reply
      (reply-func dialog)
      ;; When we write a reply and post it, a new reply tweet is created.
      (let [[func caption close] (rq/find @dialog {:elem panel/input-box-dlg})]
        (is (= caption "Reply"))
        (func "some reply"))
      ;; Now the reply should appear in the tweet-view
      (let [[{:keys [tweet ts del!]}] (tweets/tweet-view host "alice")]
        (is (= tweet [:reply ["bob" 1234] "some reply"]))
        (is (= ts 5555))
        (del!)) ;; Cleanup

      ;; Retweet
      (retweet-func dialog)
      ;; A dialog opens, we write a comment and press OK
      (let [[func caption close] (rq/find @dialog {:elem panel/input-box-dlg})]
        (func "some comment"))
      ;; Now the retweet should appear in the tweet-view
      (let [[{:keys [tweet ts del!]}] (tweets/tweet-view host "alice")]
        (is (= tweet [:retweet ["bob" 1234] [:text "foo bar"] "some comment"]))
        (is (= ts 5555))
        (del!))

      ;; Like
      (like-func dialog)
      ;; The like button does not open a dialog.
      ;; It just creates a like in the like-view.
      (is (= (count (like/like-view host "bob" 1234 "alice")) 1))
      ;; If the button is clicked again, the like is removed.
      (like-func dialog)
      (is (= (count (like/like-view host "bob" 1234 "alice")) 0)))))

;; If the user viewing the tweet is also its author, there is also a delete button, that deletes a tweet.
(deftest action-pane-2
  (let [host (-> (ax/mock-connection "alice")
                 (assoc :time (constantly 5555)))
        deleted (atom false)
        record {:author "alice"
                :ts 1234
                :tweet [:text "hi there"]
                :del! #(reset! deleted true)}
        ui-func (tweets/action-pane host record)
        ui (ui-func host record)
        [config] (rq/query ui {:elem panel/action-pane})]
    (is (= (count config) 4))
    (let [[delete-btn delete-func] (last config)
          dialog (atom nil)]
      ;; Let's hit the delete button...
      (delete-func dialog)
      ;; It should call the record's del! method
      (is @deleted))))


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

;; A :retweet with an empty comment display "<user name> retweeted:" in a :div, and the original tweet.
(deftest tweet-display-retweet-1
  (let [ui (tweets/tweet-display [:retweet ["bob" 1234] [:text "foo bar"] ""])]
    (is (= (rq/find ui :.retweet-original :span.tweet-text) ["foo bar"]))
    (is (= (rq/find ui :span.tweet-details) [[users/user-link "bob"] [tweets/tweet-link "bob" 1234 "retweeted:"]]))))
