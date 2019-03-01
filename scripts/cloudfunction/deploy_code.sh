#!/usr/bin/env bash

PARAMS=""
while (( "$#" )); do
  case "$1" in
    -f|--function)
      FARG=$2
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

if [ $FARG == "create" ]
then
  cp -R ./common_lib ./gcs_create/.
  gcloud beta functions deploy mgeneau-cf-gcs-create --source=./gcs_create/
elif [ $FARG == "delete" ]
then
  cp -R ./common_lib ./gcs_delete/.
  gcloud beta functions deploy mgeneau-cf-gcs-delete --source=./gcs_delete/
else
  echo "-f param must be either create or delete"
fi
