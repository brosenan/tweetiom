(ns tweetiom.user-page
  (:require [axiom-cljs.core :as ax]
            [reagent.core :as r]
            [tweetiom.tweets :as tweets]
            [tweetiom.routing :as route])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

(defquery user-tweets-query [u]
  [:tweetiom/user-tweets u -> tweet ts]
  :store-in (r/atom nil)
  :order-by (- ts))

(defn user-page [host u]
  [:div
   [:div.title (str "@" u)]
   (for [tweet (user-tweets-query host u)]
     ^{:key (:ts tweet)} [tweets/tweet-viewer host tweet])])

(defmethod route/render-page :user [[_ user] host]
  [user-page host user])
