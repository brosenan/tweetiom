(ns tweetiom.core
  (:require [permacode.core :as perm]
            [tweetiom.time :as time]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

(perm/pure
 (clg/defclause tl-self
   [:tweetiom/timeline user t-from t-to -> user tweet ts]
   (when-not (> (- t-to t-from) 20))
   (for [time-slot (range t-from t-to)])
   [time/timed-tweet [user time-slot] tweet ts]))

