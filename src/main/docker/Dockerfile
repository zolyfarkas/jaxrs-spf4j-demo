
# fetch basic image
#For jdk use: FROM adoptopenjdk/openjdk11:x86_64-alpine-jdk-11.0.4_11-slim
FROM adoptopenjdk/openjdk11:x86_64-alpine-jre-11.0.11_9
MAINTAINER Zoltan Farkas <zolyfarkas@yahoo.com>


# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD @project.build.directory@/graal           /usr/share/jaxrs-spf4j-demo/graal
ADD @project.build.directory@/lib           /usr/share/jaxrs-spf4j-demo/lib
ADD @project.build.directory@/@project.build.finalName@.jar /usr/share/jaxrs-spf4j-demo/@project.build.finalName@.jar

ENV JAR_FILE @project.build.finalName@.jar
env GRAAL_VERSION @project.build.finalName@

# Create a group and user to runn app as
# will ned to re-enable this once, I choose a kubernetes solution: https://github.com/kubernetes/kubernetes/issues/2630
#RUN addgroup -S demogrp && adduser -S demo -G demogrp
#RUN chown demo:demogrp /var/log && chmod ug+rw /var/log
#USER demo

ENTRYPOINT /opt/java/openjdk/bin/java -XX:ErrorFile=/var/log/hs_err_pid${KUBE_POD_NAME}%p.log '-XX:OnOutOfMemoryError=kill -9 %p'  -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:HeapDumpPath=/var/log/${KUBE_POD_NAME}.hprof -XX:+UseStringDeduplication -Xms32m -Xmx64m --add-opens=java.logging/java.util.logging=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI --upgrade-module-path=/usr/share/jaxrs-spf4j-demo/graal/compiler-${GRAAL_VERSION}.jar:/usr/share/jaxrs-spf4j-demo/graal/compiler-management-${GRAAL_VERSION}.jar:/usr/share/jaxrs-spf4j-demo/graal/truffle-api-${GRAAL_VERSION}.jar:/usr/share/jaxrs-spf4j-demo/graal/graal-sdk-${GRAAL_VERSION}.jar -jar /usr/share/jaxrs-spf4j-demo/${JAR_FILE}

# local application port and actuator port.
EXPOSE 8080 9090