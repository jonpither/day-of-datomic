(require
 '[datomic.api :as d]
 '[datomic.samples.news :as news])
(def uri "datomic:mem://news")
(def conn (news/setup-sample-db-1 uri))

(def db (d/db conn))

; Find all entities with an email attribute.
(d/q '[:find ?e
       :where [?e :user/email]]
     db)

; Only find users with "editor@example.com" as email.
(d/q '[:find ?e
       :in $ ?email
       :where [?e :user/email ?email]]
     db
     "editor@example.com")

; Find all comments made by user with "editor@example.com" as email.
(d/q '[:find ?comment
       :in $ ?email
       :where [?user :user/email ?email]
              [?comment :comment/author ?user]]
     db
     "editor@example.com")

; Use aggregator count to return count of comments instead of entity ids.
(d/q '[:find (count ?comment)
       :in $ ?email
       :where [?user :user/email ?email]
              [?comment :comment/author ?user]]
     db
     "editor@example.com")

; No commentable entity has :user/email, so this will be an empty return.
(d/q '[:find (count ?comment)
       :where
       [?comment :comment/author]
       [?commentable :comments ?comment]
       [?commentable :user/email]]
     db)

; Dropping this constraint counts all comments (40).
(d/q '[:find (count ?comment)
       :where
       [?comment :comment/author]
       [?commentable :comments ?comment]]
     db)

(d/q '[:find ?attr-name
       :where
       [?ref :comments]
       [?ref ?attr]
       [?attr :db/ident ?attr-name]]
     db)

(def editor (d/entity db [:user/email "editor@example.com"]))

(:user/firstName editor)

(d/touch editor)

(-> editor :comment/_author)

(->> editor :comment/_author (mapcat :comments))

(def editor-id (:db/id editor))

(def txid (->> (d/q '[:find ?tx
                      :in $ ?e
                      :where [?e :user/firstName _ ?tx]]
                    db
                    editor-id)
                 ffirst))

(d/tx->t txid)

(-> (d/entity (d/db conn) txid)
    :db/txInstant)

(def older-db (d/as-of db (dec txid)))
(:user/firstName (d/entity older-db editor-id))

(def hist (d/history db))
(->> (d/q '[:find ?tx ?v ?op
            :in $ ?e ?attr
            :where [?e ?attr ?v ?tx ?op]]
          hist
          editor-id
          (d/entid db :user/firstName))
     (sort-by first))

(d/q '[:find ?e
       :where [?e :user/email]]
     [[1 :user/email "jdoe@example.com"]
      [1 :user/firstName "John"]
      [2 :user/email "jane@example.com"]])

