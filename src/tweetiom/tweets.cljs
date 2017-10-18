(ns tweetiom.tweets
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [tweetiom.users :as users])
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

(defn action-pane [host author ts])

(defn tweet-viewer [host {:keys [tweet author ts]}]
  [:div
   [:div {:class "tweet-container"}
    [tweet-display tweet]]
   [:div {:class "tweet-action-pane-container"}
    [action-pane host author ts]]])

(defmethod tweet-display :text [[_ text]]
  [:span {:class "tweet-text"}
   text])

(defmethod tweet-display :reply [[_ [user ts] reply]]
  [:div
   [:span {:class "tweet-details"}
    "In reply to " [users/user-link user]]
   [:span {:class "tweet-text"}
    reply]])


