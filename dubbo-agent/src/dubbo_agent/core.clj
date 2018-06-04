(ns dubbo-agent.core
  (:require [omniconf.core :as cfg]
            [clojure.java.io :as javaio]
            [taoensso.tufte.timbre]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [dubbo-agent.start :as start])
  (:gen-class))

(cfg/define
  {:global {:nested {:run-type {:one-of ["provider" "consumer"]
                                :required true}
                     :agent-port {:type :number
                                  :default 30000}
                     :logs {:nested {:dir {:default "/root/logs"}
                                     :name {:default "agent.log"}
                                     :level {:default :debug}}}}}
   :consumer {:nested {:port {:type :number
                              :default 20000}
                       :thread-num {:type :number
                                    :default 256}
                       :scheduler {:nested {:cache-count {:type :number
                                                          :default 50}
                                            :wait {:type :number
                                                   :default 1000}}}}}
   :provider {:nested {:port {:type :number
                              :default 20880}
                       :wait {:type :number
                              :default 200}}}
   :etcd {:nested {:host {:default "127.0.0.1"}
                   :port {:type :number
                          :default 4001}
                   :protocol {:default "http"}
                   :root-path {:default "/dubboservice"}}}})

(defn -main [& args]
  ;; initialize and validate args
  (when-let [conf (cfg/get :conf)]
    (cfg/populate-from-file conf))
  (cfg/populate-from-env)
  (cfg/populate-from-cmd args)
  (cfg/populate-from-properties)
  (cfg/verify)

  ;; config timbre log file
  (let [f (format "%s/%s"
                  (cfg/get [:global :logs :dir])
                  (cfg/get [:global :logs :name]))]
    (doall
      (timbre/merge-config! {:level (cfg/get [:global :logs :level])
                             :appenders {:spit (appenders/spit-appender {:fname f})}
                             :output-fn (partial timbre/default-output-fn {:stacktrace-fonts {}})})))

  ;; for profile
  (taoensso.tufte.timbre/add-timbre-logging-handler! {})

  (start/start))
