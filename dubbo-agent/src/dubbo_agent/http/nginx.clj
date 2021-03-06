(ns dubbo-agent.http.nginx
  (:require [nginx.clojure.embed :as nginx]
            [omniconf.core :as cfg]
            [taoensso.timbre :as timbre]
            [ring.middleware.json]
            [ring.middleware.params]
            [ring.util.response :as response]
            [compojure.route]
            [compojure.handler]
            [compojure.core :as compojure]
            [dubbo-agent.slb :as slb]
            [dubbo-agent.dubbo.service :as dubbo]
            [dubbo-agent.trace :as trace])
  (:import [io.netty.bootstrap ServerBootstrap]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket.nio NioServerSocketChannel]
           [io.netty.channel ChannelOption]
           [io.netty.buffer PooledByteBufAllocator])
  (:gen-class))

(defn resp [status content]
  (response/status (-> (response/response content)
                       (response/content-type "text/plain"))
                   status))

(defn dubbo-handler [req]
  (timbre/debug "Http receive request parameter: " (:params req))
  (let [{:keys [interface method parameterTypesString parameter]} (:params req)
        uuid (str (java.util.UUID/randomUUID))
        trace (trace/init-trace uuid)
        target (slb/chose-by-weight)
        [host port] (some-> target (clojure.string/split #":"))]
    (timbre/debug "Chose server: " host " port: " port)
    (if (or (empty? target) (nil? target) (nil? host) (nil? port))
      (timbre/warn "Http process error, don't chose target, ret: " target)
      (let [dubbo-req-param {:interface interface
                             :method method
                             :parameter-type parameterTypesString
                             :parameter parameter
                             :uuid uuid}
            _ (trace/add-tracepoint trace :SendRequestBefore)
            resp (dubbo/invoke host
                               (Integer/parseInt port)
                               dubbo-req-param)
            ;;resp (deref resp-future 100 nil)
            ]
        (trace/finish trace)
        (str (some-> resp :content second))))))

(defn echo-handler [req]
  (:params req))

(defn test-handler [req]
  (repeat 30 "HelloWorld"))

(def routes
  (compojure/routes
    (compojure/POST "/" request @(future (dubbo-handler request)))
    (compojure/POST "/echo" request (echo-handler request))
    (compojure/GET "/test" [] test-handler)
    (compojure.route/not-found "Not Found Page!")))

(def app
  (-> (compojure/routes routes)
      (ring.middleware.json/wrap-json-response)
      (ring.middleware.params/wrap-params)
      (compojure.handler/site)))

(defn start-http-server [port]
  (timbre/info "Consumer http server started, listen port:" port)
  (nginx/run-server app {:port port
                         :max-threads (cfg/get [:consumer :thread-num])}))