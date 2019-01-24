#! /bin/bash
# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,cd
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# [START script]
set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/lsproject/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
HOSTNAME=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/hostname" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}  Bucket: ${BUCKET} Hostname: ${HOSTNAME}"

# get our file(s)
gsutil cp "gs://${BUCKET}/gce/"** .

# Install dependencies from apt
apt-get update
apt-get install -yq openjdk-8-jdk

# Make Java8 the default
update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java




cd /


# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog

# Reload daemon to pick up new service
systemctl daemon-reload

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &


#start server via embedded FAT jar
java -jar restEngine-jar-with-dependencies.jar ${HOSTNAME}
echo "Startup Complete"
# [END script]

