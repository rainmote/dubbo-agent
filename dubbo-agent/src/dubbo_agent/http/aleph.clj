(ns dubbo-agent.http.aleph
  (:require [aleph.http :as http]
            [omniconf.core :as cfg]
            [taoensso.timbre :as timbre]
            [ring.middleware.json]
            [ring.middleware.params]
            [ring.util.response :as response]
            [compojure.route]
            [compojure.handler]
            [compojure.response :refer [Renderable]]
            [compojure.core :as compojure]
            [manifold.deferred :as d]
            [dubbo-agent.slb :as slb]
            [dubbo-agent.trace :as trace]
            [dubbo-agent.dubbo.service :as dubbo]))

(defn resp [status content]
  (response/status (-> (response/response content)
                       (response/content-type "text/plain"))
                   status))

(extend-protocol Renderable
  manifold.deferred.IDeferred
  (render [d _] d))

(defn dubbo-handler [req]
  (timbre/debug "Http receive request parameter: " (:params req))
  (d/future
    (let [{:keys [interface method parameterTypesString parameter]}
            (clojure.walk/keywordize-keys (:params req))
          uuid (str (java.util.UUID/randomUUID))
          trace (trace/init-trace uuid)
          target (slb/chose-by-weight)
          [host port] (some-> target (clojure.string/split #":"))]
      (timbre/debug "Chose server: " host " port: " port " parameter" parameter)
      (if (or (empty? target) (nil? target) (nil? host) (nil? port))
        (timbre/warn "Http process error, don't chose target, ret: " target)
        (let [dubbo-req-param {:interface interface
                               :method method
                               :parameter-type parameterTypesString
                               :parameter parameter
                               :uuid uuid}
              _ (trace/add-tracepoint trace :SendRequestBefore)
              r (dubbo/invoke host
                              (cfg/get [:provider :port])
                              ;; (Integer/parseInt port)
                              dubbo-req-param)]
          (trace/finish trace)
          (resp 200 (str (some-> r :content second))))))))

(defn echo-handler [req]
  (:params req))

(def routes
  (compojure/routes
    (compojure/POST "/" [] dubbo-handler)
    (compojure/POST "/echo" [] echo-handler)
    (compojure.route/not-found "Not Found Page!")))

(def app
  (-> (compojure/routes routes)
      (ring.middleware.params/wrap-params)))

(defn start-http-server [port]
  (timbre/info "Consumer http server started, listen port:" port)
  (http/start-server app {:port port}))
