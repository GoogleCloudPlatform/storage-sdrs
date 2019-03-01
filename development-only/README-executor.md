#Execution Service
The execution service controls the execution of retention rules. Retention rules can be fired in a batch (run all rules), by GCP project ID, or by individual rule. Users can also initiate deletes that do not correspond to an existing retention rule.

##Retention Rules
Retention rules are the core concept of SDRS and the execution service. Retention rules specify a bucket or dataset within Google Cloud Storage and a retention period. Any objects within the bucket older than the retention period in days are moved to an archive bucket.

NOTE: Buckets must have a specific structure for this service to operate successfully. SDRS determines object age by using a path format of `/YYYY/MM/DD/HH/object`. For example, a full object path may look like `gs://mybucket/dataset/2019/02/28/12/log.txt`

###Rule Types

Retention rules come in two flavors: DATASET and GLOBAL. 

A DATASET rule applies only to a given dataset within a bucket. For example, a rule may specify that all objects in `gs://mybucket/dataset` are retained for 30 days.

A GLOBAL rule applies to all buckets that are specified by dataset rules and acts as a default retention period. For example, if the GLOBAL retention period is 60 days then the objects in `gs://mybucket/test` will be deleted after 60 days.

NOTE: SDRS allows **ONLY ONE** GLOBAL rule. This rule is denoted by configurable project ID and storage values.

##Execution Service Design
The execution service has 4 basic layers: the controller, the service, the workers, and the STS Executor. 
1) The controller handles the endpoint request and simple validation. Once the message is validated, the controller calls the service. 
2) The service will interpret the request and determine which rules (if any) need to be executed. Once the scope of the rules is known, the service will create the necessary workers.
3) The workers are responsible for handling the asynchronous communication with the external system (Google's Storage Transfer Service) to execute the rule. The internal Job Manager handles all worker thread logic and can be configured to tune the behavior and responsiveness of the Execution Service. A monitoring thread will report the results of all async workers.
4) All logic that interfaces directly with STS is contained within the StsRuleExecutor and StsUtil classes.

##Execution Service Requests
All execution requests are sent to the same endpoint: `events/execution`. By modifying the payload different behaviors can be achieved.

There are two types of requests that can be made to the execution service: **POLICY** and **USER**. A POLICY request corresponds to existing retention rules within the SDRS system. Additional arguments can be provided to narrow the scope of the rules that are executed.

###Policy Requests

The following payload will execute all retention rules within SDRS.
```json
{
  "type": "POLICY"
}
```

To narrow the scope of the rule execution to a single GCP project, the project ID can be added to the request.
```json
{
  "projectId": "<GCP_PROJECT_ID>",
  "type": "POLICY"
}
```

Finally, a target location (in the format `gs://bucket/path`) can be provided to narrow the scope of rules even further.
```json
{
  "target": "<BUCKET_PATH>",
  "projectId": "<GCP_PROJECT_ID>",
  "type": "POLICY"
}
```

###User Requests
Additionally, a user can initiate an object deletion without a corresponding rule. In this case, the user must specify the target location and the project id of the object to be deleted. Only the objects at or below the target path will be deleted.
```json
{
  "target": "<BUCKET_PATH>",
  "projectId": "<GCP_PROJECT_ID>",
  "type": "USER"
}
```

##Storage Transfer Service
SDRS is currently built to operate against Google Cloud's Storage Transfer Service. For SDRS to operate correctly, any bucket that is moved must have a "shadow" bucket created **BEFORE** a retention rule is executed. The shadow bucket should be the same name as the source bucket with a suffix appended. The suffix is configurable within SDRS.

For example, if objects are being moved from `gs://mybucket/dataset/2018` and the configured suffix is "shadow" there **MUST** be a bucket called `gs://mybucketshadow` for SDRS to operate successfully.

NOTE: STS has a limit on the number of requests that can be sent in a given time period. SDRS includes configurable values to throttle requests in case a large number of jobs are scheduled.

##Scheduled Tasks
SDRS contains a mechanism for executing certain functionality, including rule execution, validation, and notification, by periodically calling the relevant endpoints. If needed, this built in scheduler can be disabled and the endpoints can be hit directly by an external scheduler.

##Additional Information
###Configurable Values
####Job Manager
* threadPoolSize: Determines the max number of concurrent workers the job manager will spawn
* shutdownSleepMinutes: Determines how long the job manager will wait for active threads to resolve before shutting down. If no threads are pending, the job manager will shut down immediately.
####Job Manager Monitor
* initialDelay: How long the Job Manager Monitor will wait to start looking for worker results after startup
* frequency: The frequency at which the Job Manager Monitor will check for worker results
* timeUnit: The time unit for the previous two config values
####Scheduler
* enabled: Used to turn on and off the internal scheduler service
* threadPoolSize: Determines the max number of scheduled jobs the scheduler will execute at once
* shutdownWait: Determines how long the scheduler will wait for scheduled jobs to resolve before shutting down. If no jobs are pending, the scheduler will shut down immediately.
* shutdownTimeUnit: The time unit for the shutdown wait value
####Scheduled Tasks
* endpointHost: the SDRS host you want to call from the scheduled task
* endpointPort: the port you want to call from the scheduled task
* endpointApiKey: the api key to provide to the endpoint from the scheduled task
* endpointHttpsEnabled: determines whether the scheduler uses HTTPS in its call
####Rule Execution Task
* endpoint: the endpoint to call. Defaults to `events/execution`
* initialDelay: how long to wait to start making calls after application startup
* frequency: the period between calls
* timeUnit: the time unit for the initialDelay and frequency config values
* timezone: the timezone that is provided for rule scheduling
####Validation Task
* endpoint: the endpoint to call. Defaults to `events/validation`
* initialDelay: how long to wait to start making calls after application startup
* frequency: the period between calls
* timeUnit: the time unit for the initialDelay and frequency config values
####Storage Transfer Service
* maxPrefixCount: The maximum number of path prefixes to include in a single STS job. A max of 1000 is specified by GCP.
* suffix: the configurable suffix that is used to determine the destination bucket of STS jobs
* defaultProjectId: the project id value that denotes the GLOBAL rule
* defaultStorageName: the dataStorageName value that denotes the GLOBAL rule
* maxLookBackInDays: how long back the global rule will operate. This value is used to tamp down the number of prefixes passed to STS
* throttleLimit: How many STS jobs that are sent in a given interval. STS has a limit of 100 jobs per second
* throttleInterval: The interval used in conjunction with the throttleLimit value

