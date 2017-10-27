(ns tweetiom.users
  (:require [secretary.core :as secretary :refer-macros [defroute]]))

(defroute user-route "/user/:user" [user])

(defn user-link [user]
  [:a {:href (str "#" (user-route {:user user}))} (str "@" user)])
