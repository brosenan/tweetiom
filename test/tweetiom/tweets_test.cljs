(ns tweetiom.tweets-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.tweets :as tweets]
            [tweetiom.users :as users]
            [tweetiom.panel :as panel]
            [tweetiom.like :as like]
            [tweetiom.routing :as route]
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
                :tweet [:text "hi there"]}
        ui-func (tweets/action-pane host record)
        ui (ui-func host record)
        [config] (rq/query ui {:elem panel/action-pane})]
    (is (= (count config) 4))
    (let [[delete-btn delete-func] (last config)]
      ;; delete-func is a function that does nothing
      (delete-func)
      ;; The delete-btn is a tweets/delete-btn component
      (is (= delete-btn [tweets/delete-btn host "alice" 1234])))))


;;;;;; Tweet Display ;;;;;;;;;
;; tweet-display is a multimethod that allows different kinds of tweets to be rendered differently.

;; A :text tweet is displayed as a :div with the class .tweet-text
(deftest tweet-display-text
  (let [ui (tweets/tweet-display [:text "foo bar"])]
    (is (= (rq/find ui :div.tweet-text) ["foo bar"]))))

;; A :reply tweet is rendered with a :div similar to a :text tweet,
;; but in addition has a .tweet-details :div with the text "in reply to..." with the name of the user to which we reply
(deftest tweet-display-reply
  (let [ui (tweets/tweet-display [:reply ["bob" 1234] "bar foo"])]
    (is (= (rq/find ui :div.tweet-text) ["bar foo"]))
    (is (= (rq/find ui :div.tweet-details) ["In reply to " [tweets/tweet-link "bob" 1234 "this tweet"] " by " [users/user-link "bob"]]))))

;; A :retweet with an empty comment display "<user name> retweeted:" in a :div, and the original tweet.
(deftest tweet-display-retweet-1
  (let [ui (tweets/tweet-display [:retweet ["bob" 1234] [:text "foo bar"] ""])]
    (is (= (rq/find ui :.retweet-original :div.tweet-text) ["foo bar"]))
    (is (= (rq/find ui :div.tweet-details) [[users/user-link "bob"] " " [tweets/tweet-link "bob" 1234 "retweeted:"]]))
    ;; The empty comment is not displayed
    (is (= (rq/find ui :div.comment) []))))

;; If the retweet has a non-empty comment, this comment is displayed, in addition to the original tweet
(deftest tweet-display-retweet-2
  (let [ui (tweets/tweet-display [:retweet ["bob" 1234] [:text "foo bar"] "tar"])]
    (is (= (rq/find ui :div.comment) ["tar"]))
    (is (= (rq/find ui :.retweet-original :div.tweet-text) ["foo bar"]))))

;;;;;; delete-btn ;;;;;;;;;
;; delete-btn is a component function that takes an author name and a timestamp,
;; and assuming a tweet matching these details exists, displays a button.
(deftest delete-btn-1
  (let [host (ax/mock-connection "alice")
        deletes (tweets/tweet-del-view host "alice" 1234)
        {:keys [add]} (meta deletes)]
    ;; Create one tweet to delete
    (add {})
    ;; Now we should see a delete button.
    (let [ui (tweets/delete-btn host "alice" 1234)
          [del] (rq/find ui :button.delete-tweet:on-click)]
      ;; Clicking it removes the tweet
      (del)
      (is (= (count (tweets/tweet-del-view host "alice" 1234)) 0)))))

;;;;;; Tweet Link ;;;;;;;;;
;; A tweet link presents a link to a tweet, with the given text
(deftest tweet-link-1
  (let [ui (tweets/tweet-link "alice" 1234 "this tweet")]
    ;; The caption is the one given as parameter
    (is (= (rq/find ui :a) ["this tweet"]))
    ;; The link is to a page corresponding with the given tweet
    (is (= (rq/find ui :a:href) ["#/tweet/alice/1234"]))))

;;;;;; Tweet Page ;;;;;;;;;
;; The tweet page shows one tweet and all its replies.
;; It is invoked when @page gets a value of [:tweet user ts], where user and ts uniquely identify a tweet.
(deftest tweet-page-1
  (is (= (route/render-page [:tweet "bob" 1234] :host) [tweets/tweet-page :host "bob" 1234])))

;; The tweet page consists of one tweet viewer for the tweet itself, and its replies.
(deftest tweet-page-1
  (let [host (ax/mock-connection "alice")
        tweets (tweets/single-tweet-view host "bob" 1234)
        {:keys [add]} (meta tweets)
        reply-mock (ax/query-mock host :tweetiom/tweet-replies)]
    ;; Create the tweet
    (add {:tweet [:text "foo bar"]})
    ;; The .main-tweet now has our tweet
    (let [ui (tweets/tweet-page host "bob" 1234)
          [rec] (rq/find ui :.main-tweet {:elem tweets/tweet-viewer})]
      (is (= (:tweet rec) [:text "foo bar"])))
    ;; Let's create two replies
    (reply-mock ["bob" 1234] ["alice" [:text "foo"] 3000])
    (reply-mock ["bob" 1234] ["charlie" [:text "bar"] 4000])
    ;; We should see tweet-viewers for both tweets, in descending time order
    (let [ui (tweets/tweet-page host "bob" 1234)
          [rec1 rec2] (rq/find ui :.replies {:elem tweets/tweet-viewer})]
      (is (= (:tweet rec1) [:text "bar"]))
      (is (= (:tweet rec2) [:text "foo"])))
    ))
