# jaxrs-spf4j-demo
A demo project for  a JAX-RS REST service showcasing spf4j functionality.

This is right now work in progress, need to be brave to try it out :-)

The truth is: "We replaced our monolith with micro services so that every outage could be more like a murder mystery"

In our monolith we have some advantages:

1) A strack trace gives you a good/complete picture of what went wrong.

2) Profiling the monolith gives also a complete picture.

3) Refactoring is a lot easier,


...


This example shows how you can overcome some of the challenges you will face in a distributed architecture:

1) Distributed stack traces between REST services.

2) DEBUG on ERROR. (Debug logs will be made available on service error)

3) PROFILE detail on ERROR/WARN

4) Timeout Propagation.

5) Execution context available everywhere in the JAX_RS context. (Timeout, other baggage...)

6) JAX-RS Rest client with retry and hedged execution support.

7) Continuous profiling, with context enrichment.

8) On demand profiling and tracing.

9) Binary (for efficiency)/ Json (for humans) support everywhere.
  Hitting the network has a cost! (and your cloud bill will reflect that)
  your code needs to be aware of the network boundary.

10) Deprecation support, clients will be notified when hitting deprecated endpoints via HTTP Warning headers.




