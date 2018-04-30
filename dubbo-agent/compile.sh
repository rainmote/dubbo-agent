#!/bin/bash

set -eux

export TIMBRE_LEVEL=$1
lein deps
lein uberjar && cp target/mesh-agent-0.1.0-SNAPSHOT-standalone.jar app-standalone.jar
