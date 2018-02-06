#!/usr/bin/env bash
set -e

docker build -f Dockerfile -t jenkins/jenkins:lts-alpine-casc .
docker build -f Dockerfile.onbuild -t jenkins/jenkins:lts-alpine-casc-onbuild .
docker build -f Dockerfile.kubernetes -t jenkins/jenkins:lts-alpine-casc-kubernetes .

