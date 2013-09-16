(ns cfpb.qu.data.compression
  "Functions for compressing and uncompressing data going into and
coming out of Mongo."
  (:require
   [cfpb.qu.util :refer :all]
   [taoensso.timbre :as log]
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
   [clj-statsd :as sd]
   [monger.key-compression :as mzip]))

(defn- slice-columns
  [slicedef]
  (concat (:dimensions slicedef) (:metrics slicedef)))

(defn field-zip-fn
  "Given a slice definition, return a function that will compress
field names."
  [slicedef]
  (let [fields (slice-columns slicedef)]
    (sd/with-timing "qu.queries.fields.zip"
      (mzip/compression-fn fields))))

(defn field-unzip-fn
  "Given a slice definition, return a function that will decompress
field names."
  [slicedef]
  (let [fields (slice-columns slicedef)]
    (sd/with-timing "qu.queries.fields.unzip"
      (mzip/decompression-fn fields))))

(defn compress-fields
  "Given a map of fields and values, and a function to compress those
field names, return a map with the compressed names and the values."
  [fields zipfn]
  (convert-keys fields zipfn))

(defn compress-where
  "Recurse through a Mongo query, compressing the names of all the
referenced fields using the function to compress field names that you
have provided."
  [where zipfn]
  (let [f (fn [[k v]] (if (keyword? k) [(zipfn k) v] [k v]))]
    ;; only apply to maps
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) where)))

(defn compress-find
  "Given a complete map that you plan to pass to
`cfpb.qu.data/get-find` and a function to compress field names, go
through the map and compress all field names."
  [find-map zipfn]
  (-> find-map
      (update-in [:query] compress-where zipfn)
      (update-in [:fields] convert-keys zipfn)
      (update-in [:sort] convert-keys zipfn)))
