(ns tweetiom.core
  (:require [permacode.core :as perm]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

;; To be able to deploy this code on axiom, it needs to be wrapped with a perm/pure.
;; This macro restricts the underlying code to a pure subset of Clojure.
;; See http://axiom-clj.org/permacode.html for more information.
(perm/pure

 ;; This code is written in the Cloudlog DSL.
 ;; See http://axiom-clj.org/cloudlog.html for more information.

 ;; Rules work at update time.
 (clg/defrule task-where-user-is-mentioned [user author task ts]
   ;; For every task created by an author,
   [:tweetiom/task author task ts] (clg/by author)
   ;; For every pattern of the form @user in the text,
   (for [user (re-seq #"@[a-zA-Z0-9]+" task)])
   ;; Remove the first character to get the user name.
   (let [user (subs user 1)]))

 ;; Clauses work at query time.
 ;; Each clause can create different kind of answer to the same question.
 ;; This clause answers the question of 'what are my tasks' with tasks I created myself.
 (clg/defclause tasks-i-wrote
   [:tweetiom/my-tasks user -> user task ts]
   [:tweetiom/task user task ts] (clg/by user))

 ;; This clause answers this question with tasks I was mentioned in
 (clg/defclause tasks-i-was-mentioned-in
   [:tweetiom/my-tasks user -> author task ts]
   [task-where-user-is-mentioned user author task ts]))

