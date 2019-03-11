
package org.spf4j.actuator.logs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericType;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.AvroDataFileAppender;
import org.spf4j.log.LogbackUtils;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logs")
@Produces(value = {"application/avro", "application/avro-x+json", "application/octet-stream",
    "application/json"})
public class LogsResource {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  @Inject
  public LogsResource(final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
  }

  @Path("local")
  @GET
  public List<LogRecord> getLocalLogs(@QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("1000") final int limit) throws IOException {
    return getLocalLogs(tailOffset, limit, "default");
  }

  @Path("local/{appenderName}")
  @GET
  public List<LogRecord> getLocalLogs(@QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("1000") final int limit,
          @PathParam("appenderName") final String appenderName) throws IOException {
    Map<String, AvroDataFileAppender> appenders = LogbackUtils.getConfiguredFileAppenders();
    AvroDataFileAppender fa = appenders.get(appenderName);
    if (fa == null) {
      throw new NotFoundException("Resource not available: " + appenderName);
    }
    return fa.getLogs(tailOffset, limit);
  }

  @Path("cluster")
  @GET
  public List<LogRecord> getClusterLogs(@QueryParam("limit") @DefaultValue("1000")final int limit) throws IOException {
    return getClusterLogs(limit, "default");
  }


  @Path("cluster/{appenderName}")
  @GET
  public List<LogRecord> getClusterLogs(@QueryParam("limit") @DefaultValue("1000")final int limit,
          @PathParam("appenderName") final String appender) throws IOException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    List<LogRecord> result = new ArrayList(limit * peerAddresses.size());
    result.addAll(getLocalLogs(0, limit, appender));
    NetworkService service = clusterInfo.getService("http");
    if (service == null) {
      service = clusterInfo.getService("https");
    }
    for (InetAddress addr : peerAddresses) {
      String url = service.getName() + "://" + addr.getHostAddress() + ':' + service.getPort() + "/logs/local";
      Spf4jWebTarget invTarget = httpClient.target(url)
              .path(appender)
              .queryParam("limit", limit);
      result.addAll(invTarget
              .request("application/avro").get(new GenericType<List<LogRecord>>() {}));
    }
    Collections.sort(result, new Comparator<LogRecord>() {
      @Override
      public int compare(final LogRecord o1, final LogRecord o2) {
        return o1.compareTo(o2);
      }
    });
    int size = result.size();
    return result.subList(size - limit, size);
  }


}
