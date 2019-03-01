# SDRS Application deployment on Managed Instance Group

### Supplementary Data Retention Service - SDRS



1. Overview
2. Glossary
3. Prerequisites
4. Deployment Steps



Overview

This document provides design, architecture and implementation details of SDRS application on Google Cloud Platform. The focus of this document is on deployment. Also aimed at deployment in an enterprise environment

SDRS is a containerized Java server application deployed in GCP running on Autoscaled Managed Instance Groups behind an Internal Load Balancer. The application exposes services through REST API that is managed by Google Endpoints.




Glossary

[SDRS](): Supplementary Data Retention Service

[Managed Instance Group (MIG)](https://cloud.google.com/compute/docs/instance-groups/): A managed instance group is a group of identical instances. You control a managed instance group as a single entity. If you wanted to make changes to instances that are part of a managed instance group, you would make the change to the whole instance group.

[Internal Load Balancing](https://cloud.google.com/load-balancing/docs/internal/): Internal TCP/UDP Load Balancing is a regional load balancer that enables you to run and scale your services behind a private load balancing IP address that is accessible only to your internal virtual machine instances.

[Google Container Registry](https://cloud.google.com/container-registry/): Container Registry is a private container image registry that runs on Google Cloud Platform.

[Google Cloud Storage](https://cloud.google.com/storage/): Cloud Storage allows world-wide storage and retrieval of any amount of data at any time. You can use Cloud Storage for a range of scenarios including serving website content, storing data for archival and disaster recovery, or distributing large data objects to users via direct download.

[Identity and Access Management(IAM)](https://cloud.google.com/iam/) : Cloud IAM, which lets you manage access control by defining who (identity) has what access (role) for which resource. Cloud IAM lets you grant granular access to specific GCP resources and prevent unwanted access to other resources. Cloud IAM lets you adopt the security principle of least privilege, so you grant only the necessary access to your resources.

[Service Account](https://cloud.google.com/iam/docs/service-accounts): A service account is a special Google account that belongs to your application or a virtual machine (VM), instead of to an individual end user. Your application uses the service account to call the Google API of a service, so that the users aren't directly involved.

[Google Cloud SDK](https://cloud.google.com/sdk/): The Cloud SDK is a set of tools for Cloud Platform. It contains gcloud, gsutil, and bq command-line tools, which you can use to access Google Compute Engine, Google Cloud Storage, Google BigQuery, and other products and services from the command-line. You can run these tools interactively or in your automated scripts.

[gcloud](https://cloud.google.com/sdk/gcloud/): The gcloud command-line interface is a tool that provides the primary CLI to Google Cloud Platform. You can use this tool to perform many common platform tasks either from the command-line or in scripts and other automations.

[gsutil](https://cloud.google.com/storage/docs/gsutil): gsutil is a Python application that lets you access Cloud Storage from the command line. It is installed as part of Google Cloud SDK.

[Virtual Private Cloud](https://cloud.google.com/vpc/): Google Cloud Platform (GCP) Virtual Private Cloud (VPC) provides networking functionality to Compute Engine virtual machine (VM)

[Peering](https://cloud.google.com/vpc/docs/vpc-peering): Google Cloud Platform (GCP) Virtual Private Cloud (VPC) Network Peering allows private RFC 1918 connectivity across two VPC networks regardless of whether or not they belong to the same project or the same organization.

[Google Cloud Functions](https://cloud.google.com/functions/): Google Cloud Functions is a lightweight compute solution for developers to create single-purpose, stand-alone functions that respond to Cloud events without the need to manage a server or runtime environment.

Cloud Function Private VPC connectivity: In alpha release.

[Cloud SQL Private IP Connectivity](https://cloud.google.com/vpc/docs/private-access-options#service-networking): Google CloudSQL support private service access. A private connection is implemented as a VPC Network Peering connection between your VPC network and the service producer's VPC network. The service producer's network is created exclusively for you, and it is not shared with other customers.


High-level Deployment Architecture
<< MIG Architecture Diagram - OLD picture, to be updated. >>

Prerequisites

1. [Google Cloud SDK](https://cloud.google.com/sdk) installed.



Deployment

The following sections provide a walkthrough on how to deploy Cloud Endpoints starting from GCP project creation up to deploying the Endpoints and making updates to the backend software.  

1. Create a GCP Project.

        gcloud projects create PROJECT_ID

Where PROJECT_ID is the ID for the project you want to create. A project ID must start with a lowercase letter, and can contain only ASCII letters, digits, and hyphens, and must be between 6 and 30 characters.

Each GCP project has:

* A project name, which you provide.
* A project ID, which you can provide or GCP can provide for you.
* A project number, which GCP provides.

[Security Considerations](https://cloud.google.com/iam/docs/using-iam-securely)


Follow the recommended security best practices when using Cloud IAM and Google Cloud in general.

* Principle of least privilege
* Rotate your service account keys using the [Cloud IAM service account API](https://cloud.google.com/iam/reference/rest/v1/). You can rotate a key by creating a new key, switching applications to use the new key and then deleting old key.
* Do not delete service accounts that are in use by running instances. This could result in all or parts of your application to fail if you have not transitioned to using an alternative service account first.
* Don’t check in the service account keys into the source code or leave them in the Downloads directory.
* Audit who has the ability to change your Cloud IAM policies on your projects.


2. Create Network.

  Create a custom mode VPC via GCP Console or gcloud command.

    gcloud compute networks create <YOUR_VPC_NAME> --subnet-mode=custom


3. Create a subnet.

      gcloud compute networks subnet create <YOUR_SUBNET_NAME> --network=<YOUR_VPC_NAME> --region=<YOUR_REGION> --range=<YOUR_IP_CIDR_RANGE> --enable-private-ip-google-access


4. Configure [Private Google Access](https://cloud.google.com/vpc/docs/configure-private-services-access) for CloudSQL

   Considerations:

 -   Select a range that doesn't overlap with existing allocated ranges, subnets, or custom static routes. No two ranges can overlap.
-   If you're using an  [auto mode](https://cloud.google.com/vpc/docs/vpc#subnet-ranges)  VPC network, you can't create an allocated range that matches or overlaps with`10.128.0.0/9`. This range is for  [automatically created subnets](https://cloud.google.com/vpc/docs/vpc#ip-ranges).
-   Select a CIDR block that large enough to meet your current and future needs. If you later find that the range isn't sufficient in size,  [expand](https://cloud.google.com/vpc/docs/configure-private-services-access#modify-ip-range)  the range if possible. Although you can assign multiple allocations to a single service producer, Google enforces a quota on the number of IP address ranges that you can allocate, not the size (netmask) of each range.
-   Don't reuse the same allocated range for multiple service producers. Although it's possible, doing so can lead to IP address overlap. Each service producer has visibility only into their network and can't know which IP addresses other service producers are using.
-   You can only assign one CIDR block to an allocated range when you create the allocation. If you need to  [expand](https://cloud.google.com/vpc/docs/configure-private-services-access#modify-ip-range)  the IP address range, you can't add more blocks to an allocation. Instead, you can create another allocation or recreate the existing one by using a larger block that encompasses the new and existing ranges.


More information on [GCP Regions](https://cloud.google.com/compute/docs/regions-zones/).





5. Create SDRS Infrastructure


    1. Create a [cloudSQL](./cloudSQL/README.md) instance. (Takes about 10-15 minutes)

   2. Launch a [Managed Instance Group cluster](./mig_create_and_update/README.md) (Takes about 5 minutes )

         Follow the steps section I (subsection 1 through 11) to create a Managed Instance Group.

   3. Deploying the Endpoints configuration using the following command.

          gcloud endpoints services deploy openapi.yaml

Cloud Endpoints is a distributed API management system that helps you secure, monitor, analyze, and set quotas on your APIs using the same infrastructure Google uses for its own APIs. Endpoints works with the Extensible Service Proxy (ESP) to provide API management. It provides an API console, hosting, logging, monitoring, and Authenticating API users to help you create, share, maintain, and secure your APIs.

You can view the Endpoints service configuration on the Endpoints > Services page in the GCP Console.


OpenAPI configuration file, openapi.yaml is based on OpenAPI Specification v2.0.

Using your [own DNS as the name of your Cloud Endpoints Service](https://cloud.google.com/endpoints/docs/openapi/verify-domain-name).
 * If you already own a domain name (such as `mycompany.com`), and you want to use this domain name as the name of your Cloud Endpoints service, you must prove that you are authorized to use the domain.


[Enabling SSL](https://cloud.google.com/endpoints/docs/openapi/enabling-ssl) for Cloud Endpoints.

You may need to enable an SSL port for your deployed Endpoints service so that it can serve HTTPS requests. Refer to the documentation for



Application Update

6. Update the SDRS application using MIG Updater.
Follow the steps section II (subsection 1 through 4) to update the version of new software on the existing Managed Instance Group.



##### Notes:

1. The Managed Instance Group updater does NOT cause a change of Internal Load Balancer Private IP or Endpoints configuration change.
2.  The Managed Instance Group Infrastructure is immutable i.e. when you update the MIG with a newer software version the VM instances are replaced by the new VM instances running newer software version.
3. You can not stop or restart VM instances in a MIG.
