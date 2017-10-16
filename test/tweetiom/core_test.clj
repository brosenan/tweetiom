(ns tweetiom.core-test
  (:require [midje.sweet :refer :all]
            [tweetiom.core :refer :all]
            [cloudlog-events.testing :refer [scenario as emit query apply-rules]]))

(fact
 ;; This test is written in the cloudlog testing DSL.
 ;; See http://axiom-clj.org/cloudlog-events.testing.html for more details.
 (scenario
  (as "alice"
      (emit [:tweetiom/task "alice" "Create app" 1000])
      (emit [:tweetiom/task "alice" "Show app to @bob" 2000])
      (query [:tweetiom/my-tasks "alice"]) => #{["alice" "Create app" 1000]
                                              ["alice" "Show app to @bob" 2000]})
  (apply-rules [:tweetiom.core/task-where-user-is-mentioned "bob"])
  => #{["alice" "Show app to @bob" 2000]}
  (as "bob"
      (query [:tweetiom/my-tasks "bob"]) => #{["alice" "Show app to @bob" 2000]})))
