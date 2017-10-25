(ns tweetiom.like
  (:require [axiom-cljs.core :as ax]
            [reagent.core :as r])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))


(defview like-view [author ts user]
  [:tweetiom/like [author ts] user]
  :store-in (r/atom nil))


