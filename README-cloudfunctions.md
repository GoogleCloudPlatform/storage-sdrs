# Cloud Functions #

There are three different cloud functions that make up the client side installation of the SDRS. Each function listens for messages on a dedicated Pub/Sub topic.

## Deployment Manager Templates

The deployment related files are located in `/sample-client/deployment/cloudfunctions`.
* `cf.jinja` contains the actual deployment manager template for the three cloud functions and Pub/Sub topics.
* `cf.jinja.schema` has a listing of the various configuration values that must be supplied for the deployment.
* `cf.yaml` is where the various configuration values are actually set.

## Permissions
In order to be able to sign the JWT tokens to make calls to the SDRS, the service account that the cloud functions are running under requires the IAM Service Account Token Creator role. 
RPO pattern must end with _ followed by a number for the retention period in days


## Common Util Library
`/sample-client/cloudfunctions/common_lib/utils.py` contains several common methods used by all three functions for parsing Pub/Sub messages from GCS and maintaining valid JWT tokens for calling the SDRS endpoints.

When deploying the code for one of the cloud functions, the `common_lib` folder needs to be copied to the cloud function directory so that it is included in the deployment.
 
## GCS Create  
The GCS create function listens for Pub/Sub messages coming from Cloud Storage OBJECT_FINALIZE events.

If the create event matches the RPO regex pattern, it will check to see if the corresponding retention rule needs to be created or updated and then makes the corresponding request to the SDRS.

Otherwise if it matches the delete regex pattern, it will make a request to SDRS for an immediate run USER retention job.

## GCS Delete
The GCS delete function listens for Pub/Sub messages coming from Cloud Storage OBJECT_DELETE events.

If the delete event matches the delete or success regex pattern, it will make a request to SDRS to send a notification that the directory or dataset deletion was completed successfully.

Otherwise if it matches the RPO regex pattern, it will make a request to SDRS to delete the retention job.

## Scheduler
The scheduler function is triggered by a Cloud Scheduler event to send a request to SDRS to run either the executor or validation service.

## Code Deployment
`/sample-client/cloudfunctions/deploy_code.sh` is provided to allow for easy code updates for the cloud functions after they have been deployed. The first step of the script is to copy the latest common_lib folder to the cloud function folder that is being updated.

The script requires two parameters, `-f [create, delete or scheduler]` and `-d deployment-name`.
* -f will tell the script which code to deploy
* -d is the name of the deployment that was created when the cloud functions were deployed
