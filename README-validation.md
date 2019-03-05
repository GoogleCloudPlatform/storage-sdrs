#Validation Service
The validation service periodically queries the [Storage Transfer Service](https://cloud.google.com/storage-transfer/docs/) (STS) to update the status in SDRS of any non-completed STS jobs.

The service is run on a configurable schedule, and can also be invoked at the API endpoint: `events/validation`.

The configuration values listed [Configurable Values](README-executor.md#validation-task) can control how often the service is run.

##Non-Completed STS Jobs

Each time the validation service runs, it will query the database to find all non-completed STS jobs.
The service will compile a list of all DATASET and USER retention jobs that are either missing a status or are listed as pending, as well as all GLOBAL retention jobs that for the last 24 hours are either missing a status or listed as pending. 

##Validation Service Requests
The validation service can be invoked with an empty POST request to the endpoint: `events/validation`.

If the request is successful, an acknowledgement will be returned and the validation service will run asynchronously.