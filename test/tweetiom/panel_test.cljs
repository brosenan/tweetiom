(ns tweetiom.panel-test
  (:require [cljs.test :refer-macros [is testing deftest]]
            [tweetiom.panel :as panel]
            [reagent-query.core :as rq]))

;;;;;; Action Pane ;;;;;;;;;
;; action-pane creates a generic action-pane, based on the given parameters.
;; An action pane has two parts:
;; - An .action-pane-toolbar containing buttons, andz
;; - An .action-pane-dialog for displaying UI specific for the selected action.
;; The action-pane is stateful, and therefore the function returns a function that re-draws the UI each time.

;; The input to action-pane is a sequence of 2-tuples, each containing:
;; - The UI component to display as button, and
;; - A function to be called when the button is clicked
(deftest action-pane-1
  (let [clicked (atom nil)
        config [["Btn1" #(reset! clicked :btn1)]
                ["Btn2" #(reset! clicked :btn2)]]
        panel-func (panel/action-pane config)]
    ;; The content of each button...
    (is (= (rq/find (panel-func config) :.action-pane-toolbar :.action-pane-button)
           ["Btn1" "Btn2"]))
    ;; Let's click Btn1
    (let [[btn1 btn2] (rq/find (panel-func config) :.action-pane-toolbar :.action-pane-button:on-click)]
      (btn1)
      (is (= @clicked :btn1)))))

;; The callback takes a single parameter: an atom.
;; If the callback sets this atom to contain some UI, this UI will appear in the .action-pane-dialog
(deftest action-pane-2
  (let [config [["My Button" (fn [ui-atom]
                               (reset! ui-atom [:some "UI"]))]]
        panel-func (panel/action-pane config)]
    (let [[btn] (rq/find (panel-func config) :.action-pane-toolbar :.action-pane-button:on-click)]
      (btn)
      (is (= (rq/find (panel-func config) :.action-pane-dialog)
             [[:some "UI"]])))))

;;;;;; dialog-button ;;;;;;;;;
;; dialog-button is desinged to be used with action-pane, to create buttons that open dialogs.
;; It is a higher-order function: it takes a function as parameter and it returns a function.
;; To use it, you need to define a function that, given a 'close' function (a function that closes the dialog)
;; returns the dialog's UI.
;; dialog-button will return a function you can provide to action-pane.

;; For example, if you want to have an action-pane button that displays a message and a 'close' button, you can do the following:
(deftest dialog-button-1
  (let [config [["Display Message" (panel/dialog-button (fn [close] [:div
                                                                     [:span "Hello, World"]
                                                                     [:button {:on-click close} "Close"]]))]]
        panel-func (panel/action-pane config)]
    ;; Now, when we click the button...
    (let [[btn] (rq/find (panel-func config) :.action-pane-button:on-click)]
      (btn))
    ;; ... we should see the dialog in the .action-pane-dialog
    (is (= (rq/find (panel-func config) :.action-pane-dialog :div :span)
           ["Hello, World"]))
    (is (= (rq/find (panel-func config) :.action-pane-dialog :div :button)
           ["Close"]))
    ;; And if we click the "Close" button, the dialog should go away
    (let [[btn] (rq/find (panel-func config) :.action-pane-dialog :div :button:on-click)]
      (btn))
    (is (= (rq/find (panel-func config) :.action-pane-dialog) [nil]))))
