# jaxrs-spf4j-demo
A demo project for  a JAX-RS REST service showcasing spf4j profiling, logging, etc... functionality in a SOA (aka microserives) environment.

This is right now work in progress, need to be brave to try it out :-)

A recent version you can test on GKE [at](https://demo.spf4j.org/apiBrowser)

Some time ago I stumbled upon the statement: "We replaced our monolith with micro services so that every outage could be more like a murder mystery"
It did make me laugh, mostly because it is part joke, and part reality.

To clarify my view on this, let me define monolith: a single OS process doing a lot of things.
Usually it comes a time when things get too big (applies to a lot of things in CS), and a bit of  divide and conquer is needed to break things down into more manageable pieces.
In this case a single process will result in a group of processes that communicate with each other via some form on IPC.

A reminder of some of the advantages we will give away when breaking down our monolith:

1) A stack trace gives you a good/complete picture of what went wrong.

2) Profiling the monolith presents a complete picture.

3) Refactoring is a lot easier.

4) Method calls within the process boundary are fast! and reliable (no network issues, etc).

...


This example shows how you can overcome some of the challenges you will face in a distributed architecture:

1) Distributed stack traces between REST services. Know what is the root cause right away.

2) DEBUG on ERROR. Debug logs attached to the  service error response, of all the services involved!

3) PROFILE detail on slow requests. (continuous profiling)

4) Timeout Propagation.

5) Execution context available everywhere in the JAX_RS context. (Timeout, other baggage...)

6) JAX-RS Rest client with retry and hedged execution support.

7) Continuous profiling, with context enrichment.

8) On demand profiling and tracing.

9) Binary (for efficiency)/ Json (for humans) support everywhere.
  Hitting the network has a cost! (and your cloud bill will reflect that)
  your code needs to be aware of the network boundary.

10) Deprecation support, clients will be notified when hitting deprecated endpoints via HTTP Warning headers.

11) Actuator endpoints  for logs, health, info, swagger docs, jmx.

12) Serialization compatible DTO schema evolution ([see](https://github.com/zolyfarkas/jaxrs-spf4j-demo-schema)).


See the [wiki](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki) for more detailed descriptions of the concepts implemented here

This demo is built and published to docker-hub, you can run this service by:

```
$ docker pull zolyfarkas/jaxrs-spf4j-demo:0.5
$ docker run -p 8080:8080  zolyfarkas/jaxrs-spf4j-demo:0.5
```

open in your browser: 

  * [Hello](https://demo.spf4j.org/demo/helloResource/hello)
  * [Json avro response](https://demo.spf4j.org/demo/example/records?_Accept=application/json)
  * [Binary avro response](https://demo.spf4j.org/demo/example/records)
  * [Error response](https://demo.spf4j.org/demo/helloResource/aError)
  * [Error Response + profile data](https://demo.spf4j.org/demo/helloResource/slowBrokenHello?time=31)


if adventurous you can try this in kubernetes:

  Install and run a kubernetes local cluster from: https://github.com/kubernetes-sigs/kubeadm-dind-cluster

```
  kubectl create -f ./src/main/kube/kube-rbac.yaml
  
  kubectl create -f ./src/main/kube/kube-deployment.yaml

  kubectl create -f ./src/main/kube/kube-service.yaml

  kubectl port-forward  deployment/jaxrs-spf4j-demo 8080:8080

```
  now you can access the cluster via localhost:8080.

  And try out additionally some cluster endpoints in your browser:

  * [app cluster info](https://demo.spf4j.org/info/cluster?_Accept=application/json)
  * [debug a request](https://demo.spf4j.org/info/cluster?_Accept=application/json&_log-level=DEBUG)
  * [see app logs](https://demo.spf4j.org/logs/cluster)
  * [see app logs as json](https://demo.spf4j.org/logs/cluster?_Accept=application/json)
  * [browse the api](https://demo.spf4j.org/apiBrowser)
