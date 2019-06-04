# Commmand Line Interface (CLI) for SDRS Job Pool Management

This command line interface is designed to help you manage SDRS.  
It is intended to be used during the provisioning phase - post bucket creation, 
but before SDRS executes retention.

It allows you to either create or destroy an STS "Job Pool" of 25 jobs per bucket that will be  
reutilized over the lifetime of the bucket by SDRS to enforce retention.   

The 'create' command must be run one time only, once per bucket to create an STS Job Pool for that bucket.  
The 'delete' command should be run one time upon retiring a bucket from the purview of SDRS for retention enforcement.   


## Prerequisite - Setting up your Google Credential Environment

1) Install the Google Cloud [SDK](https://cloud.google.com/sdk/install)  
2) Setup your default application credentials by running the following command below. [See](https://cloud.google.com/sdk/gcloud/reference/beta/auth/application-default/login) for details.  

```
gcloud beta auth application-default login
```

Note this command line tool relies on using your Application Default Credentials (ADC) to implicitly  
find your active credentials from your environment as long as the GOOGLE\_APPLICATION\_CREDENTIALS variable is set.  
  
This implicit strategy may or may not be suitable for your production environment.  
Please [see](https://cloud.google.com/docs/authentication/) for more details on authentication strategies.  
Please also [see](https://cloud.google.com/docs/authentication/getting-started) and [also](https://cloud.google.com/docs/authentication/production#obtaining_and_providing_service_account_credentials_manually) for more credential related information.  

While the ADC strategy utilized here has the advantage of allowing authentication to “just work” it has the disadvantage of masking the particular account that is active.  
Prior to running this command line interface, it is advisable to run the following command to explicitly verify what credentialed account is currently active. 

```
 gcloud auth list

```

## Service Account Permissions


Note, in order to run this CLI, whatever account is active will need several service account permissions.  

1) Cloud Storage access: You must be the Owner or Editor of the project that manages the data transfer.  
This project does not have to be associated with either the source or sink.    

2) Source and sink access: In order for Storage Transfer Service to access the data source and the data sink,  
the service account (SA) must have source permissions and sink permissions.  

3) The SA needs the [Service Account Token Creator Role](https://cloud.google.com/iam/docs/service-accounts#the_service_account_token_creator_role) in order to sign JWTs.  

Please [see](https://cloud.google.com/iam/docs/understanding-service-accounts) for more details on Service Accounts.   


## Setting up a Python development environment

1) Follow these Google [instructions](https://cloud.google.com/python/setup) to setup your Python development environment.  
2) Then, run the following command on your prompt to install the needed libraries.   

```
    pip install --upgrade google-cloud-storage   
    pip install --upgrade google-api-python-client oauth2client
```

## Running the Program

Example command line arguments from the prompt:

```
    python command_line.py create sdrs-server 2019/05/20 ds-dev-rpo ds-bucket-dev
    
    python command_line.py delete sdrs-server 2019/05/20 ds-dev-rpo ds-bucket-dev
```


