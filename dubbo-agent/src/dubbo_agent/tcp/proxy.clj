(ns dubbo-agent.tcp.proxy
  (:require [omniconf.core :as cfg]
            [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [gloss.io :as io]
            [taoensso.timbre :as timbre]
            [dubbo-agent.dubbo.protocol :as dubbo-pr]
            [dubbo-agent.dubbo.service :as dubbo]
            [dubbo-agent.trace :as trace])
  (:import [io.netty.bootstrap ServerBootstrap]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket.nio NioServerSocketChannel]
           [io.netty.channel ChannelOption]
           [io.netty.buffer PooledByteBufAllocator])
  (:gen-class))

(defn handler [f]
  (fn [s info]
    (d/loop []
            (->
              (d/let-flow [msg (s/take! s ::none)]
                          (when-not (= ::none msg)
                            (d/future
                              (let [trace (trace/init-trace (:rpc-id msg))]
                                (timbre/debug "Receive msg: " msg)
                                (trace/add-tracepoint trace :GetDubboResp)
                                (s/put! s (f msg trace))
                                (trace/finish trace))))
                          (d/recur))
              (d/catch
                (fn [ex]
                  (s/put! s (str "Error: " ex))
                  (s/close! s)))))))

(defn dubbo-handler []
  (handler (fn [msg trace]
             (dubbo/forward-frame "127.0.0.1"
                                  (cfg/get [:provider :port])
                                  msg
                                  :trace trace))))

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