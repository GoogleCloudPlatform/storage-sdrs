# Copyright 2019 Google LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
# Any software provided by Google hereunder is distributed "AS IS", WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.

#!/usr/bin/env bash

PARAMS=""
while (( "$#" )); do
  case "$1" in
    -f|--function)
      FUNCTION=$2
      shift 2
      ;;
    -d|--deployment-name)
      DEPLOYMENT_NAME=$2
      shift 2
      ;;
    --) # end argument parsing
      shift
      break
      ;;
    -*|--*=) # unsupported flags
      echo "Error: Unsupported flag $1" >&2
      exit 1
      ;;
    *) # preserve positional arguments
      PARAMS="$PARAMS $1"
      shift
      ;;
  esac
done
# set positional arguments in their proper place
eval set -- "$PARAMS"

if [ $FUNCTION == "create" ]
then
  cp -R ./common_lib ./gcs_create/.
  echo "Deploying create code to" $DEPLOYMENT_NAME"-gcs-create"
  gcloud beta functions deploy $DEPLOYMENT_NAME-gcs-create --source=./gcs_create/
elif [ $FUNCTION == "delete" ]
then
  cp -R ./common_lib ./gcs_delete/.
  echo "Deploying delete code to" $DEPLOYMENT_NAME"-gcs-delete"
  gcloud beta functions deploy $DEPLOYMENT_NAME-gcs-delete --source=./gcs_delete/
elif [ $FUNCTION == "scheduler" ]
then
  cp -R ./common_lib ./scheduler/.
  echo "Deploying scheduler code to" $DEPLOYMENT_NAME"-scheduler"
  gcloud beta functions deploy $DEPLOYMENT_NAME-scheduler --source=./scheduler/
else
  echo "-f param must be either create, delete or scheduler"
fi
