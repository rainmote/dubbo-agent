#!/bin/bash

ETCD_HOST=$(ip addr show docker0 | grep 'inet\b' | awk '{print $2}' | cut -d '/' -f 1)
ETCD_PORT=2379
ETCD_URL=http://$ETCD_HOST:$ETCD_PORT

echo ETCD_URL = $ETCD_URL

if [[ "$1" == "consumer" ]]; then
  echo "Starting consumer agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -XX:NewRatio=1 \
       -XX:AutoBoxCacheMax=20000 \
       -XX:MaxDirectMemotySize=1280M \
       -Dio.netty.leakDetectionLevel=DISABLED \
       -Dglobal.run-type=consumer \
       -Dglobal.agent-port=20900 \
       -Detcd.host=$ETCD_HOST \
       -Detcd.port=$ETCD_PORT \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-small" ]]; then
  echo "Starting small provider agent..."
  java -jar \
       -Xms512M \
       -Xmx512M \
       -XX:NewRatio=1 \
       -XX:AutoBoxCacheMax=20000 \
       -XX:MaxDirectMemotySize=1280M \
       -Dio.netty.leakDetectionLevel=DISABLED \
       -Dglobal.run-type=provider \
       -Dglobal.agent-port=20901 \
       -Dprovider.port=20889 \
       -Detcd.host=$ETCD_HOST \
       -Detcd.port=$ETCD_PORT \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-medium" ]]; then
  echo "Starting medium provider agent..."
  java -jar \
       -Xms1536M \
       -Xmx1536M \
       -XX:NewRatio=1 \
       -XX:AutoBoxCacheMax=20000 \
       -XX:MaxDirectMemotySize=1280M \
       -Dio.netty.leakDetectionLevel=DISABLED \
       -Dglobal.run-type=provider \
       -Dglobal.agent-port=20902 \
       -Dprovider.port=20890 \
       -Detcd.host=$ETCD_HOST \
       -Detcd.port=$ETCD_PORT \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
elif [[ "$1" == "provider-large" ]]; then
  echo "Starting large provider agent..."
  java -jar \
       -Xms2560M \
       -Xmx2560M \
       -XX:NewRatio=1 \
       -XX:AutoBoxCacheMax=20000 \
       -XX:MaxDirectMemotySize=1280M \
       -Dio.netty.leakDetectionLevel=DISABLED \
       -Dglobal.run-type=provider \
       -Dglobal.agent-port=20903 \
       -Dprovider.port=20891 \
       -Detcd.host=$ETCD_HOST \
       -Detcd.port=$ETCD_PORT \
       -Detcd.url=$ETCD_URL \
       -Dlogs.dir=/root/logs \
       /root/dists/mesh-agent.jar
else
  echo "Unrecognized arguments, exit."
  exit 1
fi
