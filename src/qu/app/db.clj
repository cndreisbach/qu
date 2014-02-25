(ns qu.app.db
  (:require [monger.core :as mongo]
            [qu.data.source :as source]
            [qu.util :refer :all]
            [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]))

(defrecord DB [source conn options auth]
  component/Lifecycle
  
  (start [component]
    (source/connect source conn options auth)
    component)

  (stop [component]
    (source/disconnect source)
    component))

(defn new-db [options]
  (map->DB options))
