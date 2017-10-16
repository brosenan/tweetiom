(ns tweetiom.tweets
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

(defonce tweet-text (r/atom ""))

(defview tweet-view [user]
  [:tweetiom/tweet user tweet ts])

(defn tweet-editor [host]
  (let [{:keys [add]} (meta (tweet-view host (user host)))]
    [:div
     [:input {:value @tweet-text
              :on-change #(reset! tweet-text (.-target.value %))}]
     [:button {:on-click #(add {:tweet [:tweet @tweet-text]
                                :ts ((:time host))})} "Tweet"]]))
