# NLDL

**N**atural **L**anguage for Clojure's **D**ata**l**og flavor
as present in Datomic, Datascript, Datahike etc.

Inspired by [nl-datalog](https://github.com/harc/nl-datalog) and its [demo](http://alexwarth.com/projects/nl-datalog/)

## Usage
```clojure
(ns examples.basic
  (:require [nldl.core :as nldl]
            [datascript.core :as d]))

(nldl/nl->dl
"find movie
 where movie title is avatar
 movie rating is 8")

;; => [:find ?movie :where [?movie :title "avatar"] [?movie :rating 8]]


(def db
  (-> (d/empty-db)
      (d/db-with
       [{:db/id 1 :title "titanic" :rating 3}
        {:db/id 2 :title "avatar" :rating 8}
        {:db/id 8 :title "avatar 2" :rating 5}
        {:db/id 3 :title "holy motors" :rating 8}
        {:db/id 4 :title "greener grass" :rating 9}])))

(d/q
 (nldl/nl->dl
  "find movie
  where movie title is avatar
  movie rating is 8")
 db)

;; => #{[2]}


(d/q
 (nldl/nl->dl
  "find movie
  where movie title is avatar
  movie rating is 8"
  {:find (fn [var] [:find (list 'pull var '[*])])})
 db)

;; =>  ([{:db/id 2, :rating 8, :title "avatar"}])
```

## Status: Alpha
Experimental! I'm toying with this for easier entry of datalog queries on mobile
(for my project [Zeal](https://github.com/den1k/zeal))
where Clojure Datalog's beloved parentheses, brackets, question marks and colons
are always at least two taps away.

Thanks to its simple text processing NLDL also allows for
accessibility enhancements through speech-to-text queries. 


## License

Copyright © 2020 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
