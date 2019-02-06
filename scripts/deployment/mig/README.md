# Introduction #

The deployment creates an Auto-Scaled, Regional Managed Instance Group with an Internal Load Balancer.


igm - Instance Group Manager

## Update igm.yaml accordingly. ##

### Instructions on how to use the igm.yaml (configuration file) ###

1. Project name, Region and Subnet name in url of property subnetwork in igm.yaml needs to be provided.
2. Project name and VPC name in url of property network in igm.yaml needs to be provided.

### Usage: ###

igm.yaml, igm.jinja and igm.jinja.schema needs to be in the same directory.

gcloud deployment-manager deployments create my-mig-deployment --config igm.yaml

my-mig-deployment is the deployment name - To be provided by the user.
