#! /bin/bash

# UPDATE the path to your env.txt stored on your GCS Bucket
ENV_VARS_GCS_PATH=gs://my-bucket-name/env.txt

# UPDATE the path to your service account credentials.json stored on your GCS Bucket
CRED_FILE_GCS_PATH=gs://my-bucket-name/credentials.json 

# UPDATE the path to your container image
# To be updated with desired conatiner image and with an updated image for every subsequent software update
# GCR Format: gcr.io/<YOUR_GOOGLE_PROJECT_ID>/YOUR_CONTAINER_IMAGE:TAG
CONTAINER_IMAGE_PATH=gcr.io/my-project-id/my-sdrs-build:0.1.0

# UPDATE Cloud Endpoints Services URL
# Format: <ENDPOINT_NAME>.endpoints.<ENDPOINTS_PROJECT_ID>.cloud.goog
ENDPOINT_SERVICE_URL=sdrs-api.endpoints.my-project-id.cloud.goog

# NO CHANGES required below this point unless you know what you are doing
gcloud auth configure-docker

# env.txt contains the environment specific variables.
gsutil cp $ENV_VARS_GCS_PATH .

# Create directory to store GCP credentials file
mkdir /root/cred

# Copy GCP Service Account credentials file to secured directory
gsutil cp $CRED_FILE_GCS_PATH /root/cred

sleep 5

# Create docker networking
docker network create --driver bridge esp_network

sleep 5

# Run docker image containing SDRS
docker run --detach -v /root/cred:/var/sdrs \
           --name=sdrs --env-file=./env.txt \
           --log-driver=gcplogs \
           --publish=8080:8080 \
           --publish=8086:8086 \
           --net=esp_network $CONTAINER_IMAGE_PATH

# Delete the env.txt file after running container.
rm -rf ./env.txt

sleep 30

docker run --name=esp \
           --log-driver=gcplogs \
           --detach --publish=80:8080 \
           --net=esp_network gcr.io/endpoints-release/endpoints-runtime:1 \
           --service=$ENDPOINT_SERVICE_URL \
           --rollout_strategy=managed --dns=169.254.169.254 --backend=sdrs:8080
