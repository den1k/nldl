(ns nldl.core
  (:require [instaparse.core :as insta]
            [clojure.string :as str]))

(defn read-clj [s]
  #?(:clj  (read-string s)
     :cljs (cljs.reader/read-string s)))


(def grammar
  "
 <query> = find <whitespace> where

 find = <'find'> <whitespace> vars

 var = #'^(?!find|where)[a-zA-Z-]+'
 <vars> = (<whitespace>? var)*

 where = <'where'> where-clauses

 subject = var
 attr = #'^(?![-])[a-zA-Z-/]+'
 pred = word
 obj = number | word

 where-clause = <whitespace> subject
                <whitespace> attr
                <whitespace> <pred>
                <whitespace> obj

 <where-clauses> = where-clause (((<','> | <'\n'>) where-clause)*)?

  (* regex with lookahead. word can contain dashes but not start with them *)
  <word> = #'^(?![-])[a-zA-Z-]+'
  <words> = (<whitespace>? word)*
  number = #'[\\d|.]+'
  whitespace = #'\\s+'
 "
  )

(insta/defparser nldl-parser grammar)

(def default-transforms
  {:var          (fn [var] (symbol (str "?" var)))
   :attr         keyword
   :number       read-clj
   :subject      identity
   :obj          identity
   :where-clause vector})

(defn nl->dl
  "Natural Language (string) -> Datalog (EDN).

  Parse syntax:
    - `:where` clauses can be seperated by commas or new-lines

  2-arity version takes a map of transforms that can be used to extend or
  replace default transforms.
  For instance, the `:find` clause can be extended to pull fields for a
  variable, with a transform map like:
    `{:find (fn [var] [:find (list 'pull var '[*])])}`

  "
  ([s] (nl->dl s default-transforms))
  ([s transforms]
   (let [p (some->> s str/trim not-empty nldl-parser)]
     (if (insta/failure? p)
       (assoc p :errored? true)
       (some->> p
                not-empty
                (insta/transform (merge default-transforms transforms))
                (apply concat)
                vec)))))

