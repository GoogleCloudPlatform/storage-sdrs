#! /bin/bash

gcloud auth configure-docker
gsutil cp gs://vars-env/env.txt .      # env.txt contains the environment specific variables. These are pulled from a GCS bucket and should be updated as desired.
mkdir /root/cred
gsutil cp gs://vars-env/credentials.json /root/cred  # credentials.json are service account specific. These are pulled from a GCS bucket and should be updated when using a different service account.
sleep 5
docker network create --driver bridge esp_network
sleep 5
# gcr.io/<YOUR_GOOGLE_PROJECT_ID>/YOUR_CONTAINER_IMAGE:TAG. To be updated with desired conatiner image and with an updated image for every subsequent software update
docker run --detach -v /root/cred:/var/sdrs --name=sdrs --env-file=./env.txt --log-driver=gcplogs --publish=8080:8080 --net=esp_network gcr.io/sdrs-server/eshen-sdrs-demo:private-db-1
rm -rf ./env.txt
sleep 30
docker run --name=esp --log-driver=gcplogs --detach --publish=80:8080 --net=esp_network gcr.io/endpoints-release/endpoints-runtime:1 --service=sdrs-api.endpoints.sdrs-server.cloud.goog --rollout_strategy=managed --dns=169.254.169.254 --backend=sdrs:8080
