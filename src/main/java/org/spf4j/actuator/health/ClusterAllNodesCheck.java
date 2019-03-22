package org.spf4j.actuator.health;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.concurrent.ContextPropagatingCompletableFuture;
import org.spf4j.jaxrs.ConfigProperty;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
@Service
public class ClusterAllNodesCheck implements HealthCheck {

  private final Cluster cluster;

  private final Spf4JClient httpClient;

  private final long timeoutNanos;

  @Inject
  public ClusterAllNodesCheck(final Cluster cluster,
          final Spf4JClient httpClient,
          @ConfigProperty("spf4j.health.cluster.timeoutMillis")
          @DefaultValue("10000")
          final long timeouMillis) {
    this.cluster = cluster;
    this.httpClient = httpClient;
    this.timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeouMillis);
  }

  @Override
  public void test(Logger logger) throws Exception {
  }

  @Override
  public HealthRecord getRecord(String name, String origin,
          Logger logger, boolean isDebug,
          boolean isDebugOnError) {
    try (ExecutionContext ec = ExecutionContexts.start(name,
            timeout(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS)) {
      ClusterInfo clusterInfo = cluster.getClusterInfo();
      Set<InetAddress> addresses = clusterInfo.getAddresses();

      NetworkService service = clusterInfo.getHttpService();
      CompletableFuture<List<HealthRecord>> cf
              = ContextPropagatingCompletableFuture.completedFuture(Collections.synchronizedList(
                      new ArrayList<>(addresses.size())));

      for (InetAddress addr : addresses) {
        URI uri;
        try {
          uri = new URI(service.getName(), null,
                  addr.getHostAddress(), service.getPort(), "/health/check/local", null, null);
        } catch (URISyntaxException ex) {
           throw new RuntimeException(ex);
        }
        cf = cf.thenCombine(httpClient.target(uri)
                .request("application/avro").rx().get(HealthRecord.class),
                (res, info) -> {
                  res.add(info);
                  return res;
                });
      }
      List<HealthRecord> result;
      try {
        result = cf.get();
        return new HealthRecord(origin, name, HealthStatus.HEALTHY,
                isDebug ? ec.getDebugDetail(origin, null) : null, result);
      } catch (InterruptedException | ExecutionException | RuntimeException ex) {
        return new HealthRecord(origin, name, HealthStatus.HEALTHY,
                isDebugOnError ? ec.getDebugDetail(origin, ex) : null,
                Collections.EMPTY_LIST);
      }
    }
  }

  @Override
  public String info() {
    return "A health checks, that runs and aggregates the health checks going against all cluster members";
  }

  @Override
  public Type getType() {
    return Type.cluster;
  }

  @Override
  public long timeout(final TimeUnit tu) {
    return tu.convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

}
