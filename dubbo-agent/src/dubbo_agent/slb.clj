(ns dubbo-agent.slb
  (:require [omniconf.core :as cfg]
            [taoensso.timbre :as timbre]
            [clojure.spec.alpha :as alpha]
            [dubbo-agent.etcd.client :as etcd]
            [dubbo-agent.util :as util])
  (:gen-class))

(def ^:dynamic *data* (atom {}))

(defmacro record-request
  "Evaluates expr and prints the time it took.  Returns the value of expr."
  [k expr]
  `(let [start# (. System (nanoTime))
         t# (doall (swap! *data* update-in [~k :count] inc))
         ret# ~expr
         t# (doall (swap! *data* update-in [~k :count] dec))
         runtime# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)
         v# (get-in @*data* [~k :lat])]
     (timbre/debug "statistic time: " runtime#)
     (when v#
       (doall (swap! *data* assoc-in [~k :lat]
                     (drop (- (count v#)
                              (cfg/get [:consumer :scheduler :cache-count]))
                           (conj v# runtime#)))))
     ret#))

(defn update-node [ks]
  (let [f (fn [k]
            (when-not (contains? @*data* k)
              (swap! *data* assoc k {:lat []
                                     :count 0})))]
    (doall (map f ks))
    (timbre/info "Service node update:" @*data*)))

(defn find-node [d]
  (some->> (etcd/get-key d :recursive true)
           :node
           :nodes
           (map #(:key %))
           (map #(last (clojure.string/split % #"/")))
           ;; TODO: add ip regex
           ;; (filter #(re-find #"" %))
           ))

(defn watch [d]
  (future
    (loop []
      (update-node (find-node d))
      (let [r (etcd/watch-key d :recursive true)]
        (timbre/info "Watch key:" r)
        (recur)))))

(defn chose-by-random []
  (let [ks (keys @*data*)
        n (count ks)]
    (nth ks (rand-int n))))

(defn chose-by-weight []
  (let [calc (fn [[k v]]
               (let [[host port] (some-> k (clojure.string/split #":"))]
                 (condp = port
                   "20901" (vector :100 k)
                   "20902" (vector :200 k)
                   "20903" (vector :300 k)
                   (util/exit -1 "Please check provider port"))))
        k-weight (into {} (map calc @*data*))
        r (rand-int 600)]
    (timbre/debug "k-weight:" k-weight ", rand:" r)
    (if (empty? k-weight)
      (timbre/warn "chose by weight is empty")
      (cond
        (alpha/int-in-range? 0 100 r) (or (:100 k-weight) (chose-by-random))
        (alpha/int-in-range? 100 300 r) (or (:200 k-weight) (chose-by-random))
        (alpha/int-in-range? 300 600 r) (or (:300 k-weight) (chose-by-random))
        :else (do
                (timbre/error "chose by weight fail, rand:" r ", k-weight:" k-weight)
                (chose-by-random))))))

(defn chose-by-min-latency []
  (doall
    (let [calc (fn [[k v]]
                 (let [c (:count v)
                       lat (:lat v)]
                   (if (> c (cfg/get [:consumer :scheduler :request-limit]))
                     (vector k (Integer/MAX_VALUE))
                     (if (empty? lat)
                       (vector k 0)
                       (vector k (/ (reduce + lat) (count lat)))))))
          k-avg (map calc @*data*)]
      (timbre/debug "chose by min latency, avg: " (take 3 k-avg))
      (if-not (empty? k-avg)
        (let [r (first (apply min-key second k-avg))]
          (when-not (= r (Integer/MAX_VALUE))
            r))
        (chose-by-random)))))