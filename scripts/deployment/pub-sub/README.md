# Introduction


This [Google Cloud Deployment Manager](https://cloud.google.com/deployment-manager/overview) template
deploys a PubSub topic and creates a subscription.


## Prerequisites
- Install [gcloud](https://cloud.google.com/sdk)
- Create a [GCP project, set up billing, enable requisite APIs](../project/README.md)


## Deployment

### Resources

1. [pubsub-v1:projects.topics](https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.topics)
2. [pubsub-v1:projects.subscriptions](https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions)


### Properties


- [ackDeadlineSeconds](https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions/create)
- [retainAckedMessages](https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions/create)



### Usage Instructions


1. Clone the [Deployment Manager Scripts](https://github.com/GoogleCloudPlatform/storage-sdrs.git)

```shell
    git clone https://github.com/GoogleCloudPlatform/storage-sdrs.git
```

2. Change directory to pub-sub

```shell
    cd ~/storage-sdrs/scripts/deployment/pub-sub
```

3. Copy the example DM config to be used as a model for the deployment as follows

```shell
    cp pubsub.yaml my_pubsub.yaml
```

4. Change the values in the config file to match your specific GCP setup.
   Refer to the properties in the schema files described above. Use your favorite
   editor vim or nano to edit the file.

```shell
    vim my_pubsub.yaml  # <== change values to match your GCP setup
    imports:
    - path: pubsub.jinja

    resources:
    - name: pubsub
      type: pubsub.jinja
      properties:
        ackDeadlineSeconds: 20
        retainAckedMessages: false
```


5. Create your deployment as described below, replacing <YOUR_DEPLOYMENT_NAME>
   with your with your own deployment name

```shell
    gcloud deployment-manager deployments create <YOUR_DEPLOYMENT_NAME> \
        --config my_pubsub.yaml
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
      --config my_pubsub.yaml  

```

9. In case you need to delete your deployment:

```shell
    gcloud deployment-manager deployments delete <YOUR_DEPLOYMENT_NAME>
```
