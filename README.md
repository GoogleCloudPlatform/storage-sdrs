01.12.2018

This POC demonstrates how to use maven to package a FAT Jar with an 
embedded grizzly Http server suitable for deployment on GCP as a CE MIG

#SETUP STEPS

0) pre-req: maven and java 8 sdk are installed
1) gotta setup gcp libraries and do this and that

For reference, I adapted this bookshelf tutorial for our purposes (main diff - we are deploying a fat jar NOT a war)
https://cloud.google.com/java/docs/tutorials/bookshelf-on-compute-engine

2) super important don't forget to do this
gcloud beta auth application-default login

3) Add your GCP credentials json file to project root directory as `credentials.json`

#BUILD STEPS using Maven

1) from within the top directory ../rest-engine at the prompt type: "mvn clean install package"
   that will package a fat jar within the target directory

2) Launch the jar by typing at the prompt: "java -jar ./target/restEngine-jar-with-dependencies.jar localhost"

3) Verify the jar's deployment by going to a web browser and trying these paths (host relative to box local vs cloud's ip):
http://localhost:8080/myresource

#DEPLOYMENT to GCP

1) create a bucket in your GCP project and fill the name in the make file: BUCKET=FILL_IN_BUCKET_NAME

deploy to a single VM CE instance by executing ./makeRestEngine gce
tear down by executing ./makeRestEngine down

#DEPLOYMENT to CE MIG

./makeRestEngine gce-many
./makeRestEngine down-many


#Docker Deployment to GCP
1) Build the docker image
```
docker build -t gcr.io/sdrs-server/sdrs:tag . -f sdrs.dockerfile
```
2) Publish the image to the GCP image repository to make it available
```
docker push gcr.io/sdrs-server/sdrs:tag
```

(to run the docker container locally)
```
 docker run -p 8080:8080 <IMAGE ID>
```
