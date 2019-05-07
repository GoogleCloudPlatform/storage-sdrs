# Overview
The simple tool creates a STS job pool consisting of 25 STS daily recurring jobs, 24 jobs scheduled at every hour for dataset rule and 1 job scheduled at 23:59:59 for default rule. Once the STS jobs are created, it then store the info in the pooled_sts_job table. The tool is meant for development use.  
# Instruction 
1. Download the pool_management directory to a machine that can access the database
1. Create a virtual env using [virtualenv](https://virtualenv.pypa.io/en/latest/)
1. Run ```$> pip install -r requiements.txt ``` to install the dependency
1. Change the config.json to your needs. Make sure you don't change the file name
1. Run ```$> python sts_job_pool.py```
1. Check pooled_sts_job table and verify the job info are properly stored in the table

# Configuration
```
{
  "database": {
    "host": "35.202.0.248",  # host of database 
    "schema": "sdrs",  # db schema name
    "user": "sdrs",  # db user name
    "password": "sdrsgreat"  # db user password
  },
  "pool": [ # array of pools you want to create
    {
      "project_id": "sdrs-server", 
      "buckets": [ # arry of buckets you'd want to provisioing the pool
        "sdrs-func-testing-log-cat1",
        "sdrs-func-testing-user-steve"
      ]
    }
  ],
  "shadow_bucket_ext": {  # shadow bucket extention setting
    "ext": "shadow",
    "suffix": true
  }
}

```
