(ns datahike-mem-bench.analytics
  (:require
   #_[oz.core :as oz]
   [clojure.instant :as instant]
   [clojure.string :as s])
  (:import
   [java.util Date]))

(comment


  (def tx-stats
    (let [metrics (->> (s/split (slurp "bench.log") #"\n")
                       (drop 5)
                       (butlast)
                       (map #(s/split % #" "))
                       (map #(vector (-> % first instant/read-instant-date .getTime) (last %))))
          start-time (ffirst metrics)]
      (->> metrics
           (map (fn [[timestamp tx-count]] {:Timestamp (float (/ (- timestamp start-time) 1000))
                                            :TxCount (Float/parseFloat tx-count)})))))

  (def jvm-stats
    (let [[attrs & metrics] (->> (s/split (slurp "jstat.out") #"\n")
                                 (map #(s/split % #" "))
                                 (map #(remove s/blank? %)))
          keyz              (mapv keyword attrs)]
      (->> metrics
           (map (fn [m] (zipmap keyz (map #(Float/parseFloat %) m))))
           (map #(select-keys % [:Timestamp :OU])))))


  (def jdbc-tx-stats
    (let [metrics (->> (s/split (slurp "jdbc_bench.log") #"\n")
                       (drop 11)
                       (map #(s/split % #" "))
                       (map #(vector (-> % first instant/read-instant-date .getTime) (last %))))
          start-time (ffirst metrics)]
      (->> metrics
           (map (fn [[timestamp tx-count]] {:Timestamp (float (/ (- timestamp start-time) 1000))
                                            :TxCount (Float/parseFloat tx-count)})))))

  (count jdbc-tx-stats)

  (def jdbc-jvm-stats
    (let [[attrs & metrics] (->> (s/split (slurp "jdbc_jstat.out") #"\n")
                                 (map #(s/split % #" "))
                                 (map #(remove s/blank? %)))
          keyz              (mapv keyword attrs)]
      (->> metrics
           (map (fn [m] (zipmap keyz (map #(Float/parseFloat %) m))))
           (map #(select-keys % [:Timestamp :OU])))))

  (def tx-stats-1
    (let [metrics (->> (s/split (slurp "file_bench_1.log") #"\n")
                       (drop 5)
                       (butlast)
                       (map #(s/split % #" "))
                       (map #(vector (-> % first instant/read-instant-date .getTime) (last %))))
          start-time (ffirst metrics)]
      (->> metrics
           (map (fn [[timestamp tx-count]] {:Timestamp (float (/ (- timestamp start-time) 1000))
                                            :TxCount (Float/parseFloat tx-count)})))))

  (def jvm-stats-1
    (let [[attrs & metrics] (->> (s/split (slurp "file_jstat_1.out") #"\n")
                                 (map #(s/split % #" "))
                                 (map #(remove s/blank? %)))
          keyz              (mapv keyword attrs)]
      (->> metrics
           (map (fn [m] (zipmap keyz (map #(Float/parseFloat %) m))))
           (map #(select-keys % [:Timestamp :OU])))))


  (count jdbc-jvm-stats)

  (count tx-stats)

  (count jvm-stats)

  (oz/start-server!)

  (def combined-plot
    {:data {:values (vec (sort-by :Timestamp (concat tx-stats jvm-stats)))}
     :width 1500
     :height 1000
     :layer [{:mark {:stroke "red"
                     :type "line"
                     :interpolate "monotone"}
              :encoding {:y {:field "OU"
                             :type "quantitative"
                             :title "Old space utilization"}
                         :x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}}}
             {:mark {:type "circle"
                     :color "blue"}
              :encoding {:x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}
                         :y {:field "TxCount"
                              :type "quantitative"
                              :title "Transaction count"}}}]
     :resolve {:scale {:y "independent"}}})

  (def jdbc-combined-plot
    {:data {:values (vec (sort-by :Timestamp (concat jdbc-tx-stats jdbc-jvm-stats)))}
     :width 1500
     :height 1000
     :layer [{:mark {:stroke "red"
                     :type "line"
                     :interpolate "monotone"}
              :encoding {:y {:field "OU"
                             :type "quantitative"
                             :title "Old space utilization"}
                         :x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}}}
             {:mark {:type "circle"
                     :color "blue"}
              :encoding {:x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}
                         :y {:field "TxCount"
                             :type "quantitative"
                             :title "Transaction count"}}}]
     :resolve {:scale {:y "independent"}}})


  (def combined-plot-1
    {:data {:values (vec (sort-by :Timestamp (concat tx-stats-1 jvm-stats-1)))}
     :width 1500
     :height 1000
     :layer [{:mark {:stroke "red"
                     :type "line"
                     :interpolate "monotone"}
              :encoding {:y {:field "OU"
                             :type "quantitative"
                             :title "Old space utilization"}
                         :x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}}}
             {:mark {:type "circle"
                     :color "blue"}
              :encoding {:x {:field "Timestamp"
                             :title "Seconds since start"
                             :type "quantitative"}
                         :y {:field "TxCount"
                             :type "quantitative"
                             :title "Transaction count"}}}]
     :resolve {:scale {:y "independent"}}})

  (def viz
    [:div
     [:h2 "Datahike Memory Benchmark"]
     [:vega-lite combined-plot]
     [:h2 "Datahike PG Memory Benchmark"]
     [:vega-lite jdbc-combined-plot]
     [:h2 "Datahike PG Memory Benchmark Low Cache"]
     [:vega-lite combined-plot-1]])

  (oz/view! viz)

  )
