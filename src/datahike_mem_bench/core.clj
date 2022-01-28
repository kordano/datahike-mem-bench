(ns datahike-mem-bench.core
  (:gen-class)
  (:require [datahike.api :as d]
            [clojure.tools.cli :refer [parse-opts]]
            [datahike-jdbc.core]
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

(def cli-opts
  [["-h" "--help" "Print this help"
    :default false]
   ["-l" "--log-file log file"
    :default "./log.out"
    :default-desc "default to ./log.out"
    :parse-fn #(String. %)]
   ["-c" "--config datahike config"
    :default "./config.edn"
    :default-desc "default to ./config.edn"
    :parse-fn #(String. %)]
   ["-u" "--upserts upsert count"
    :default 0
    :default-desc "default to 0"
    :parse-fn #(Integer. %)
    :validate [#(< % 10000000) "Reached maximum upserts."]]
   ["-x" "--text text length"
    :default 10
    :default-desc "default to 10"
    :parse-fn #(Integer. %)]
   ["-t" "--txs transactions"
    :default 50
    :default-desc "fifty transactions per default"
    :parse-fn #(Integer. %)
    :validate [#(< % 10000000) "Reached the maximum transactions."]]])

(defn -main [& args]
  (let [{:keys [options errors]} (parse-opts args cli-opts)]
    (if (nil? errors)
      (let [_ (setup-logging (:log-file options))
            cfg (-> options :config slurp read-string)
            conn (setup-conn cfg)
            {:keys [txs upserts text]} options
            tx-upserts (if (pos? upserts)
                         (long (Math/floor (/ txs upserts)))
                         txs)]
        (log/info "Options:" options)
        (log/info "Base eavt count" (count @conn))
        (log/info "Start transacting...")
        (doseq [n (range txs)]
          (when (= 0 (mod n 1000))
            (log/info "tx count" n))
          (d/transact conn {:tx-data [{:block/text (rand-str text)
                                       :block/id   (mod n tx-upserts)}]}))
        (log/info "Done"))
      (log/error errors))))

(comment

  (def cfg {:store {:backend :mem
                    :id "bench"}
            :keep-history? true
            :schema-flexibility :read})

  (def conn (setup-conn cfg))

  (:meta @conn)

  {:block/text (rand-str 100)
   :block/id 5}


  (-main "-c" "./config.edn" "-t" "1000" "-l"  "./file_log.out")

  (def conn 
    (-> "./config.edn" slurp read-string d/connect))

  (d/q '[:find (count ?e)
         :where
         [?e :block/text ?t _ false]] (d/history @conn))

  )
