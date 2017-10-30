(ns tweetiom.core-test
  (:require [midje.sweet :refer :all]
            [tweetiom.core :refer :all]
            [tweetiom.time :as time]
            [cloudlog-events.testing :refer [scenario as emit query apply-rules]]))

;;;;;;;;;;;; Time Quantization and Self Tweets ;;;;;;;;;;;;;;;
;; In order to support pagination and not retrieve all tweets at once,
;; we index tweets by user and the time they were introduced.
;; The timed-tweet rule performs this indexing, calculating for each tweet a time-slot,
;; which is its timestamp divided by the time-quant -- an amount of time used for this indexing.
;; Tweets retreived by timed-tweets are limited to one time-slot.
(fact
 (scenario
  (as "alice"
      (emit [:tweetiom/tweet "alice" [:text "no-show"] (+ (* time/quant 2) 7)])
      (emit [:tweetiom/tweet "alice" [:text "hello"] (+ (* time/quant 3) 2)])
      (emit [:tweetiom/tweet "alice" [:text "world"] (+ (* time/quant 3) 5)])
      (emit [:tweetiom/tweet "alice" [:text "something-else"] (+ (* time/quant 4) 0)])
      (emit [:tweetiom/tweet "alice" [:text "no-show"] (+ (* time/quant 5) 0)]))
  (apply-rules [::time/timed-tweet ["alice" 3]])
  => #{[[:text "hello"] (+ (* time/quant 3) 2)]
       [[:text "world"] (+ (* time/quant 3) 5)]}
  ;; Timeline queries return all tweets by the same user, based on the given time-slot range
  (as "alice"
      (query [:tweetiom/timeline "alice" 3 5]) ;; 3 to 5 exclusive
      => #{["alice" [:text "hello"] (+ (* time/quant 3) 2)]
           ["alice" [:text "world"] (+ (* time/quant 3) 5)]
           ["alice" [:text "something-else"] (+ (* time/quant 4) 0)]}
      ;; The same query will return no results if the range is too big (> 20)
      (query [:tweetiom/timeline "alice" 3 24])
      => map?)))

;;;;;;;;;;;; Fetching a Single Tweet ;;;;;;;;;;;;;;;
(fact
 (scenario
  (as "bob"
      (emit [:tweetiom/tweet "bob" [:text "foo"] 12345]))
  (as "alice"
      (query [:tweetiom/single-tweet "bob" 12345]) => #{[[:text "foo"]]})))
