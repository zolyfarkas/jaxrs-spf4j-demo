
# fetch basic image
#For jdk use: FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.4_11-slim
FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.8_10
MAINTAINER Zoltan Farkas <zolyfarkas@yahoo.com>


# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           /usr/share/jaxrs-spf4j-demo/lib
# Add the service itself
ARG JAR_FILE
ENV JAR_FILE ${JAR_FILE}
ADD target/${JAR_FILE} /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

ENTRYPOINT /opt/java/openjdk/bin/java -XX:ErrorFile=/var/log/hs_err_pid${KUBE_POD_NAME}%p.log '-XX:OnOutOfMemoryError=kill -9 %p'  -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:HeapDumpPath=/var/log/${KUBE_POD_NAME}.hprof -XX:+UseStringDeduplication -Xms32m -Xmx64m --add-opens=java.logging/java.util.logging=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path=/usr/share/jaxrs-spf4j-demo/lib/compiler-20.1.0.jar:/usr/share/jaxrs-spf4j-demo/lib/compiler-management-20.1.0.jar:/usr/share/jaxrs-spf4j-demo/lib/truffle-api-20.1.0.jar:/usr/share/jaxrs-spf4j-demo/lib/graal-sdk-20.1.0.jar -jar /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

# local application port and actuator port.
EXPOSE 8080 9090