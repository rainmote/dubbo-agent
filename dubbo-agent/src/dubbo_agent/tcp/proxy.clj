(ns dubbo-agent.tcp.proxy
  (:require [omniconf.core :as cfg]
            [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [gloss.io :as io]
            [taoensso.timbre :as timbre]
            [dubbo-agent.dubbo.protocol :as dubbo-pr]
            [dubbo-agent.dubbo.service :as dubbo]))

(defn handler [f]
  (fn [s info]
    (d/loop []
            (->
              (d/let-flow [msg (s/take! s ::none)]
                          (when-not (= :none msg)
                            (timbre/debug "Receive msg: " msg)
                            (d/let-flow [resp (d/future (f msg))]
                                        (s/put! s resp)
                                        (d/recur))))
              (d/catch
                (fn [ex]
                  (s/put! s (str "Error: " ex))
                  (s/close! s)))))))

(defn dubbo-handler []
  (handler (fn [x]
             (dubbo/forward-frame "127.0.0.1"
                                  (cfg/get [:provider :port])
                                  x))))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
      (s/map #(io/encode protocol %) out)
      s)
    (s/splice
      out
      (io/decode-stream s protocol))))

(defn _start [handler port]
  (timbre/info "Start tcp server for dubbo agent, port: " port)
  (tcp/start-server
    (fn [s info]
      (timbre/debug "Found tcp connect: " s)
      (handler (wrap-duplex-stream dubbo-pr/protocol s) info))
    {:port port}))

(defn start [port]
  (_start (dubbo-handler) port))