package org.spf4j.demo.resources.live;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.hk2.api.Immediate;
import org.spf4j.base.CloseableIterable;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.ClusterInfo;
import org.spf4j.io.Streams;

/**
 * @author Zoltan Farkas
 */
@Path("/files")
@Immediate
@Singleton
public class ReplicatedFileStoreResource implements FileStore {
  private final Cluster cluster;

  private final int port;

  private final String protocol;

  private final FileStore localStore;

  @Inject
  public ReplicatedFileStoreResource(final Cluster cluster,
          @ConfigProperty(name = "servlet.port") final int port,
          @ConfigProperty(name = "servlet.protocol") final String protocol,
          @Named("local") final FileStore localStore) {
    this.cluster = cluster;
    this.port = port;
    this.protocol = protocol;
    this.localStore = localStore;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public CloseableIterable<String> list(final String path) throws IOException {
    CloseableIterable<String> list = localStore.list(path);
    if (list == null) {
      throw new NotFoundException("Not found " + path);
    }
    return list;
  }

  @Override
  public InputStream readFile(final String filePath) throws IOException {
    return localStore.readFile(filePath);
  }

  @Path("{path:.*}")
  @POST
  public void storeFileLocal(@PathParam("path") String filePath,
          final InputStream is) throws IOException, TimeoutException {
    try (OutputStream os = localStore.storeFile(filePath)) {
      Streams.copy(is, os);
    }
  }


  @Override
  public OutputStream storeFile(String filePath) throws IOException, TimeoutException {
    BroadcastOutputStream bos = new BroadcastOutputStream(localStore.storeFile(filePath));
    ClusterInfo clusterInfo = cluster.getClusterInfo();
    Set<InetAddress> peerAddresses = clusterInfo.getPeerAddresses();
    for (InetAddress addr : peerAddresses) {
      URI uri;
      try {
        uri = new URI(protocol, null, addr.getHostAddress(), port, "/files/" + filePath, null, null);
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
      final HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(ExecutionContexts.getTimeToDeadlineInt(TimeUnit.MILLISECONDS));
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.connect();
      OutputStream os = conn.getOutputStream();
      bos.addStream(os);
      bos.addCloseable(() -> {
        int responseCode = conn.getResponseCode();
        if (responseCode >= 300 || responseCode < 200) {
          //todo handle response payload for extra debug
          conn.disconnect();
          throw new WebApplicationException(responseCode);
        }
      });

    }
    return bos;
  }






}
