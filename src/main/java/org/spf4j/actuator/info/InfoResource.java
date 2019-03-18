
package org.spf4j.actuator.info;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.glassfish.hk2.api.Immediate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.log.ExecContextLogger;

/**
 *
 * @author Zoltan Farkas
 */
@Path("info")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@Immediate
public class InfoResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(InfoResource.class));

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final String hostName;

  @Inject
  public InfoResource(final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    this.hostName = Sets.intersection(clusterInfo.getLocalAddresses(), clusterInfo.getAddresses())
                    .iterator().next().getHostName();
  }

  @GET
  public ApplicationInfo getApplicationInfo() {
    return org.spf4j.base.Runtime.getApplicationInfo();
  }

  @Path("local")
  @GET
  public ProcessInfo getProcessInfo() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    return getProcessInfo(clusterInfo);
  }

  private ProcessInfo getProcessInfo(final ClusterInfo clusterInfo) {
    return ProcessInfo.newBuilder()
            .setAppVersion(org.spf4j.base.Runtime.getAppVersionString())
            .setHostName(hostName)
            .setInstanceId(org.spf4j.base.Runtime.PROCESS_ID)
            .setName(org.spf4j.base.Runtime.PROCESS_NAME)
            .setJreVersion(org.spf4j.base.Runtime.JAVA_VERSION)
            .setNetworkServices(new ArrayList<>(clusterInfo.getServices()))
            .setProcessId(org.spf4j.base.Runtime.PID)
            .build();
  }

  @Path("cluster")
  @GET
  public void getClusterInfo(@Suspended final AsyncResponse ar) {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<ProcessInfo> result = Collections.synchronizedList(new ArrayList(peerAddresses.size() + 1));
    result.add(getProcessInfo(clusterInfo));
    CompletableFuture<List<ProcessInfo>> cf = CompletableFuture.completedFuture(result);
    NetworkService service = getNetworkService(clusterInfo);
    for (InetAddress addr : peerAddresses) {
      String url = service.getName() + "://" + addr.getHostAddress() + ':' + service.getPort() + "/info/local";
      cf = cf.thenCombine(httpClient.target(url)
              .request("application/avro").rx().get(ProcessInfo.class),
              (res, info) -> {res.add(info); return res;});
    }
    cf.whenComplete((res, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(new org.spf4j.base.avro.ClusterInfo(getApplicationInfo(), res));
      }
    });
  }

  public NetworkService getNetworkService(ClusterInfo clusterInfo) {
    NetworkService service = clusterInfo.getService("http");
    if (service == null) {
      service = clusterInfo.getService("https");
    }
    return service;
  }


}
