(ns qu.data.mongo
  (:require [qu.data.source :as source]
            [qu.util :refer :all]
            [taoensso.timbre :as log]
            [monger
             [core :as mongo :refer [with-db get-db]]
             [query :as q]
             [collection :as coll]
             [conversion :as conv]
             joda-time
             json]))

(defn concept-collection [concept]
  (str "concept__" (name concept)))

(defn authenticate-mongo
  [auth]
  (doseq [[db [username password]] auth]
    (mongo/authenticate (mongo/get-db (name db))
                        username
                        (.toCharArray password))))

(defn connect-mongo
  [{:keys [uri hosts host port] :as conn} options auth]  
  (let [options (apply-kw mongo/mongo-options options)
        connection 
        (cond
         uri (try (mongo/connect-via-uri! uri)
                  (catch Exception e
                    (log/error "The Mongo URI specified is invalid.")))
         hosts (let [addresses (map #(apply mongo/server-address %) hosts)]
                 (mongo/connect! addresses options))
         :else (mongo/connect! (mongo/server-address host port) options))]
    (if (map? auth)
      (authenticate-mongo auth))
    connection))


(defrecord MongoSource []
  source/DataSource

  (connect [this conn options auth]
    (connect-mongo conn options auth))

  (disconnect [this]
    (when (bound? #'mongo/*mongodb-connection*)
      (mongo/disconnect!)))
  
  (get-datasets [this]
    (with-db (get-db "metadata")
      (coll/find-maps "datasets" {})))

  (get-metadata [this dataset]
    (with-db (get-db "metadata")
      (coll/find-one-as-map "datasets" {:name dataset})))

  (get-concept-data [this dataset concept]
    (with-db (get-db dataset)
      (coll/find-maps (concept-collection concept)))))
