# Introduction

The deployment Creates and Updates an Auto-Scaled, Regional Managed Instance Group with an Internal Load Balancer.

IGM - Instance Group Manager

Usage instructions section I (subsection 1 through 11) is for Managed Instance Group creation

Usage instructions section II (subsection 1 through 4) is for updating new version of the Application (Software Release) on existing MIG. For any given environment that has an existing MIG, you could directly execute steps section II (1 through 4) for subsequent software releases.

- [Instance Templates](https://cloud.google.com/compute/docs/instance-templates/)
- [Managed Instance Group](https://cloud.google.com/compute/docs/instance-groups/)
- [Autoscaling](https://cloud.google.com/compute/docs/autoscaler/)
- [Internal Load Balancing](https://cloud.google.com/sql/docs/mysql/high-availability)
- [Managed Instance Group Updater](https://cloud.google.com/compute/docs/instance-groups/updating-managed-instance-groups)

## Prerequisites
- Install [Google Cloud SDK](https://cloud.google.com/sdk)
- Create a [GCP project, set up billing, enable requisite APIs](../../README.md)


## Deployment

### Resources

1. [compute.v1.instanceTemplate](https://cloud.google.com/compute/docs/reference/rest/v1/instanceTemplates)
2. [compute.v1.regionInstanceGroupManagers](https://cloud.google.com/compute/docs/reference/rest/v1/regionInstanceGroupManagers)
3. [compute.v1.regionBackendService](https://cloud.google.com/compute/docs/reference/rest/v1/regionBackendServices)
4. [compute.v1.regionAutoscaler](https://cloud.google.com/compute/docs/reference/rest/v1/regionAutoscalers)
5. [compute.v1.forwardingRule](https://cloud.google.com/compute/docs/reference/rest/v1/forwardingRules)
6. [compute.v1.firewall](https://cloud.google.com/compute/docs/reference/rest/v1/firewalls)
7. [compute.v1.healthCheck](https://cloud.google.com/compute/docs/reference/rest/v1/healthChecks)



### Properties

See the `properties` section in the [schema file(s)](https://cloud.google.com/deployment-manager/docs/configuration/templates/using-schemas)

The property in the configuration file can be changed without changing the template file.

For example in configuration file (igm.yaml), the following properties could be modified to customize your deployment.

1. targetSize - Specifies the intended number of instances to be created from the instanceTemplate.
2. maxNumReplicas - The maximum number of instances that the autoscaler can scale up to.
3. machineType - Predefined machine types have a fixed collection of resources.
4. externalIp - VM gets only private IPs if value is set false
5. region - GCP Region
6. email - Service Account email.
7. network - VPC network name in a GCP Project
8. subnetwork - Subnetwork name within a VPC network
9. value - Google Cloud Storage bucket path to startup Scripts




### Usage Instructions

#### I. Creating a Managed Instance Group


1. Clone the [Deployment Manager Scripts](https://github.com/GoogleCloudPlatform/storage-sdrs.git)

```shell
    git clone https://github.com/GoogleCloudPlatform/storage-sdrs.git
```

2. Change directory to MIG_Create_Update

```shell
    cd ~/storage-sdrs/scripts/deployment/mig_create_and_update
```

3. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp igm.yaml <YOUR_FILE_NAME>.yaml
```

4. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

```shell
vim <YOUR_FILE_NAME>.yaml   # <== change values to match your GCP setup
imports:
- path: igm.jinja

resources:
- name: igm
  type: igm.jinja
  properties:
    region: us-central1
    maxNumReplicas: 5
    machineType: n1-standard-1  # <== change values to match your desired Instance tier
    value: gs://<YOUR_BUCKET>/<YOUR_FOLDER>/startup.sh
    targetSize: 2
    externalIp: False
    email: default # <== Service Account email address
    network: https://www.googleapis.com/compute/v1/projects/sdrs-server/global/networks/sdrs-server-dev-vpc # Edit it to run in a different GCP Project and VPC. Modify YOUR_PROJECT_NAME and YOUR_VPC_NAME to match yours. https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_NAME/global/networks/YOUR_VPC_NAME
    subnetwork: https://www.googleapis.com/compute/v1/projects/sdrs-server/regions/us-central1/subnetworks/subnet-b # Edit it to run in a different GCP Project and VPC. Modify YOUR_PROJECT_NAME, YOUR_REGION and YOUR_SUBNET_NAME to match yours. https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_NAME/regions/YOUR_REGION/subnetworks/YOUR_SUBNET_NAME
```

5. Modify env.txt accordingly.

```shell
vim ../scripts/env.txt   # <== change values to match your environment

HIBERNATE_CONNECTION_URL=jdbc:mysql://<your_host>:3306/<your_schema>
HIBERNATE_CONNECTION_USER=<your_db_user>
HIBERNATE_CONNECTION_PASSWORD=<your_db_password>
SDRS_PUBSUB_TOPIC_NAME=projects/<your_project_id>/topics/<your_topic>
GOOGLE_APPLICATION_CREDENTIALS=<path_to_your_credentials_json>
```

6. Upload the modified env.txt file to a pre-created GCS bucket.

```shell
    gsutil cp ../scripts/env.txt gs://<YOUR_ENV_VAR_BUCKET>/<YOUR_ENV_VAR_FOLDER>
```

7. Modify startup.sh accordingly.

   For specific instructions refer to inline comments in

   [startup.sh](./scripts/startup.sh)   


8. Upload the modified startup.sh file to a pre-created GCS bucket.

```shell
    gsutil cp ../scripts/startup.sh gs://<YOUR_STARTUP_SCRIPT_BUCKET>/<YOUR_STARTUP_SCRIPT_FOLDER>
```

9. Create your deployment as described below, replacing <YOUR_DEPLOYMENT_NAME>
   with your with your own deployment name

```shell
    gcloud deployment-manager deployments create <YOUR_DEPLOYMENT_NAME> \
        --config=<YOUR_FILE_NAME>.yaml
```
10. To list your deployment:

```shell
    gcloud deployment-manager deployments list
```
> At this point you have a MIG running in AutoScaling mode having an
> Internal Load Balancer (ILB) named <YOUR_DEPLOYMENT_NAME>-fr. From the
> GCP console note the IP assigned to the ILB. This would be passed in
> openapi.yaml configuration to deploy
> [Endpoints](https://cloud.google.com/endpoints/docs/openapi/)

11. To see the details of your deployment:

```shell
    gcloud deployment-manager deployments describe <YOUR_DEPLOYMENT_NAME>
```

12. In case you need to update your deployment:

```shell
    gcloud deployment-manager deployments update <YOUR_DEPLOYMENT_NAME> \
      --config <YOUR_FILE_NAME>.yaml   # <== Examples would be update targetSize, machineType or service account email.
```

13. In case you need to delete your deployment:

```shell
    gcloud deployment-manager deployments delete <YOUR_DEPLOYMENT_NAME>
```


#### II. Updating MIG with New Software Version

The steps listed below creates a new Instance Template with an updated version of software. This new instance template <YOUR_NEW_DEPLOYMENT_NAME>-it is then used to update the existing MIG <YOUR_DEPLOYMENT_NAME>-igm (created in step I.7 above) with the newer version of software release. You pass the startup_new.sh in property "value" (step II.2). The startup_new.sh is a copy of modified ../scripts/script.sh updated with a newer version of container image gcr.io/<YOUR_GOOGLE_PROJECT_ID>/YOUR_CONTAINER_IMAGE:TAG.

1. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp version.yaml <YOUR_CURR_VER_FILE_NAME>.yaml
```

2. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

   ```shell
   vim <YOUR_CURR_VER_FILE_NAME>.yaml
   imports:
   - path: instance-template.jinja

   resources:
   - name: sdrs
     type: instance-template.jinja
     properties:
       targetSize: 2
       maxNumReplicas: 5
       machineType: n1-standard-1
       value: gs://<YOUR_STARTUP_SCRIPT_BUCKET>/<YOUR_STARTUP_SCRIPT_FOLDER>/startup_new.sh # <== change values to match your startup script and it's location.
       externalIp: False
       region: us-central1
       email: default
       network: https://www.googleapis.com/compute/v1/projects/sdrs-server/global/networks/sdrs-server-dev-vpc # Edit it to run in a different GCP Project and VPC. Modify YOUR_PROJECT_NAME and YOUR_VPC_NAME to match yours. https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_NAME/global/networks/YOUR_VPC_NAME
       subnetwork: https://www.googleapis.com/compute/v1/projects/sdrs-server/regions/us-central1/subnetworks/subnet-b # Edit it to run in a different GCP Project and VPC. Modify YOUR_PROJECT_NAME, YOUR_REGION and YOUR_SUBNET_NAME to match yours. https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_NAME/regions/YOUR_REGION/subnetworks/YOUR_SUBNET_NAME
    ```

  3. Create a new Instance Template as deployment as described below, replacing <YOUR_NEW_DEPLOYMENT_NAME>
         with your with your own deployment name.

  ```shell
  gcloud deployment-manager deployments create <YOUR_NEW_DEPLOYMENT_NAME> --config=<YOUR_CURR_VER_FILE_NAME>.yaml
  ```

  This creates an Instance Template with updated version of container image as specified in 'startup_new.sh'.



  4. Update the Managed Instance Group Deployment (created in step 5) using the new Instance Template (created in step 11) as follows

  ```shell
  gcloud beta compute instance-groups managed rolling-action start-update <YOUR_DEPLOYMENT_NAME>-igm –version template=<YOUR_NEW_DEPLOYMENT_NAME>-it –region=us-central1
  ```

  This updates your MIG with new version of your Application container image.
