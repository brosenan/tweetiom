(ns tweetiom.users-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.users :as users]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

;;;;;; User Link ;;;;;;;;;
;; A user link is a a link displaying a user ID, linking to a hash location depicting this user.
(deftest user-link-1
  (let [ui (users/user-link "alice")]
    ;; The content of the user link is @<user id>
    (is (= (rq/find ui :a) ["@alice"]))
    ;; The link is to #user/<user>
    (is (= (rq/find ui :a:href) ["#/user/alice"]))))
