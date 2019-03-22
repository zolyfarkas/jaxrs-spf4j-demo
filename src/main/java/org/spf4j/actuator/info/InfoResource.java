
package org.spf4j.actuator.info;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 *
 * @author Zoltan Farkas
 */
@Path("info")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
@Immediate
public class InfoResource {

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
  public void getClusterInfo(@Suspended final AsyncResponse ar) throws URISyntaxException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<ProcessInfo> result = Collections.synchronizedList(new ArrayList(peerAddresses.size() + 1));
    result.add(getProcessInfo(clusterInfo));
    CompletableFuture<List<ProcessInfo>> cf = ContextPropagatingCompletableFuture.completedFuture(result);
    NetworkService service = clusterInfo.getHttpService();
    for (InetAddress addr : peerAddresses) {
      URI uri = new URI(service.getName(), null,
                  addr.getHostAddress(), service.getPort(), "/info/local", null, null);
      cf = cf.thenCombine(httpClient.target(uri)
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


}
