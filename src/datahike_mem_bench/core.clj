(ns datahike-mem-bench.core
  (:gen-class)
  (:require [datahike.api :as d]
            [clojure.tools.cli :refer [parse-opts]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders])
  (:import [java.util Random]))

(def cfg {:store {:backend :file
                  :path "/tmp/mem-bench"}
          :keep-history? true
          :attribute-refs? false
          :schema-flexibility :write})

(def schema [{:db/ident :block/text
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one}
             {:db/ident :block/counter
              :db/valueType :db.type/long
              :db/cardinality :db.cardinality/one}
             {:db/ident :block/id
              :db/valueType :db.type/long
              :db/unique :db.unique/identity
              :db/cardinality :db.cardinality/one}])

(defn setup-logging [log-file]
  (log/set-level! :info)
  (log/merge-config!
   {:appenders {:spit (appenders/spit-appender {:fname log-file})}}))

(defn setup-conn [cfg]
  (log/info "Setting up datahike...")
  (d/delete-database cfg)
  (d/create-database cfg)
  (let [conn (d/connect cfg)]
    (d/transact conn {:tx-data schema})
    (log/info "Done")
    conn))

(defn rand-str
  ^String [^Long len]
  (let [leftLimit 97
        rightLimit 122
        random (Random.)
        stringBuilder (StringBuilder. len)
        diff (- rightLimit leftLimit)]
    (dotimes [_ len]
      (let [ch (char (.intValue ^Double (+ leftLimit (* (.nextFloat ^Random random) (+ diff 1)))))]
        (.append ^StringBuilder stringBuilder ch)))
    (.toString ^StringBuilder stringBuilder)))

(defmacro timed
  "Evaluates expr. Returns the value of expr and the time in a map."
  [expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     {:res ret# :t (/ (double (- (. System (nanoTime)) start#)) 1000000.0)}))

(def cli-opts
  [["-h" "--help" "Print this help"
    :default false]
   ["-l" "--log-file log file"
    :default "./log.out"
    :default-desc "default to ./log.out"
    :parse-fn #(String. %)]
   ["-s" "--store store path"
    :default "/tmp/mem-bench"
    :default-desc "default to /tmp/mem-bench"
    :parse-fn #(String. %)]
   ["-t" "--txs transactions"
    :default 50
    :default-desc "fifty transactions per default"
    :parse-fn #(Integer. %)
    :validate [#(< % 10000000) "Reached the maximum."]]])


(defn -main [& args]
  (let [{:keys [options errors]} (parse-opts args cli-opts)]
    (if (nil? errors)
      (let [_ (setup-logging (:log-file options))
            conn (setup-conn (assoc-in cfg [:store :path] (:store options)))]
        (log/info "Options:" options)
        (log/info "Base eavt count" (count @conn))
        (log/info "Start transacting...")
        (doseq [n (range (:txs options))]
          (when (= 0 (mod n 1000))
            (log/info "tx count" n))
          (d/transact conn {:tx-data [{:block/text (rand-str (rand-int 10000))
                                       :block/id   n}]}))
        (log/info "Done")
        (System/exit 0))
      (log/error errors))))

