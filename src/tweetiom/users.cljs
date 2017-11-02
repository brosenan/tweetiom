(ns tweetiom.users
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [axiom-cljs.core :as ax]
            [reagent.core :as r]
            [tweetiom.routing :as route])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

(defroute user-route "/user/:user" [user]
  (route/navigate :user user))

(defn user-link [user]
  [:a {:href (str "#" (user-route {:user user}))} (str "@" user)])




