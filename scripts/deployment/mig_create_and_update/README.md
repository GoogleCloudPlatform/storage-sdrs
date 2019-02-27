# Introduction

The deployment creates and updates an Auto-Scaled, Regional Managed Instance Group with an Internal Load Balancer.

IGM - Instance Group Manager

- [Instance Templates](https://cloud.google.com/compute/docs/instance-templates/)
- [Managed Instance Group](https://cloud.google.com/compute/docs/instance-groups/)
- [Autoscaling](https://cloud.google.com/compute/docs/autoscaler/)
- [Internal Load Balancing](https://cloud.google.com/sql/docs/mysql/high-availability)
- [Managed Instance Group Updater](https://cloud.google.com/compute/docs/instance-groups/updating-managed-instance-groups)

## Prerequisites
- Install [Google Cloud SDK](https://cloud.google.com/sdk)
- Create a [GCP project, set up billing, enable requisite APIs](../project/README.md)


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


- [Instance Group Manager](gs://sdrs/deployment-manager/MIG_Create_Update/igm.jinja.schema)



### Usage Instructions

### Creating a Managed Instance Group


1. Download the [Deployment Manager Scripts](gs://sdrs/deployment-manager/MIG_Create_Update)

```shell
    gsutil cp -r gs://sdrs/deployment-manager/MIG_Create_Update .
```

2. Change directory to MIG_Create_Update

```shell
    cd ./MIG_Create_Update
```

3. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp igm.yaml my_igm.yaml
```

4. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

```shell
vim my_igm.yaml   # <== change values to match your GCP setup
imports:
- path: igm.jinja

resources:
- name: igm
  type: igm.jinja
  properties:
    region: us-central1
    maxNumReplicas: 5
    machineType: n1-standard-1  # <== change values to match your desired Instance tier
    value: gs://YOUR_BUCKET/YOUR_FOLDER/startup.sh
    targetSize: 2
    externalIp: False
    email: default # <== Service Account email address
    network: https://www.googleapis.com/compute/v1/projects/sdrs-server/global/networks/sdrs-server-dev-vpc
    subnetwork: https://www.googleapis.com/compute/v1/projects/sdrs-server/regions/us-central1/subnetworks/subnet-b
```



5. Create your deployment as described below, replacing <YOUR_DEPLOYMENT_NAME>
   with your with your own deployment name

```shell
    gcloud deployment-manager deployments create <YOUR_DEPLOYMENT_NAME> \
        --config my_igm.yaml
```
6. To list your deployment:

```shell
    gcloud deployment-manager deployments list
```

7. To see the details of your deployment:

```shell
    gcloud deployment-manager deployments describe <YOUR_DEPLOYMENT_NAME>
```

8. In case you need to delete your deployment:

```shell
    gcloud deployment-manager deployments delete <YOUR_DEPLOYMENT_NAME>
```


### Updating the Managed Instance Group with new Software version (container image)

9. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp version.yaml current_version.yaml
```

10. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

   ```shell
   vim current_version.yaml
   imports:
   - path: instance-template.jinja

   resources:
   - name: sdrs
     type: instance-template.jinja
     properties:
       targetSize: 2
       maxNumReplicas: 5
       machineType: n1-standard-1
       value: gs://YOUR_BUCKET/YOUR_FOLDER/startup_new.sh # <== change values to match your startup script and it's location.
       externalIp: False
       region: us-central1
       email: default
       network: https://www.googleapis.com/compute/v1/projects/sdrs-server/global/networks/sdrs-server-dev-vpc
       subnetwork: https://www.googleapis.com/compute/v1/projects/sdrs-server/regions/us-central1/subnetworks/subnet-b
    ```

  11. Create a new Instance Template as deployment as described below, replacing <YOUR_NEW_DEPLOYMENT_NAME>
       with your with your own deployment name

    ```shell
        gcloud deployment-manager deployments create <YOUR_NEW_DEPLOYMENT_NAME> \
            --config current_version.yaml
    ```
     This creates an Instance Template with updated version of container image as specified in "startup_new.sh"


  12. Update the Managed Instance Group Deployment (created in step 5) using the new Instance Template (created in step 11) as follows

  ```shell
  gcloud beta compute instance-groups managed rolling-action start-update <YOUR_DEPLOYMENT_NAME>-igm –version template=<YOUR_NEW_DEPLOYMENT_NAME>-it –region=us-central1
  ```
