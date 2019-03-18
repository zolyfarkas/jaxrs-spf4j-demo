package org.spf4j.actuator.logs;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.StreamingOutput;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.AvroDataFileAppender;
import org.spf4j.log.LogPrinter;
import org.spf4j.log.LogbackUtils;
import org.spf4j.zel.vm.CompileException;
import org.spf4j.zel.vm.Program;

/**
 *
 * @author Zoltan Farkas
 */
@Path("logs")
public class LogsResource {

  private static final Comparator<LogRecord> L_COMP = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o2.getTs().compareTo(o1.getTs());
    }
  };

  private static final Comparator<LogRecord> N_COMP = new Comparator<LogRecord>() {
    @Override
    public int compare(final LogRecord o1, final LogRecord o2) {
      return o1.getTs().compareTo(o2.getTs());
    }
  };

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  @Inject
  public LogsResource(final Cluster cluster, final Spf4JClient httpClient) {
    this.cluster = cluster;
    this.httpClient = httpClient;
  }

  @Path("local")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public List<LogRecord> getLocalLogs(@QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter) throws IOException {
    return getLocalLogs(tailOffset, limit, filter, "default");
  }

  @Path("local/{appenderName}")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public List<LogRecord> getLocalLogs(@QueryParam("tailOffset") @DefaultValue("0") final long tailOffset,
          @QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @PathParam("appenderName") final String appenderName) throws IOException {
    Map<String, AvroDataFileAppender> appenders = LogbackUtils.getConfiguredFileAppenders();
    AvroDataFileAppender fa = appenders.get(appenderName);
    if (fa == null) {
      throw new NotFoundException("Resource not available: " + appenderName);
    }
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    String hostName = Sets.intersection(clusterInfo.getLocalAddresses(), clusterInfo.getAddresses())
            .iterator().next().getHostName();
    List<LogRecord> result;
    if (filter != null) {
      try {
        result = fa.getFilteredLogs(hostName, tailOffset, limit, Program.compilePredicate(filter, "log"));
      } catch (CompileException ex) {
        throw new ClientErrorException("Invalid filter " + filter + ", " + ex.getMessage(), 400, ex);
      }
    } else {
      result = fa.getLogs(hostName, tailOffset, limit);
    }
    Collections.reverse(result);
    return result;
  }

  @Path("cluster")
  @GET
  @Produces(value = {"text/plain"})
  public void getClusterLogsText(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter, @Suspended final AsyncResponse ar)
          throws IOException {
    getClusterLogs(limit, filter, new AsyncResponseWrapper(ar) {
      @Override
      public boolean resume(Object response) {
        return super.resume(new StreamingOutput() {
          @Override
          public void write(final OutputStream output) throws IOException, WebApplicationException {
            LogPrinter printer = new LogPrinter(StandardCharsets.UTF_8);
            for (LogRecord record : (Iterable<LogRecord>) response) {
              printer.print(record, output);
            }
          }
        });
      }

    });
  }

  @Path("cluster")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public void getClusterLogs(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter, @Suspended final AsyncResponse ar)
          throws IOException {
    getClusterLogs(limit, filter, "default", ar);
  }

  /**
   * @Path("cluster/{appenderName}")
   * @GET
   * @Produces(value = {"application/avro-x+json", "application/json", "application/avro+json", "application/avro",
   * "application/octet-stream"}) public List<LogRecord> getClusterLogs(@QueryParam("limit") @DefaultValue("1000")final
   * int limit,
   * @QueryParam("filter") @Nullable final String filter,
   * @PathParam("appenderName") final String appender) throws IOException { ClusterInfo clusterInfo =
   * cluster.getClusterInfo(); Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses(); List<LogRecord> result =
   * new ArrayList(limit * peerAddresses.size()); result.addAll(getLocalLogs(0, limit, filter, appender));
   * NetworkService service = getNetworkService(clusterInfo); for (InetAddress addr : peerAddresses) { String url =
   * service.getName() + "://" + addr.getHostAddress() + ':' + service.getPort() + "/logs/local"; Spf4jWebTarget
   * invTarget = httpClient.target(url) .path(appender) .queryParam("limit", limit); if (filter != null) { invTarget =
   * invTarget.queryParam("filter", filter); } result.addAll(invTarget .request("application/avro").get(new
   * GenericType<List<LogRecord>>() {})); } Collections.sort(result, L_COMP); int size = result.size(); return
   * result.subList(Math.max(0, size - limit), size); }
   */
  @Path("cluster/{appenderName}")
  @GET
  @Produces(value = {"application/avro-x+json", "application/json",
    "application/avro+json", "application/avro", "application/octet-stream"})
  public void getClusterLogs(@QueryParam("limit") @DefaultValue("1000") final int limit,
          @QueryParam("filter") @Nullable final String filter,
          @PathParam("appenderName") final String appender, @Suspended final AsyncResponse ar) throws IOException {
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    CompletableFuture<PriorityQueue<LogRecord>> cf
            = CompletableFuture.supplyAsync(() -> {
              PriorityQueue<LogRecord> result = new PriorityQueue(limit, N_COMP);
              Collection<LogRecord> ll;
              try {
                ll = getLocalLogs(0, limit, filter, appender);
              } catch (IOException ex) {
                throw new UncheckedIOException(ex);
              }
              addAll(limit, result, ll);
              return result;
            }, DefaultExecutor.INSTANCE);

    NetworkService service = getNetworkService(clusterInfo);
    for (InetAddress addr : peerAddresses) {
      String url = service.getName() + "://" + addr.getHostAddress() + ':' + service.getPort() + "/logs/local";
      Spf4jWebTarget invTarget = httpClient.target(url)
              .path(appender)
              .queryParam("limit", limit);
      if (filter != null) {
        invTarget = invTarget.queryParam("filter", filter);
      }
      cf = cf.thenCombine(
              invTarget.request("application/avro").rx().get(new GenericType<List<LogRecord>>() {
              }),
              (PriorityQueue<LogRecord> result, List<LogRecord> rl) -> {
                addAll(limit, result, rl);
                return result;
              }
      );
    }
    cf.whenComplete((records, t) -> {
      if (t != null) {
        ar.resume(t);
      } else {
        ArrayList<LogRecord> result = new ArrayList(limit);
        result.addAll(records);
        Collections.sort(result, L_COMP);
        ar.resume(result);
      }
    });
  }

  private static final void addAll(final int limit,
          PriorityQueue<LogRecord> result,
          Collection<LogRecord> records) {
    synchronized (result) {
      result.addAll(records);
      int removeCnt = result.size() - limit;
      for (int i = 0; i < removeCnt; i++) {
        result.remove();
      }
    }
  }

  private static NetworkService getNetworkService(ClusterInfo clusterInfo) {
    NetworkService service = clusterInfo.getService("http");
    if (service == null) {
      service = clusterInfo.getService("https");
    }
    return service;
  }

}
