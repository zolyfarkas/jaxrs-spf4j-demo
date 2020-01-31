# jaxrs-spf4j-demo
A demo project for  a JAX-RS REST service showcasing Avro REST SQL, spf4j profiling, logging, etc... functionality in a SOA (aka micro-services) environment.

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

7) Continuous profiling, with context enrichment [see](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki/ContinuousProfiling) for more detail.

8) On demand profiling and tracing.

9) Binary (for efficiency), Json (for humans) support everywhere, CSV for excel wizards.

10) Deprecation support, clients will be notified when using deprecated endpoints or deprecated objects/attributes via HTTP Warning headers.

11) Actuator endpoints  for logs, health, info, swagger docs, jmx, profiles, metrics. ([see](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki/JaxRsActuator))

12) Serialization compatible DTO schema evolution ([see](https://github.com/zolyfarkas/jaxrs-spf4j-demo-schema), and [see](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki/AvroReferences)).


See the [wiki](https://github.com/zolyfarkas/jaxrs-spf4j-demo/wiki) for more detailed descriptions of the concepts implemented here

This demo is built and published to docker-hub, you can run this service by ([install docker](https://docs.docker.com/install/)):

```
$ docker pull zolyfarkas/jaxrs-spf4j-demo:0.8-SNAPSHOT
$ docker run -p 8080:8080  zolyfarkas/jaxrs-spf4j-demo:0.8-SNAPSHOT
```

open in your browser (use http and port 8080 if you run it locally):

  * [Hello](https://demo.spf4j.org/helloResource/hello)
  * [Json avro response](https://demo.spf4j.org/example/records?_Accept=application/json)
  * [Binary avro response](https://demo.spf4j.org/example/records)
  * [Error response](https://demo.spf4j.org/helloResource/aError)
  * [Error Response + profile data](https://demo.spf4j.org/helloResource/slowBrokenHello?time=10)


if adventurous you can try this in kubernetes:

  Install kind [from](https://kind.sigs.k8s.io)


```
  # install kubectl
  sudo port install kubectl-1.15

  #switch between kubectl versions.
  sudo port select --set kubectl kubectl1.15

  #create local kubernetes cluster
  kind create cluster --config cluster-3n.yml

  #deploy the app
  kubectl create -f ./src/main/kube/kube-rbac.yaml
  
  kubectl create -f ./src/main/kube/kube-deployment.yaml

  kubectl create -f ./src/main/kube/kube-service.yaml

  kubectl port-forward  deployment/jaxrs-spf4j-demo 8080:8080

```
  now you can access the app via localhost:8080.

  And try out additionally some cluster endpoints in your browser (use http and  localhost8080 for the local app):

  * [app cluster info](https://demo.spf4j.org/info/cluster?_Accept=application/json)
  * [debug a request](https://demo.spf4j.org/info/cluster?_Accept=application/json&_log-level=DEBUG)
  * [see app logs](https://demo.spf4j.org/logs/cluster)
  * [see app logs as json](https://demo.spf4j.org/logs/cluster?_Accept=application/json)
  * [browse the api](https://demo.spf4j.org/apiBrowser)
