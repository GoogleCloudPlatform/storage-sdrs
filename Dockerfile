# Dockerfile for SDRS

FROM centos:7 as sdrs-build

LABEL vendor="Google PSO"
LABEL version=1.0

# Install and configure openJDK 8
RUN yum install -y \
       java-1.8.0-openjdk \
       java-1.8.0-openjdk-devel
ENV JAVA_HOME /etc/alternatives/jre

# Install maven
RUN yum install maven -y

# Copy POM and install dependencies
COPY ./pom.xml ./pom.xml
RUN mvn clean install

# Copy source code and package the project
COPY ./src ./src
# TODO: Tests currently rely on integration with gcp
#       Until the tests properly mock GCP interaction, tests will fail during docker build
RUN mvn package -Dmaven.test.skip=true

FROM centos:7

# Install and configure openJDK 8
RUN yum install -y \
       java-1.8.0-openjdk

# Copy the JAR from the build stage
COPY --from=sdrs-build ./target/storage-sdrs-jar-with-dependencies.jar ./app.jar

# Server runs on port 8080
EXPOSE 8080

# Start server on 0.0.0.0
CMD ["java", "-jar", "./app.jar", "0.0.0.0", "-Xms256m", "-Xmx512m"]
