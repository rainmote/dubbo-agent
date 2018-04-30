#!/bin/bash

role=$1

if [ "$1" = "consumer" ]; then
  docker run \
    -p 8087:8087 \
    -p 20000:20000 \
    -it dubbo-agent:1.0.0 $role
else
  docker run \
    -it dubbo-agent:1.0.0 $role
fi

#    -p 20000:20000 \
#    -p 30000:30000 \
#    -p 20889:20889 \
#    -p 20890:20890 \
#    -p 20891:20891 \
#    -p 30001:30001 \
#    -p 30002:30002 \
#    -p 30003:30003 \
