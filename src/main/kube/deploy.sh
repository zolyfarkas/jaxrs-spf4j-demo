#!/bin/sh
# rolling restart.
kubectl patch  deployment jaxrs-spf4j-demo -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"date\":\"`date +'%s'`\"}}}}}"
