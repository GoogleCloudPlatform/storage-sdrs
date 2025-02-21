# Docker

The SDRS server application is intended to run in a docker container.  
The directions below are intended as a quickstart guide to quickly deploy an SDRS image into a basic GCE environment (non-MIG).    

Download and install [Docker](https://www.docker.com/get-started) on your local development environment.  
[See Getting started](https://docs.docker.com/get-started/) with Docker for more details if needed.    

## Build & Publish
The docker build process will copy source code, install dependencies, and run the maven package target all into an image.   
The docker image exposes port 8080 for the service.
The docker image optionally exposes port 8086 if it gets the environment variable ENABLE_JMX=true

Once you have Docker installed, from the home project directory /storage-sdrs, build and tag it as so:  

```
    docker build -t=dev-sdrs . 
```

Verify that the docker image you just built is in your machine’s local Docker image registry:

```
    docker image ls 
```

Verify that you can run the docker image locally by testing it on your browser at http://localhost:4000/status

```
    docker run -p 4000:8080 dev-sdrs
```

The docker image you just built can be published to the GCP container registry.  
To use gcloud as the crediential helper, run the command:

```
    gcloud auth configure-docker
```

Tag the local image with the registry name by using the command.
 
```
    docker tag [SOURCE_IMAGE] [HOSTNAME]/[PROJECT-ID]/[IMAGE]
```

where [SOURCE_IMAGE] is the local image name. 
[See](https://cloud.google.com/container-registry/docs/pushing-and-pulling) to determine what the hostname is and for more details. 

Push the image to your registry

```
    docker push [HOSTNAME]/[PROJECT-ID]/[IMAGE]
```

Use gcloud to deploy the image you just created to Compute Engine as follows.
Use the gcloud compute instances create-with-container command:

```
 gcloud compute instances create-with-container [INSTANCE_NAME] \
     --container-image [DOCKER_IMAGE]
```


[See](https://cloud.google.com/compute/docs/containers/deploying-containers) for more details on deploying containers into GCP.  

## Configuration
There are configuration values for the application that can be passed in as environment variables. 
When running the docker image, the environment variables can be passed in as a file using a command like:

    docker run <image> --env-file=<path/to/txt/file>

## Credentials
Providing GCP credentials to the docker container is a 2 step process
1. Mount the directory containing the credentials JSON file on the docker container as a run argument

    `docker run <image> -v </directory/of/credentials/file>:</mounted/directory>`

2. Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to the location of the credentials file in the mounted 
directory.

    `GOOGLE_APPLICATION_CREDENTIALS=</mounted/directory/credentials.json>`
    
    This can be set within the environment variables file in the configuration step specified above.
