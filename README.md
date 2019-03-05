# Google Cloud Storage - Supplementary Data Retention Service (SDRS)

SDRS allows an organization to manage the Time to Live (TTL) for objects in Google Cloud Storage (GCS) 
according to retention policies based off of the creation time encoded in partition prefixes. 

For example, the official age of an object could exist as follows: 
bucketX/datasetY/{yyyy}/{mm}/{dd}/{hh}/log.txt

In this example, the information encoded in the object name rather than the GCS object metadata creation time serves to define its age. 
An organization can define a TTL for datasets and thereby reliably enforce object retention based of the encoded creation time. 
 
At the most fundamental level, SDRS enforces object retention by mapping policy rules defining the time-to-live (TTL) for datasets existing in GCS buckets. 
Note, for scenarios where GCS object retention management can rely solely on object creation time rather than an encoded prefix, please see:
Object Lifecycle Management https://cloud.google.com/storage/docs/lifecycle

## High Level Architecture

SDRS is an open-source GCP GitHub project https://github.com/GoogleCloudPlatform/storage-sdrs

SDRS exists in two main parts:

1) A server side service exposing functionality through a RESTful API 
2) A sample client demonstrating interaction with the server side services

SDRS is primarily written in the Java 8 and Python 3 programming languages. 
Maven is used as the Java build management tool. 
Deployment Manager is used as the DevOps Cloud Orchestration tool. 

## Key GCP Technologies Utilized in SDRS

Managed Instance Groups (MIGs) - https://cloud.google.com/compute/docs/instance-groups/

Cloud Functions (FaaS) https://cloud.google.com/functions/

Cloud Pub/Sub https://cloud.google.com/pubsub/

Cloud Endpoints https://cloud.google.com/endpoints/

Google Stackdriver https://cloud.google.com/stackdriver/

Cloud SQL https://cloud.google.com/sql/

Cloud Scheduler https://cloud.google.com/scheduler/

Storage Transfer Service (STS) https://cloud.google.com/storage-transfer/docs/overview

Cloud Deployment Manager https://cloud.google.com/deployment-manager/


## Getting Started with SDRS

To get started, clone the project from Google Cloud Platform's Github site here - https://github.com/GoogleCloudPlatform/storage-sdrs
The full source code for both the server along with a sample client are included in the project. 
Build and deployment instructions are included as well. 

## Local Development/Build Steps

The instructions in this section describe how to quickly get started and deploy SDRS to a DEV GCP environment. 

TODO - need to fill in this section 

## Enterprise Deployment Steps to Google Cloud Platform (GCP)

The instructions in this section serve as an example for deploying SDRS to a full production like GCP environment.

Deploying SDRS to a production like environment should occur in the following order:

### Deploying the Server Side Components 

1) Cloud SQL Infrastructure Deployment see [the Cloud SQL Deployment README](./scripts/deployment/cloudSQL/README.md).
2) MySQL DDL execution see [the MySQL Schema (./scripts/sql/retention_schema.sql).
3) Pub/Sub Infrastructure Deployment [the Server Pub/Sub Deployment README](./scripts/deployment/pub-sub/README.md).
4) MIG Deployment [the Server Pub/Sub Deployment README](./scripts/deployment/mig_create_and_update/README.md).

### Deploying the Sample Client Side Components 

1) Cloud Function Deployment (Includes Client side Pub/Sub triggers)
2) Cloud Scheduler Crontab creation by way of the GCP Console UI

## Server - Configuration Service Details 

The Configuration Service is a server side component that is responsible for exposing
a RESTful API that handles CRUD operations for the retention policies. 

The Configuration Service is the key touchpoint to SDRS when provisioning or updating retention policies. 
For more details, see [the Configuration Service README](./README-configuration.md).

## Server - Execution Service Details 

The Execution Service is a server side component that is responsible for exposing
a RESTful API that manages the execution of retention policy enforcement (i.e. the deletion of objects). 
The Execution Service is capable of enforcing object retention for three specific use cases:

1) Retention Policies - dataset specific policies provisioned by way of the configuration service
2) Default/Global Policy - a global dataset rule that serves as a catch-all for datasets not already covered by specific retention policies
3) On-demand Delete Markers - ad hoc requests to delete specific datasets immediately

For more details, see [the Execution Service README](./README-executor.md).

## Server - Validation Service Details 

The Validation Service is a server side component that is responsible for exposing
a RESTful API that manages the execution of jobs that serve to validate the completion of already requested enforcement processes. 
For more details, see [the Validation Service README](./README-validation.md).

## Server - Notification Service Details

The Notification Service is a server side component that is responsible for broadcasting notifications of SDRS events to 
interested parties by way of Pub/Sub
For more details, see [the Notification Service README](./README-notification.md).

## Sample Client Cloud Functions Deployment & Details

The Cloud Functions serve as an example client demonstrating how to interact with the server side SDRS RESTful API. 
Included in the code base are Cloud Functions that invoke the Configuration, Execution, 
Validation, and Notification services. 
For more details, see [the Client Cloud Functions README](./README-cloudFunctions.md).

## Sample Cloud Scheduler Details

SDRS has several functional areas that can be scheduled on a recurring frequency.  The scheduler strategy in this sample uses
Cloud Scheduler as decoupled, externally managed crontab service that invokes a Pub/Sub topic that invokes a Cloud Function to invoke
SDRS Execution and Validation functionality on a scheduled basis.
  
For more details, see [the Cloud Scheduler README](./README-cloudScheduler.md).
