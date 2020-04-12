#!/bin/sh
# quick push and rolling restart.
kubectl set image deployments/jaxrs-spf4j-demo jaxrs-spf4j-demo=zolyfarkas/jaxrs-spf4j-demo:${1}
