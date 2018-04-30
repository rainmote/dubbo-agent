(ns dubbo-agent.trace
  (:require [taoensso.timbre :as timbre]))

(defn get-current-ts []
  (System/currentTimeMillis))

(defn init-trace
  [id]
  (atom {:id id
         :Begin (get-current-ts)}))

(defn add-tracepoint
  [m name]
  (swap! m assoc name (get-current-ts)))

(defn finish [m]
  (doall
    (swap! m assoc :End (get-current-ts))
    (let [sorted-v (some->> @m
                            (filter #(number? (val %)))
                            (sort-by val <))
          min-v (apply min (vals sorted-v))]
      (timbre/info
        (merge (some->> sorted-v
                        (map #(vector (key %)
                                      (- (val %) min-v)))
                        (into {}))
               {:Begin min-v
                :id (:id @m)})))))