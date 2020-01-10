(ns examples.basic
  (:require [nldl.core :as nldl]
            [datascript.core :as d]))

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

;;=>  ([{:db/id 2, :rating 8, :title "avatar"}])



