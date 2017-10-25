(ns tweetiom.routing-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.routing :as route]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

;;;;;; Navigate ;;;;;;;;;
;; navigate is inteneded to complete Secretary's defroute macro, by causing a navigation to a page.
;; It takes one or more parameters.
;; The first parameter is a keyword representing the path to navigate to.
;; The other parameters are positional parameters extracted from the route, that will be later given to the page rendering function.
(deftest navigate-1
  (route/navigate :foo 1 2 3)
  ;; The vector of all arguments is placed in the page atom
  (is (= @route/page [:foo 1 2 3])))


;;;;;; current-page ;;;;;;;;;
;; Dispatching is done through the multi-method current-page.
;; current-page takes the content of the page atom, and renders a page.
;; Dispatch is done on the first element -- the keyword identifying the page.
;; Methods may expect a second argument, intended as context to be passed from the application level.
;; Typically, this is going to be the Axiom host map.

;; For example:
(defmethod route/current-page :foo [[_foo a b c] ctx]
  [:div "This is foo: " a b c ctx])
(defmethod route/current-page :bar [[_bar a b c] ctx]
  [:div "This is bar: " a b c])

;; Now, displaying the current page should dispatch on the keyword.
(deftest current-page-1
  (is (= (route/current-page [:foo 1 2 3] :ctx) [:div "This is foo: " 1 2 3 :ctx]))
  (is (= (route/current-page [:bar 3 2 1] :ctx) [:div "This is bar: " 3 2 1])))

;;;;;; Route ;;;;;;;;;
;; The route function puts this all together.
;; It takes the context as parameter, and calls current-page with the contents of @page and the context.
(deftest route-1
  (route/navigate :foo "I" "<3" "Tweetiom")
  (is (= (route/route :my-ctx) [:div "This is foo: " "I" "<3" "Tweetiom" :my-ctx])))

