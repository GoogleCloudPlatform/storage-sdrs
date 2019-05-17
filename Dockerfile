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
RUN mvn package

FROM centos:7

# Install and configure openJDK 8
RUN yum install -y \
       java-1.8.0-openjdk

# Copy the JAR from the build stage
COPY --from=sdrs-build ./target/storage-sdrs-jar-with-dependencies.jar ./app.jar

# Server runs on port 8080
EXPOSE 8080

# CMD ["java", "-jar", "./app.jar", "-Xms256m", "-Xmx512m"]
# Conditional enable of JMX Remote based on environment variable ENABLE_JMX
CMD if [ "$ENABLE_JMX" = true ]; \
    then java -Xms256m -Xmx512m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8086 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -jar ./app.jar; \
    else java -Xms256m -Xmx512m -jar ./app.jar; \
    fi
