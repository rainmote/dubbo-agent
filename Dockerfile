
FROM clojure AS clj

# copy src
COPY . /root/workspace/agent
WORKDIR /root/workspace/agent/dubbo-agent

# compile
RUN lein deps

# compile and rename
RUN cp "$(lein uberjar 2> stderr | sed -n 's/^Created \(.*standalone\.jar\)/\1/p' | grep standalone.jar)" target/app-standalone.jar


FROM registry.cn-hangzhou.aliyuncs.com/aliware2018/services AS builder
FROM registry.cn-hangzhou.aliyuncs.com/aliware2018/debian-jdk8-devel

COPY --from=builder /root/workspace/services/mesh-provider/target/mesh-provider-1.0-SNAPSHOT.jar /root/dists/mesh-provider.jar
COPY --from=builder /root/workspace/services/mesh-consumer/target/mesh-consumer-1.0-SNAPSHOT.jar /root/dists/mesh-consumer.jar
COPY --from=builder /usr/local/bin/docker-entrypoint.sh /usr/local/bin

COPY --from=clj /root/workspace/agent/dubbo-agent/target/app-standalone.jar /root/dists/mesh-agent.jar
COPY --from=clj /root/workspace/agent/start-agent.sh /usr/local/bin/start-agent.sh

RUN set -ex \
 && chmod a+x /usr/local/bin/start-agent.sh \
 && mkdir -p /root/logs
RUN addgroup nobody
RUN adduser nobody nobody
RUN echo '127.0.0.1' `hostname` | tee -a /etc/hosts


EXPOSE 8087

ENTRYPOINT ["docker-entrypoint.sh"]
