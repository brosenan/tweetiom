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
    ;; Each with a unique ID
    (is (= (rq/find (panel-func config) :.action-pane-toolbar :.action-pane-button:key)
           [0 1]))
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

;;;;;; input-box-dlg ;;;;;;;;;
;; An input-box-dlg is a control that contains an :input box, a primary (OK) button and a secondary (Cancel) button.
;; It takes the following parameters:
;; - func: A function to be called when the primary button is clicked. It takes the value of the :input box as parameter.
;; - btn-caption: The caption for the primary button
;; - close: A function that closes the dialog.
;; The input-box-dlg function returns a function that renders the UI.
(deftest input-box-dlg-1
  (let [result (atom nil)
        closed (atom 0)
        func #(reset! result %)
        caption "Do it!"
        close #(swap! closed inc)
        ui-func (panel/input-box-dlg func caption close)
        ui #(ui-func func caption close)]
    ;; The UI contains an :input box with empty text
    (is (= (rq/find (ui) :input:value) [""]))
    ;; Editing the :input box persists
    (let [[on-change] (rq/find (ui) :input:on-change)]
      (on-change (rq/mock-change-event "some text")))
    (is (= (rq/find (ui) :input:value) ["some text"]))
    ;; The primary :button displays the given caption
    (is (= (rq/find (ui) :button.btn.btn-primary) ["Do it!"]))
    ;; Clicking it will call func with the string we placed in the :input box
    (let [[ok] (rq/find (ui) :button.btn.btn-primary:on-click)]
      (ok))
    (is (= @result "some text"))
    ;; ... and calls close
    (is (= @closed 1))
    ;; The secondary button has the caption Cancel
    (is (= (rq/find (ui) :button.btn.btn-secondary) ["Cancel"]))
    ;; Clicking it calls close.
    (let [[cancel] (rq/find (ui) :button.btn.btn-secondary:on-click)]
      (cancel))
    (is (= @closed 2))))

;;;;;; input-box ;;;;;;;;;
;; input-box is a function that specializes dialog-button by creating an input-box-dlg as the dialog.
;; It takes as parameter a callback function to be called when a value is provided, and a caption for the OK button.
;; And when required, creates an input-box-dlg.
(deftest input-box-1
  (let [result (atom nil)
        config [["Display Input Box" (panel/input-box :some-func "Go for It!")]]
        panel-func (panel/action-pane config)]
    ;; Now, when we click the button...
    (let [[btn] (rq/find (panel-func config) :.action-pane-button:on-click)]
      (btn))
    ;; ... we should an input box in the .action-pane-dialog
    (let [[func caption close] (rq/find (panel-func config) :.action-pane-dialog {:elem panel/input-box-dlg})]
      ;; func is the function we provided
      (is (= func :some-func))
      ;; and caption is the caption we provided
      (is (= caption "Go for It!"))
      ;; close is a function that closes the dialog
      (close)
      (is (= (rq/find (panel-func config) :.action-pane-dialog {:elem panel/input-box-dlg}) []))
      )
    (comment (is (= 
                  []))
             (is (= (rq/find (panel-func config) :.action-pane-dialog :div :button.btn.btn-primary)
                    ["Go for It!"]))
             (is (= (rq/find (panel-func config) :.action-pane-dialog :div :button.btn.btn-secondary)
                    ["Cancel"]))
             ;; If we edit the :input box and click the .btn-primary, the callback is called
             (let [[on-change] (rq/find (panel-func config) :.action-pane-dialog :div :input:on-change)
                   [ok] (rq/find (panel-func config) :button.btn.btn-primary:on-click)]
               (on-change (rq/mock-change-event "some text"))
               (ok))
             (is (= @result "some text"))
             ;; The button should close the dialog
             (is (= (rq/find (panel-func config) :.action-pane-dialog :div :button.btn.btn-primary)
                    [])))))

(comment ;; The Cancel button closes the dialog without invoking the callback
  (deftest input-box-2
    (let [result (atom nil)
          config [["Display Input Box" (panel/input-box #(reset! result %) "Go for It!")]]
          panel-func (panel/action-pane config)]
      (let [[btn] (rq/find (panel-func config) :.action-pane-button:on-click)]
        (btn))
      (let [[on-change] (rq/find (panel-func config) :.action-pane-dialog :div :input:on-change)
            [cancel] (rq/find (panel-func config) :button.btn.btn-secondary:on-click)]
        (on-change (rq/mock-change-event "some text"))
        (cancel))
      ;; The result is unchanged
      (is (= @result nil))
      ;; The dialog is closed
      (is (= (rq/find (panel-func config) :.action-pane-dialog :div :button.btn.btn-primary)
             [])))))

