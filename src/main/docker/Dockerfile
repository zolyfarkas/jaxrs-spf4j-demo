
# fetch basic image
#For jdk use: FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.4_11-slim
FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.5_10
MAINTAINER Zoltan Farkas <zolyfarkas@yahoo.com>


# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           /usr/share/jaxrs-spf4j-demo/lib
# Add the service itself
ARG JAR_FILE
ENV JAR_FILE ${JAR_FILE}
ADD target/${JAR_FILE} /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

ENTRYPOINT /opt/java/openjdk/bin/java -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:HeapDumpPath=/var/log -Xmx68m -jar /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

# local application port
EXPOSE 8080