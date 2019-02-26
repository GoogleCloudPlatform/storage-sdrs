#! /bin/bash

gcloud auth configure-docker
gsutil cp gs://vars-env/env.txt .
mkdir /root/cred
gsutil cp gs://vars-env/credentials.json /root/cred
sleep 5
docker network create --driver bridge esp_network
sleep 5
docker run --detach -v /root/cred:/var/sdrs --name=sdrs --env-file=./env.txt --log-driver=gcplogs --publish=8080:8080 --net=esp_network gcr.io/sdrs-server/eshen-sdrs-demo:private-db-1
rm -rf ./env.txt
sleep 30
docker run --name=esp --log-driver=gcplogs --detach --publish=80:8080 --net=esp_network gcr.io/endpoints-release/endpoints-runtime:1 --service=sdrs-api.endpoints.sdrs-server.cloud.goog --rollout_strategy=managed --dns=169.254.169.254 --backend=sdrs:8080
