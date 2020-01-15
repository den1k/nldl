(ns nldl.warth
  "Inspired by Alex Warth's Natural Language Datalog
  https://github.com/harc/nl-datalog
  http://alexwarth.com/projects/nl-datalog/"
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [datascript.core :as d]
            [nldl.util :as u]))

;; TODO text->rules

(str/split-lines
 "Homer is Bart's father
 Homer is Lisa's father
 Abe is Homer's father

 X is Y's parent if X is Y's father
 X is Y's grandfather if X is Z's father and Z is Y's parent
 X has Y.count grandchildren if X is Y's grandfather
 ")

(def s
  (str/join \newline
            ["Homer simpson is Bart's father"
             " Homer simpson is Lisa's father"
             " Abe simpson is Homer's father"
             "Bart is blue"
             "Bart is tall"
             "Bart is Lisa's brother"
             "Bart has a dog"
             "Billy is bart's dog"
             "Amy is bart's dog"
             " "
             ;" X is Y's parent if X is Y's father"
             ;" X is Y's grandfather if X is Z's father and Z is Y's parent"
             ;" X has Y.count grandchildren if X is Y's grandfather"
             ]))

(do

  (def text->datoms-grammar
    "<expr> = (<whitespaces-or-newlines>? datom)*

    <datom> = relation | statement | assignment

    subject = words
    ref = word <'\\''?><'s'>? (* optional `'s` postfixes *)
    attr = word

    relation = subject <whitespace> <'is'> <whitespace> ref <whitespace> attr
    statement = subject <whitespace> <'is'> <whitespace> attr
    assignment = subject <whitespace> <'has'> <whitespace> <word>? <whitespace> attr

    <word> = #'^(?![-])[a-zA-Z-]+'
    <words> = (<whitespace>? word)*
    whitespace = #'\\s+'
    whitespaces-or-newlines = (<whitespace> | <'\n'>)*"
    )

  (insta/defparser text->datoms-parser text->datoms-grammar)

  (def default-transforms

    (letfn [(name-ref [strs] {:name (str/join " " strs)})]
      {:subject    (fn [& strs] [:subject (name-ref strs)])
       :ref        (fn [& strs] [:ref (name-ref strs)])
       :attr       (fn [x] [:attr (keyword x)])
       :statement  (fn [[_ subj] [_ attr]]
                     (assoc subj :is attr))
       :relation   (fn [[_ subj] [_ ref] [_ attr]]
                     (assoc ref attr subj))
       :assignment (fn [[_ subj] [_ attr]]
                     (assoc subj :has attr))}))

  (defn text->tx-data [s]
    [s]
    (let [p (some->> s str/trim not-empty str/lower-case text->datoms-parser)]
      (if (insta/failure? p)
        (assoc p :errored? true)
        (some->> p
                 not-empty
                 (insta/transform (merge default-transforms #_transforms))
                 vec))))

  (defn text->schema [s]
    (let [p (some->> s str/trim not-empty str/lower-case text->datoms-parser)]
      (if (insta/failure? p)
        (assoc p :errored? true)
        (some->> p
                 not-empty
                 (filter #(-> % first (= :relation)))
                 (insta/transform {:attr     (fn [x] [:attr (keyword x)])
                                   :relation (fn [_subj _ref [_ attr]]
                                               attr)})
                 (into #{})
                 (u/project (juxt identity (constantly {:db/valueType   :db.type/ref
                                                        :db/cardinality :db.cardinality/many})))
                 )))
    )

  ;(text->schema s)
  (text->tx-data s)

  (-> (d/empty-db (merge {:name {:db/unique :db.unique/identity}
                          :is   {:db/cardinality :db.cardinality/many}
                          :has  {:db/cardinality :db.cardinality/many}
                          ;                       :father  {:db/valueType :db.type/ref}
                          ;:brother {:db/valueType :db.type/ref}
                          }
                         (text->schema s)))
      (d/with (text->tx-data s))
      :db-after
      ;(d/entity [:name "bart"])
      (d/entity [:name "lisa"])
      d/touch
      :brother
      first
      d/touch
      ;:father
      ;first
      ;d/touch
      ;:dog
      ;d/touch
      )

  )

(defn datomic->datascript-schema [schema]
  (u/project (juxt :db/ident #(dissoc % :db/ident)) schema))

