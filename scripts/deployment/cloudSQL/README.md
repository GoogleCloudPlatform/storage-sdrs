Introduction

The deployment creates a MYSQL_5_7 master instance with a failover in different region, a read replica and root user.

Deployment

## Update mysql.yaml accordingly.

Before creating the deployment, insert a root password in mysql.yaml and launch the deployment. After the deployments successfully created remove the password.

Usage:

gcloud deployment-manager deployments create dev --config mysql.yaml

dev is the deployment name - To be provided by the user.
