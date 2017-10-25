(ns tweetiom.time
  (:require [permacode.core :as perm]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

(perm/pure
 ;; Quant is a day
 (def quant (* 1000 60 60 24))

 (clg/defrule timed-tweet [[author time-slot] tweet ts]
   [:tweetiom/tweet author tweet ts] (clg/by author)
   (let [time-slot (quot ts quant)])))
