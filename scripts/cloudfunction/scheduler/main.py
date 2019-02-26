import base64
import os
import requests
 
import urllib.parse

import flask

def scheduler_pubsub(pub_sub_event, context):
    """Triggered from a message on a Cloud Pub/Sub topic.
    Args:
         pub_sub_event (dict): Event payload.
         context (google.cloud.functions.Context): Metadata for the pub_sub_event.
    """
    pub_sub_message = base64.b64decode(pub_sub_event['data']).decode('utf-8')
    print(pub_sub_message)
    project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
    print(f"Project : {project}")
   
   	# only need type - target and projectId parameters are optional
    type='POLICY'
    dict_to_send = {'type': 'DATASET'}
  
    print("debug here in 02/26/2019")
    
    if pub_sub_message == 'executor':
    	print ('Invoking the executor')
    	response = requests.post('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/events/execution/', json=dict_to_send)
    	dict_from_server = get_response.json()
    elif pub_sub_message == 'validator':
    	print('Invoking the validator')
		response = requests.post('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/events/validation/')
    else:
    	print('invalid event payload - no work to do')
    	
  
