# jaxrs-spf4j-demo
A demo project for  a JAX-RS REST service showcasing spf4j functionality.

This is right now work in progress, need to be brave to try it out :-)

The truth is: "We replaced our monolith with micro services so that every outage could be more like a murder mystery"

In our monolith we have some advantages:

1) A strack trace gives you a good/complete picture of what went wrong.

2) Profiling the monolith gives also a complete picture.

3) Refactoring is a lot easier.


...


This example shows how you can overcome some of the challenges you will face in a distributed architecture:

1) Distributed stack traces between REST services. Know what is the root cause right away.

2) DEBUG on ERROR. Debug logs attached to the  service error response, of all the services involved!

3) PROFILE detail on ERROR/WARN.

4) Timeout Propagation.

5) Execution context available everywhere in the JAX_RS context. (Timeout, other baggage...)

6) JAX-RS Rest client with retry and hedged execution support.

7) Continuous profiling, with context enrichment.

8) On demand profiling and tracing.

9) Binary (for efficiency)/ Json (for humans) support everywhere.
  Hitting the network has a cost! (and your cloud bill will reflect that)
  your code needs to be aware of the network boundary.

10) Deprecation support, clients will be notified when hitting deprecated endpoints via HTTP Warning headers.


This demo is built and published to docker-hub, you can run this service by:

```
$ docker pull zolyfarkas/jaxrs-spf4j-demo:0.4
$ docker run -p 8080:8080  zolyfarkas/jaxrs-spf4j-demo:0.4
```

open in your browser: 

  * [Hello](http://127.0.0.1:8080/demo/helloResource/hello)
  * [Json avro response](http://127.0.0.1:8080/demo/example/records?_Accept=application/json)
  * [Binary avro response](http://127.0.0.1:8080/demo/example/records)
  * [Error response](http://127.0.0.1:8080/demo/helloResource/aError)
  * [Error Response + profile data](http://127.0.0.1:8080/demo/helloResource/slowBrokenHello?time=31)


if adventurous you can try this in kubernetes:

  Install a kubernetes local cluster from (or use minikube): https://github.com/kubernetes-sigs/kubeadm-dind-cluster

```
  kubectl create -f ./src/main/kube/kube-deployment.yaml

  kubectl create -f ./src/main/kube/kube-service.yaml

  kubectl port-forward  deployment/jaxrs-spf4j-demo 8080:8080

```
  now you can access the clustervvia localhost:8080.