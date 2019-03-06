# Cloud Scheduler

SDRS provides an example strategy of scheduling recurring jobs by way of Cloud Scheduler.
More specifically, the code base provides an example of how to use Cloud Scheduler and Cloud Pub/Sub to trigger a Cloud Function.
That Cloud Function in turn invokes event endpoints on the SDRS RESTful API for the "execution" and "validation" services.

The recommended flow is as follows:  

1) Two distinct Cloud Scheduler Jobs (for Execution and Validation respectively) are configured to run at a set crontab frequency.  
2) When Cloud Scheduler runs the jobs, they invoke a Cloud Pub/Sub topic.  
3) The Cloud Pub/Sub topic triggers a Cloud Function that contains logic to invoke either the execution or validation endpoints. 

Please see the GCP documentation for more information https://cloud.google.com/scheduler/docs/tut-pub-sub  

## Deployment & Configuration

1) Ensure that you are configuring the scheduler after the client deployment steps have already been executed.  The Cloud Scheduler depends on the pub/sub and cloud function components already existing in your GCP project. 
See [the Client Cloud Functions README](../sample-client/README-cloudfunctions.md).  

2) Create two respective Cloud Scheduler jobs in the GCP console to trigger the provided sample pub/sub topic and cloud function at the desired frequency.
See https://cloud.google.com/scheduler/docs/quickstart

