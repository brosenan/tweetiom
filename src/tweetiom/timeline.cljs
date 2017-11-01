(ns tweetiom.timeline
  (:require [axiom-cljs.core :as ax]
            [reagent.core :as r]
            [tweetiom.tweets :as tweets]
            [tweetiom.routing :as route])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]
                   [secretary.core :refer [defroute]]))


(defn floor [x]
  (.floor js/Math x))

(defn time-range-display [params]
  (let [{:keys [time-aggregation initial-range]
         :or {time-aggregation (* 1000 60 60 24)
              initial-range 7}} params
        curr (-> (js/Date.)
                 .getTime
                 (/ time-aggregation)
                 floor)
        ranges (r/atom [[(inc (- curr initial-range)) (inc curr)]])]
    (fn [params]
      (let [{:keys [query render wrap
                    time-aggregation
                    range-increment increment-btn-caption]
             :or {time-aggregation time-aggregation
                  range-increment 3
                  increment-btn-caption "Get older"}} params]
        (-> (apply concat
                   (for [[from to] @ranges]
                     (for [elem (query from to)]
                       (do
                         (render elem)))))
            (concat [[:button.get-older {:key :get-older-btn
                                         :on-click #(let [[from' to'] (last @ranges)
                                                           to from'
                                                           from (- from' range-increment)]
                                                       (swap! ranges conj [from to]))}
                      increment-btn-caption]])
            wrap)))))

(defquery timeline-query [user t-from t-to]
  [:tweetiom/timeline user t-from t-to -> author tweet ts]
  :store-in (r/atom nil)
  :order-by (- ts))

(defn timeline [host]
  [time-range-display {:wrap (fn [content]
                               [:div.timeline
                                content])
                       :query (partial timeline-query host (user host))
                       :render (fn [{:keys [author ts] :as r}]
                                 ^{:key [author ts]} [tweets/tweet-viewer host r])}])

(defmethod route/render-page :timeline [[_tl] host]
  [:div
   [tweets/tweet-editor host]
   [timeline host]])

(defmethod route/render-page :default [_ host]
  (route/render-page [:timeline] host))

(defroute "/timeline" []
  (route/navigate :timeline))

(defroute "/" []
  (route/navigate :timeline))
