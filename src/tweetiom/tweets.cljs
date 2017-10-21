(ns tweetiom.tweets
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [tweetiom.users :as users]
            [tweetiom.like :as like]
            [tweetiom.panel :as panel])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

(defview tweet-view [user]
  [:tweetiom/tweet user tweet ts])

(defn tweet-editor [host]
  (let [{:keys [add]} (meta (tweet-view host (user host)))
        tweet-text (atom "")]
    (fn [host]
      [:div
       [:input {:value @tweet-text
                :on-change #(reset! tweet-text (.-target.value %))}]
       [:button {:on-click #(add {:tweet [:tweet @tweet-text]
                                  :ts ((:time host))})} "Tweet"]])))

(defmulti tweet-display first)

(defn action-pane [host record]
  (fn [host record]
    (let [{:keys [author ts tweet del!]} record
          tweets (tweet-view host (user host))
          {:keys [add]} (meta tweets)
          config [[[:div "Reply"] (panel/input-box #(add {:tweet [:reply [author ts] %]
                                                          :ts ((:time host))}) "Reply")]
                  [[:div "Retweet"] (panel/input-box #(add {:tweet [:retweet [author ts] tweet %]
                                                            :ts ((:time host))}) "Retweet")]
                  [[:div "Like"] (fn []
                                   (let [likes (like/like-view host author ts (user host))
                                         {:keys [add]} (meta likes)]
                                     (cond (empty? likes)
                                           (add {})
                                           :else
                                           (let [{:keys [del!]} (first likes)]
                                             (del!)))))]]
          config (cond (= author (user host))
                       (conj config [[:div "Delete"] del!])
                       :else config)]
      [panel/action-pane (seq config)])))

(defn tweet-viewer [host {:keys [tweet author ts] :as record}]
  [:div
   [:div {:class "tweet-container"}
    [tweet-display tweet]]
   [:div {:class "tweet-action-pane-container"}
    [action-pane host record]]])

(defmethod tweet-display :text [[_ text]]
  [:span {:class "tweet-text"}
   text])

(defn tweet-link [author ts link-content])

(defmethod tweet-display :reply [[_ [user ts] reply]]
  [:div
   [:span {:class "tweet-details"}
    "In reply to " [tweet-link user ts "this tweet"] " by " [users/user-link user]]
   [:span {:class "tweet-text"}
    reply]])


