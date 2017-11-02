(ns tweetiom.users-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.users :as users]
            [tweetiom.user-page :as userpage]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]
            [tweetiom.tweets :as tweets]
            [tweetiom.routing :as route]))

;;;;;; User Link ;;;;;;;;;
;; A user link is a a link displaying a user ID, linking to a hash location depicting this user.
(deftest user-link-1
  (let [ui (users/user-link "alice")]
    ;; The content of the user link is @<user id>
    (is (= (rq/find ui :a) ["@alice"]))
    ;; The link is to #user/<user>
    (is (= (rq/find ui :a:href) ["#/user/alice"]))))

;;;;;; User Page ;;;;;;;;;
;; A user's page shows all tweets made by that user.
(deftest user-page-1
  (let [host (ax/mock-connection "alice")
        user-tweets-mock (ax/query-mock host :tweetiom/user-tweets)
        ui (userpage/user-page host "bob")]
    ;; The user name needs to be written in the .title
    (is (= (rq/find ui :.title) ["@bob"]))
    ;; In a .user-tweets container, all the tweets made by this user are displayed
    (user-tweets-mock ["bob"] [[:text "foo"] 12345])
    (user-tweets-mock ["bob"] [[:text "bar"] 23456])
    (let [ui (userpage/user-page host "bob")
          records (rq/find ui {:elem tweets/tweet-viewer})]
      ;; Tweets are displayed in descending order
      (is (= (map :tweet records) [[:text "bar"] [:text "foo"]])))))

;; The user page is displayed when the page atom contains [:user <user-id>]
(deftest user-page-2
  (is (= (route/render-page [:user "bob"] :host) [userpage/user-page :host "bob"])))
