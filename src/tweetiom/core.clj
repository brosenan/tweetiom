(ns tweetiom.core
  (:require [permacode.core :as perm]
            [tweetiom.time :as time]
            [tweetiom.replies :as replies]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

(perm/pure
 (clg/defclause tl-self
   [:tweetiom/timeline user t-from t-to -> user tweet ts]
   (when-not (> (- t-to t-from) 20))
   (for [time-slot (range t-from t-to)])
   [time/timed-tweet [user time-slot] tweet ts])

 (clg/defclause single-tweet
   [:tweetiom/single-tweet user ts -> tweet]
   [:tweetiom/tweet user tweet ts] (clg/by user))

 (clg/defclause tweet-replies
   [:tweetiom/replies orig-author orig-ts -> author tweet ts]
   [replies/tweet-replies [orig-author orig-ts] author tweet ts])

 (clg/defclause user-tweets
   [:tweetiom/user-tweets user -> tweet ts]
   [:tweetiom/tweet user tweet ts] (clg/by user)))

