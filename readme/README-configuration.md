# Configuration Service

## REST API
The API is fully documented in openapi 2.0 (Swagger) format within `/scripts/deployment/openapi/openapi.yaml`
It can be visually inspected with the Swagger editor available at `https://editor.swagger.io/` 

All responses will include a JSON payload including at least UUID corresponding to that specific request.
All error responses will include an error message indicating the reason for the error.

A `correlation-uuid` header may be provided. If no value is provided, one will be generated and returned by the server.

### Authentication
Operations are authenticated with JWTs included as a request header, like:
`Authorization: Bearer <token>`. 


### Retention Rules
CRUD operations are supported on Retention Rules.

---

#### Create Retention Rule: `POST /retentionrules`
##### Fields and Validation
JSON Body Params:

`type`: A string matching `GLOBAL`, `DATASET` or `DEFAULT`. `DATASET` and `DEFAULT` rules require a `projectId` and `dataStorageName`. 

`dataStorageName`: A string indicating where this rule will take effect. It must begin with the storage prefix `gs://` and containing at least one segment indicating the bucket name. Segments are separated using `/`.

`projectId`: A string indicating the project where this rule will take effect. This is the GCP project name.

`retentionPeriod`: A number indicating the number of days to retain objects. Must be between 0 and 1095. 

`retentionPeriodUnit`: Optional. A string indicating retention period unit. `DAY` or `MONTH`.

`dataSetName`: Optional. A name for the dataset covered by this rule.

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
JSON Body Params:

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
