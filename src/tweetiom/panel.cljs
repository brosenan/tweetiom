(ns tweetiom.panel
  (:require [reagent.core :as r]))

(defn action-pane [tuples]
  (let [dialog (r/atom nil)]
    (fn [tuples]
      [:div {:class "action-pane"}
       [:div {:class "action-pane-toolbar"}
        (for [[i [content func]] (map-indexed vector tuples)]
          [:div {:class "action-pane-button"
                 :key i
                 :on-click #(func dialog)}
           content])]
       [:div {:class "action-pane-dialog"}
         @dialog]])))

(defn dialog-button [func]
  (fn [dialog]
    (let [close #(reset! dialog nil)]
      (reset! dialog (func close)))))

(defn input-box-dlg [func btn-caption close]
  (let [content (r/atom "")]
    (fn []
      [:div.input-box
       [:input {:value @content
                :on-change #(reset! content (.-target.value %))}]
       [:button.btn.btn-primary {:on-click #(do
                                              (func @content)
                                              (close))} btn-caption]
       [:button.btn.btn-secondary {:on-click close} "Cancel"]])))

(defn input-box [func btn-caption]
  (let [content (r/atom "")]
    (dialog-button (fn [close]
                     [input-box-dlg func btn-caption close]))))
