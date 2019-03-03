
# fetch basic image
FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-alpine-slim
MAINTAINER Zoltan Farkas <zolyfarkas@yahoo.com>


# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           /usr/share/jaxrs-spf4j-demo/lib
# Add the service itself
ARG JAR_FILE
ENV JAR_FILE ${JAR_FILE}
ADD target/${JAR_FILE} /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

ENTRYPOINT /opt/java/openjdk/bin/java -jar /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

# local application port
EXPOSE 8080