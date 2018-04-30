(ns dubbo-agent.start
  (:require [omniconf.core :as cfg]
            [dubbo-agent.util :as util]
            [dubbo-agent.etcd.client :as etcd]
            [dubbo-agent.slb :as slb]
            [dubbo-agent.http.server :as http-server]
            [dubbo-agent.tcp.proxy :as tcp-proxy]
            [taoensso.timbre :as timbre]))

(defn register [k v]
  (loop []
    (let [resp (etcd/set-key! k v :ttl 600)]
      (timbre/info "Set etcd key resp:" resp)
      (when (not= (some-> resp :node :key) k)
        (recur)))))

(defn start-provider []
  (timbre/info "Starting agent for provider...")
  (register (format "%s/%s:%d"
                    (cfg/get [:etcd :root-path])
                    (util/get-local-ip)
                    (cfg/get [:global :agent-port]))
            "")
  ;; start tcp proxy
  (tcp-proxy/start (cfg/get [:global :agent-port])))

(defn start-consumer []
  (timbre/info "Starting agent for consumer...")
  (slb/watch (format "%s" (cfg/get [:etcd :root-path])))
  (http-server/start-http-server (cfg/get [:consumer :port])))

(defn start []
  (let [running-type (cfg/get [:global :run-type])]
    (timbre/info "Running type: " running-type)
    ;; connect etcd server
    (etcd/set-connection! (cfg/get :etcd))
    (condp = running-type
      "provider" (start-provider)
      "consumer" (start-consumer)
      (util/exit -1 "Running type only support provider or consumer"))))