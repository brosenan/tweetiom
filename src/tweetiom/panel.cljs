(ns tweetiom.panel
  (:require [reagent.core :as r]))

(defn action-pane [tuples]
  (let [dialog (r/atom nil)]
    (fn [tuples]
      [:div {:class "action-pane"}
       [:div {:class "action-pane-toolbar"}
        (for [[content func] tuples]
          [:div {:class "action-pane-button"
                 :on-click #(func dialog)}
           content])]
       [:div {:class "action-pane-dialog"}
         @dialog]])))

(defn dialog-button [func]
  (fn [dialog]
    (let [close #(reset! dialog nil)]
      (reset! dialog (func close)))))
