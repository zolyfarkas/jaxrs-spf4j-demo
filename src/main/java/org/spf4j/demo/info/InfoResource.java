
package org.spf4j.demo.info;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.base.avro.ProcessInfo;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 *
 * @author Zoltan Farkas
 */
@Path("info")
@Produces(value = {"application/avro", "application/avro-x+json", "application/octet-stream",
    "application/json"})
public class InfoResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  @Inject
  public InfoResource(final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
  }

  @GET
  public ApplicationInfo getApplicationInfo() {
    return org.spf4j.base.Runtime.getApplicationInfo();
  }

  @Path("node")
  @GET
  public ProcessInfo getProcessInfo() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    return getProcessInfo(clusterInfo);
  }

  private ProcessInfo getProcessInfo(final ClusterInfo clusterInfo) {
    return ProcessInfo.newBuilder()
            .setAppVersion(org.spf4j.base.Runtime.getAppVersionString())
            .setHostName(Sets.intersection(clusterInfo.getLocalAddresses(), clusterInfo.getAddresses())
                    .iterator().next().getHostName())
            .setInstanceId("")
            .setJreVersion(org.spf4j.base.Runtime.JAVA_VERSION)
            .setNetworkServices(new ArrayList<>(clusterInfo.getServices()))
            .setProcessId(org.spf4j.base.Runtime.PID)
            .build();
  }

  @Path("cluster")
  @GET
  public org.spf4j.base.avro.ClusterInfo getClusterInfo() {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<ProcessInfo> result = new ArrayList(peerAddresses.size() + 1);
    result.add(getProcessInfo(clusterInfo));
    NetworkService service = clusterInfo.getService("http");
    if (service == null) {
      service = clusterInfo.getService("https");
    }
    for (InetAddress addr : peerAddresses) {
      result.add(httpClient.target(service.getName() + "://" + addr.toString() + ':' + service.getPort() + "/info/node")
              .request("application/avro").get(ProcessInfo.class));
    }
    return new org.spf4j.base.avro.ClusterInfo(getApplicationInfo(), result);
  }


}
