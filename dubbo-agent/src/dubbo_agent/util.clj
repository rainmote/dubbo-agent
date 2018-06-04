(ns dubbo-agent.util)

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn get-local-ip []
  (.getHostAddress (java.net.InetAddress/getLocalHost)))
