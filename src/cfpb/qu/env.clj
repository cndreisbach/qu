(ns cfpb.qu.env
  "This namespace is the access point to the external configuration,
which is set via environment variables, Java properties, or a
configuration file.

See the `default-env` for the default environment setup. These may be
overridden via your `project.clj` or environment variables. If the
environment variable QU_CONFIG is set with a configuration file (in
Clojure, as a map like `default-env`), the values in that
configuration file override everything else."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [environ.core :as environ]))

(def default-env
  {:mongo-host "127.0.0.1"
   :mongo-port 27017
   :statsd-port 8125
   :http-ip "127.0.0.1"
   :http-port 3000
   :http-threads 4
   :http-queue-size 20480
   :log-file nil
   :log-level :info
   :dev false
   :integration false
   :api-name "Data API"})

(def ^{:doc "A map of environment variables."}
  env
  (let [env (merge default-env environ/env)
        config-file (:qu-config environ/env)]
    (if config-file
      (merge env
             (binding [*read-eval* false]
               (read-string (slurp config-file))))
      env)))
