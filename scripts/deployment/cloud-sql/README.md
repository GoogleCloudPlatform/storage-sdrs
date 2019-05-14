# Introduction

The deployment creates a CloudSQL instance to use private IP.
It creates a MYSQL_5_7 master instance with a failover in different region, a read replica and root user.
- [CloudSQL Replication Options](https://cloud.google.com/sql/docs/mysql/replication/)
- [CloudSQL High Availability Configuration](https://cloud.google.com/sql/docs/mysql/high-availability)


## Prerequisites
- Install [gcloud](https://cloud.google.com/sdk)
- Create a [GCP project, set up billing, enable requisite APIs](../project/README.md)
- Configure [Private Service Access](https://cloud.google.com/vpc/docs/configure-private-services-access])
  (Private services access is a private connection between your VPC network and a network owned by Google or a third party)


## Deployment

### Resources

1. [sqladmin.v1beta4.instance](https://cloud.google.com/sql/docs/mysql/admin-api/v1beta4/instances)
2. [sqladmin.v1beta4.database](https://cloud.google.com/sql/docs/mysql/admin-api/v1beta4/databases)
3. [sqladmin.v1beta4.user](https://cloud.google.com/sql/docs/mysql/admin-api/v1beta4/users)


### Properties

See the `properties` section in the schema file(s)

- [CloudSQL](gs://sdrs/deployment-manager/CloudSQL/mysql.jinja.schema)



### Usage Instructions


1. Clone the [Deployment Manager Scripts](https://github.com/GoogleCloudPlatform/storage-sdrs.git)

```shell
    git clone https://github.com/GoogleCloudPlatform/storage-sdrs.git
```

2. Change directory to cloud-sql

```shell
    cd ~/storage-sdrs/scripts/deployment/cloud-sql
```

3. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp mysql.yaml my_cloudsql.yaml
```

4. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

```shell
    vim my_cloudsql.yaml  # <== change values to match your GCP setup
    imports:
      - path: mysql.jinja
    resources:
      - name: sdrs
        type: mysql.jinja
        properties:
          cloudsql:
            tier: db-n1-standard-1 # <== change values to match your desired Instance tier
            region: us-central1  # <== change values to match your desired region
            zone: us-central1-c  # <== change values to match your desired zone from the region above
            dataDiskSizeGb: 150
            privateNetwork: https://www.googleapis.com/compute/v1/projects/my-project-id/global/networks/my-vpc-name  # Edit it to run in a different GCP Project and VPC. Modify YOUR_PROJECT_NAME and YOUR_VPC_NAME to match yours. https://www.googleapis.com/compute/v1/projects/YOUR_PROJECT_NAME/global/networks/YOUR_VPC_NAME

          database:
            name: sdrs
          dbUser:
            user: root
            password:      # <== Put a temporary password **to be DELETED after successful deployment.**
          failover: true   # <== change values accordingly. "false" means No High Availability
          readReplicas: 1  # <== change values accordingly. "1" means single replica, "0" means No replicas.

```



5. Create your deployment as described below, replacing <YOUR_DEPLOYMENT_NAME>
   with your with your own deployment name

```shell
    gcloud deployment-manager deployments create <YOUR_DEPLOYMENT_NAME> \
        --config my_cloudsql.yaml
```

6. To list your deployment:

```shell
    gcloud deployment-manager deployments list
```

7. To see the details of your deployment:

```shell
    gcloud deployment-manager deployments describe <YOUR_DEPLOYMENT_NAME>
```

8. In case you need to update your deployment:

```shell
    gcloud deployment-manager deployments update <YOUR_DEPLOYMENT_NAME> \
      --config my_cloudsql.yaml   # <== Examples would be update my_cloudsql.yaml database property "failover: true" to "failover: false"

```

9. In case you need to delete your deployment:

```shell
    gcloud deployment-manager deployments delete <YOUR_DEPLOYMENT_NAME>
```

10. Set "log_bin_trust_function_creators" to "On" via GCP console [manually](https://cloud.google.com/sql/docs/mysql/flags)




## Notes/Considerations:

1. The deployment creation will fail with the following errors if the  password is not provided in the property password of my_cloudsql.yaml (step #4)

ERROR: (gcloud.deployment-manager.deployments.create) Error in Operation [operation-1550602726211-58243d4af00fd-fbd71850-f38fcb5a]: errors:
- code: MANIFEST_EXPANSION_USER_ERROR
location: /deployments/db01/manifests/manifest-1550602726354
message: |-
  Manifest expansion encountered the following errors: Invalid properties for 'mysql.jinja':
  None is not of type 'string' at ['dbUser', 'password']
   Resource: sdrs Resource: config


   To fix these error provide the property password in mysql.yam file and re-run the deployment.

IMPORTANT: After the successful launch of deployment **remove the password from the my_cloudsql.yaml file**


2. While trying to create Trigger on table, once the deployment is successfully created, you may encounter the following error.

ERROR 1419 (HY000) at line 83: You do not have the SUPER privilege and binary logging is enabled (you *might* want to use the less safe log_bin_trust_function_creators variable)

This is because the Deployment Scripts are not able to set log_bin_trust_function_creators to true.
This could be done via UI. [https://stackoverflow.com/questions/47359508/cant-create-trigger-on-mysql-table-within-google-cloud]
