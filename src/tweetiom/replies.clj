(ns tweetiom.replies
  (:require [permacode.core :as perm]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

(perm/pure
 (clg/defrule tweet-replies [[orig-author orig-ts] author [:text reply] ts]
   [:tweetiom/tweet author [:reply [orig-author orig-ts] reply] ts]  (clg/by author)))
