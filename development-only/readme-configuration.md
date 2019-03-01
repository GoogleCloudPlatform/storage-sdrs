# Configuration Service

## REST API
The API is fully documented in openapi 2.0 (Swagger) format within `/scripts/deployment/openapi/openapi.yaml`
It can be visually inspected with the Swagger editor available at `https://editor.swagger.io/` 

### Retention Rules
CRUD operations are supported on Retention Rules.
Successful operations will return a 200 response with a JSON payload.

---

#### Create Retention Rule: `POST /retentionrules`
##### Fields and Validation
Body Params:

`type`: A string matching either `GLOBAL` or `DATASET`. `DATASET` rules require a `projectId` and `dataStorageName`. 

`dataStorageName`: A string indicating where this rule will take effect. It must begin with the storage prefix `gs://` and containing at least one segment indicating the bucket name. Segments are separated using `/`.

`projectId`: A string indicating the project where this rule will take effect. This is the GCP project name.

`retentionPeriod`: A number indicating the number of days to retain objects. Must be between 0 and 1095. 

`dataSetName`: A name for the dataset covered by this rule.

##### Errors
`400`: 
* No more than one retention rule can be created with the same `type`, `dataStorageName`, and `projectId`.
* Validation errors

---

#### Read Retention Rule: `GET /retentionrules?{params}`
##### Fields and Validation
Query Params:

`type`: The `type` of the requested rule.

`projectId`: The `projectId` of the requested rule.

`dataStorageName`: The `dataStorageName` of the requested rule.

##### Errors
`400`: 
* Validation errors

`404`:
* No rule exists matching the provided values.

---

#### Update Retention Rule: `PUT /retentionrules/{ruleId}`
##### Fields and Validation
Body Params:

`ruleId`: A number indicating the id of the rule to update. 

`retentionPeriod`: The new retention period.

##### Errors
`400`: 
* Validation errors
* No rule exists with the specified id.

---

#### Delete Retention Rule: `DELETE /retentionrules?{params}`
##### Fields and Validation
Query Params:

`type`: The `type` of the requested rule.

`projectId`: The `projectId` of the requested rule.

`dataStorageName`: The `dataStorageName` of the requested rule.

##### Errors
`400`: 
* Validation errors

`404`:
* No rule exists matching the provided values.

---
