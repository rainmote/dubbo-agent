
FROM clojure AS clj

# copy src
COPY . /root/workspace/agent
WORKDIR /root/workspace/agent/dubbo-agent

# compile
RUN export TIMBRE_LEVEL=":info"
RUN lein deps

# compile and rename
RUN cp "$(lein uberjar 2> stderr | sed -n 's/^Created \(.*standalone\.jar\)/\1/p' | grep standalone.jar)" target/app-standalone.jar


FROM registry.cn-hangzhou.aliyuncs.com/tianchi4-docker/tianchi4-services AS builder
FROM registry.cn-hangzhou.aliyuncs.com/tianchi4-docker/debian-jdk8

COPY --from=builder /root/workspace/services/mesh-provider/target/mesh-provider-1.0-SNAPSHOT.jar /root/dists/mesh-provider.jar
COPY --from=builder /root/workspace/services/mesh-consumer/target/mesh-consumer-1.0-SNAPSHOT.jar /root/dists/mesh-consumer.jar
COPY --from=builder /usr/local/bin/docker-entrypoint.sh /usr/local/bin

COPY --from=clj /root/workspace/agent/dubbo-agent/target/app-standalone.jar /root/dists/mesh-agent.jar
COPY --from=clj /root/workspace/agent/start-agent.sh /usr/local/bin/start-agent.sh

RUN set -ex && mkdir -p /root/logs
RUN addgroup nobody
RUN adduser nobody nobody
RUN echo '127.0.0.1' `hostname` | tee -a /etc/hosts

ENTRYPOINT ["docker-entrypoint.sh"]