#!/bin/sh
# quick push and rolling restart.
docker push zolyfarkas/jaxrs-spf4j-demo:0.7-SNAPSHOT
kubectl patch  deployment jaxrs-spf4j-demo -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}"
