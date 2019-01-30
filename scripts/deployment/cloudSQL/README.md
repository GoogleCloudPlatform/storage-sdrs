# Introduction #

The deployment creates a MYSQL_5_7 master instance with a failover in different region, a read replica and root user.

Deployment

## Update mysql.yaml accordingly. ##

### Instructions on how to use the mysql.yaml (configuration file) ###

1. Specify a root password for property [cloudsql.dbUser.password]for root user in mysql.yaml
2. Modify other properties for e.g. tier, dataDiskSizeGb, failover, number of readReplicas according to your requirement.
3. IMPORTANT: After the successful launch of deployment **remove the password from the mysql.yaml file**
4. Project name and VPC name in url of property privateNetwork in mysql.yaml needs to be provided.

## Usage: ##

mysql.yaml, mysql.jinja and mysql.jinja.schema needs to be in the same directory.

gcloud deployment-manager deployments create my-deployment --config mysql.yaml

my-deployment is the deployment name - To be provided by the user.

Note:

The deployment creation will fail and the script will throw an error if the  password is not provided in the property password of mysql.yaml (step #1)
