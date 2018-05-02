(defproject dubbo-agent "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main dubbo-agent.core
  :aot [dubbo-agent.core]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 ;; for parse args
                 [com.grammarly/omniconf "0.3.0"]

                 ;; for tcp protocol
                 [aleph "0.4.4"]
                 [gloss "0.2.6"]

                 ;; for http
                 [nginx-clojure "0.4.5"]
                 [nginx-clojure/nginx-clojure-embed "0.4.5"]
                 [ring "1.6.3"]
                 [compojure "1.6.1"]
                 [ring/ring-json "0.4.0"]

                 ;; for dubbo
                 [com.alibaba/fastjson "1.1.20"]

                 ;; for logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tufte "2.0.0"]

                 ;; for etcd
                 [http-kit "2.1.16"]
                 [cheshire "5.2.0"]
                 [slingshot "0.10.3"]
                 [com.cemerick/url "0.1.0"]

                 [io.netty/netty-all "4.1.24.Final"]
                 ])
