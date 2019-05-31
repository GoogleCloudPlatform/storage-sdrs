# Python Commmand Line Tool for the SDRS Job Pool

This command line tool is designed to help you create and provision an STS  
Job Pool for each bucket that SDRS will manage for retention. 

It allows you to either create or destroy an STS Job Pool of 25 jobs per bucket 

## Setting up a Python development environment

1) Follow these Google [instructions](https://cloud.google.com/python/setup) to setup your environment.  
2) Run the following command on your prompt  

```
    pip install --upgrade google-cloud-storage   
```

## Running the Program

Example command line arguments from the prompt:

```
    python command_line.py create sdrs-server 2019/05/20 ds-dev-rpo ds-bucket-dev
    
    python command_line.py delete sdrs-server 2019/05/20 ds-dev-rpo ds-bucket-dev
```


