import os
import requests

#for url encoding 
import urllib.parse

import flask

def rpo_listener(object_finalize_event, context):
    
    project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
    print(f"Project : {project}")
    print(f"Processing object_finalize_event: {object_finalize_event['name']}.")
    name = object_finalize_event['name']
    print( name.rfind('_'))
    split_index = name.rfind('_')
    ttl = name[split_index+1:]
    print (ttl)
    int_ttl = int(ttl)
    prefix_index = name.find('/')
    print(prefix_index)
    prefix = name[0:prefix_index]
    print(prefix)
    print('Bucket: {}'.format(object_finalize_event['bucket']))
    print(f"Processing full object_finalize_event: {object_finalize_event}")
    bucket = object_finalize_event['bucket']
    bucket_prefix = 'gs://'+bucket+'/'+prefix
    print(bucket)
    print ('data:','invoking REST call')
    dict_to_send = {'datasetName': prefix, 'dataStorageName': bucket_prefix, 'projectId':project, 'retentionPeriod': int_ttl, 'type': 'DATASET'}
  
    # vpc format http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/
    print(" test 1007 ")
    # below is the format needed for our VPC cloud endpoints setup, note the port 80
    #response = requests.get('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/myresource/')
    #http://localhost:8080/retentionrules/getByBusinessKey?project=sdrs-server&bucket=gs:%2F%2Fds-dev-rpo%2FdataSetY&dataSet=dataSetY
    
    encoded = urllib.parse.quote_plus(bucket_prefix)
    print ('encoded: ',encoded)
   
    url = 'http://104.198.4.155:8080/retentionrules/getByBusinessKey?project={}&bucket={}&dataSet={}'.format(project, encoded, prefix)
    print ('url: ', url)
    get_response = requests.get(url)
    print ('response from server:', get_response)
    print ('response code: ', get_response.status_code)
    
    if get_response.status_code == requests.codes.ok:
    	print ('200 response, so exists, do an update')
    	dict_from_server = get_response.json()
    	print ('bag of data', dict_from_server)
    	rule_id = dict_from_server.get('ruleId')
    	print ('the numeric ruleId is: ', rule_id)
    	put_url = 'http://104.198.4.155:8080/retentionrules/{}'.format(rule_id)
    	dict_4_put = {'retentionPeriod': int_ttl}
    	put_response = requests.put(put_url, json=dict_4_put)
    	print ('Response from server: {}'.format(put_response.text))
    else:
    	print('non 200 response, does not exist, do a create')
    	post_response = requests.post('http://104.198.4.155:8080/retentionrules/', json=dict_to_send)
    	print ('Response from server: {}'.format(post_response.text))
   
    