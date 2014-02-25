(ns qu.data.source)

(defprotocol DataSource
  (connect [source conn options auth])
  (disconnect [source])
  (get-datasets [source]) ;; returns a list of all dataset metadata
  (get-metadata [source dataset]) ;; returns the metadata for a dataset
  (get-concept-data [source dataset concept]) ;; returns the data table for a concept
;;  (get-results [source query])
;;  (load-dataset [source definition options])
  )
