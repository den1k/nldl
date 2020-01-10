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


(def parse-result-transforms
  {:var          (fn [var] (symbol (str "?" var)))
   :attr         keyword
   :number       read-clj
   :subject      identity
   :obj          identity
   :where-clause vector})


(defn nl->dl
  ([s] (nl->dl s parse-result-transforms))
  ([s transforms]
   (let [p (some->> s str/trim not-empty nldl-parser)]
     (if (insta/failure? p)
       (assoc p :errored? true)
       (some->> p
                not-empty
                (insta/transform (merge parse-result-transforms transforms))
                (apply concat)
                vec)))))

