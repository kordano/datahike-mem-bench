(ns datahike-mem-bench.analytics
  (:require
   [oz.core :as oz]
   [clojure.instant :as instant]
   [clojure.string :as s])
  (:import
   [java.util Date]))

(comment


  (def tx-stats
    (let [metrics (->> (s/split (slurp "bench.log") #"\n")
                       (drop 5)
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

  (count tx-stats)

  (count jvm-stats)

  (oz/start-server!)

  (def combined-plot
    {:data {:values (vec (sort-by :Timestamp (concat tx-stats jvm-stats)))}
     :width 2048
     :height 1024
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
     [:vega-lite combined-plot]])

  (oz/view! viz)

  )
