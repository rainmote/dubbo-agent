#!/bin/bash

set -exu

docker build . -f Dockerfile_local -t dubbo-agent:1.0.0
