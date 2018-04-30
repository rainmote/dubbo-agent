#!/bin/bash

set -eux

ROLE=$1
LOG_DIR=`pwd`/log/$ROLE
mkdir -p $LOG_DIR
lein run --global.run-type $ROLE \
         --global.logs.dir $LOG_DIR \
         --global.agent-port 20901 \
         --provider.port 20889
