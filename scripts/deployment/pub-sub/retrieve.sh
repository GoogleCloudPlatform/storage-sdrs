#!/bin/bash

gcloud pubsub subscriptions pull --auto-ack $1
