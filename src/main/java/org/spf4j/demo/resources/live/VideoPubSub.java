package org.spf4j.demo.resources.live;

import com.google.common.collect.Iterables;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.spf4j.base.CloseableIterable;
import org.spf4j.io.Streams;
import org.spf4j.jaxrs.Timeout;

/**
 * @author Zoltan Farkas
 */
@Path("video")
public class VideoPubSub {

  private final FileStore fileStore;

  @Inject
  public VideoPubSub(final FileStore fileStore) {
    this.fileStore = fileStore;
  }

  @GET
  @Produces("application/json")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public CloseableIterable<String> getGroups() throws IOException {
    return fileStore.list("");
  }

  @Path("{group}")
  @GET
  @Produces("application/json")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public CloseableIterable<String> getStreams(@PathParam("group") final String group) throws IOException {
    CloseableIterable<String> list = fileStore.list(group);
    return CloseableIterable.from(Iterables.filter(list, (f) -> f.endsWith(".m3u8")), list);
  }

  /**
   * to publish: ffmpeg -i OperationWetBoss.m4v -c:v h264 -hls_flags independent_segments -hls_list_size 10000  -hls_time 10 http://localhost:8080/video/test/testStream.m3u8
   */
  @Path("{group}/{stream}")
  @PUT
  @Consumes("*/*")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public void put(
          @PathParam("group") final String group,
          @PathParam("stream") final String stream,
          InputStream input) throws IOException, ParseException, PlaylistException {
      fileStore.storeFile(group, stream, input);
  }

  @Path("{group}/{stream}")
  @POST
  @Consumes("*/*")
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public void post(
          @PathParam("group") final String group,
          @PathParam("stream") final String stream,
          InputStream input) throws IOException, ParseException, PlaylistException {
      fileStore.storeFile(group, stream, input);
  }

  @Path("{group}/{stream}")
  @GET
  @Timeout(value = 5, unit = TimeUnit.SECONDS)
  public Response get(
          @PathParam("group") final String group,
          @PathParam("stream") final String stream) {
    Response.ResponseBuilder bldr = Response.ok();
    if (stream.endsWith(".m3u8")) {
      bldr.type("application/vnd.apple.mpegurl");
    }
    return bldr.entity(new FileStream(group, stream, fileStore)).build();
  }

  private static class FileStream implements StreamingOutput {

    private final FileStore fileStore;
    private final String group;
    private final String stream;

    public FileStream(String group, String stream, FileStore fileStore) {
      this.group = group;
      this.stream = stream;
      this.fileStore = fileStore;
    }

    @Override
    public void write(OutputStream os) throws IOException {
      InputStream readFile = fileStore.readFile(group, stream);
      if (readFile == null) {
        throw new NotFoundException("No stream " + group + '/' + stream);
      }
      try (InputStream is = readFile) {
        Streams.copy(is, os);
      }
    }
  }

}
