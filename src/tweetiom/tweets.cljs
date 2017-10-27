(ns tweetiom.tweets
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [tweetiom.users :as users]
            [tweetiom.like :as like]
            [tweetiom.panel :as panel]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

(defview tweet-view [user]
  [:tweetiom/tweet user tweet ts]
  :store-in (r/atom nil))

(defn tweet-editor [host]
  (let [tweet-text (r/atom "")]
    (fn [host]
      (let [{:keys [add]} (meta (tweet-view host (user host)))]
        [:div
         [:input {:value @tweet-text
                  :on-change #(reset! tweet-text (.-target.value %))}]
         [:button {:on-click #(do
                                (add {:tweet [:text @tweet-text]
                                      :ts ((:time host))})
                                (reset! tweet-text ""))} "Tweet"]]))))

(defmulti tweet-display first)

(defview tweet-del-view [author ts]
  [:tweetiom/tweet author tweet ts]
  :store-in (r/atom nil))

(defn delete-btn [host author ts]
  (let [[{:keys [del!]}] (tweet-del-view host author ts)]
    (when-not (nil? del!)
      [:button.delete-tweet {:on-click del!} "Delete"])))

(defn action-pane [host record]
  (fn [host record]
    (let [{:keys [author ts tweet]} record
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
                       (conj config [[delete-btn host author ts] (fn [])])
                       :else config)]
      [panel/action-pane (seq config)])))

(defn tweet-viewer [host {:keys [tweet author ts] :as record}]
  [:div
   [:div.tweet-container
    [tweet-display tweet]]
   [:div.tweet-action-pane-container
    [action-pane host record]]])

(defmethod tweet-display :text [[_ text]]
  [:div.tweet-text
   text])

(defroute tweet-route "/tweet/:author/:ts" [author ts])

(defn tweet-link [author ts link-content]
  [:a {:href (str "#" (tweet-route {:author author
                                    :ts ts}))} link-content])

(defmethod tweet-display :reply [[_ [user ts] reply]]
  [:div
   [:div.tweet-details
    "In reply to " [tweet-link user ts "this tweet"] " by " [users/user-link user]]
   [:div.tweet-text
    reply]])


(defmethod tweet-display :retweet [[_ [user ts] orig comment]]
  [:div
   [:div.tweet-details
    [users/user-link user] " " [tweet-link user ts "retweeted:"]]
   (when-not (empty? comment)
     [:div.comment
      comment])
   [:div.retweet-original
    (tweet-display orig)]])




