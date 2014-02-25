(ns qu.app
  (:require
   [qu.logging :as logging]
   [qu.app.webserver :refer [new-webserver]]
   [qu.app.db :refer [new-db]]   
   [qu.data.mongo :refer [->MongoSource]]   
   [qu.app.options :refer [inflate-options]]
   [qu.cache :as qc]
   [taoensso.timbre :as log]
   [com.stuartsierra.component :as component]))

(defrecord Log [level file]
  component/Lifecycle

  (start [component]
    (logging/config level file)
    component)

  (stop [component]
    component))

(defn new-log [options]
  (map->Log options))

(defrecord CacheWorker []
  component/Lifecycle

  (start [component]
    (let [cache (qc/create-query-cache)
          worker (qc/create-worker cache)]
      (assoc component
        :worker worker
        :worker-agent (qc/start-worker worker))))

  (stop [component]
    (let [worker-agent (:worker-agent component)]
      (qc/stop-worker worker-agent)
      component)))

(def system-components [:log :db :api :cache-worker])

(defrecord QuSystem [options api db log cache-worker]
  component/Lifecycle

  (start [system]
    (let [system (component/start-system system system-components)]
      (log/info "Started with settings" (str options))
      system))
  
  (stop [system]
    (component/stop-system system system-components)))

(defn new-qu-system [options]
  (let [{:keys [http dev log mongo] :as options} (inflate-options options)]
    (map->QuSystem {:options options
                    :db (new-db (assoc mongo :source (->MongoSource)))
                    :log (new-log log)
                    :cache-worker (->CacheWorker)
                    :api (component/using
                          (new-webserver (assoc http :dev dev))
                          [:db
                           :log])})))
