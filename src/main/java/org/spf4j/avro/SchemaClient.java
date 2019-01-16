package org.spf4j.avro;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipError;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.glassfish.jersey.client.ClientProperties;
import org.spf4j.base.UncheckedExecutionException;
import org.spf4j.io.Streams;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;

/**
 * @author Zoltan Farkas
 */
public final class SchemaClient implements SchemaResolver {

  private final String schemaArtifactClassifier;

  private final String schemaArtifactExtension;

  private final int failureCacheMillis;

  private final int snapshotCacheMillis;

  private final URI remoteMavenRepo;

  private final Path localMavenRepo;

  private final LoadingCache<String, Schema> memoryCache;

  private final Spf4JClient client;

  public SchemaClient(final URI remoteMavenRepo) {
    this(remoteMavenRepo, Paths.get(org.spf4j.base.Runtime.USER_HOME, ".m2", "repository"),
            "", "jar");
  }

  public SchemaClient(final URI remoteMavenRepo, final Path localMavenRepo,
          final String schemaArtifactClassifier, final String schemaArtifactExtension) {
    this(remoteMavenRepo, localMavenRepo, schemaArtifactClassifier, schemaArtifactExtension,
            new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .register(ExecutionContextClientFilter.class)
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build()));
  }

  public SchemaClient(final URI remoteMavenRepo, final Path localMavenRepo,
          final String schemaArtifactClassifier, final String schemaArtifactExtension,
          final Spf4JClient client) {
    this.schemaArtifactClassifier = schemaArtifactClassifier;
    this.schemaArtifactExtension = schemaArtifactExtension;
    this.failureCacheMillis = 5000;
    this.snapshotCacheMillis = 300000;
    try {
      this.remoteMavenRepo = remoteMavenRepo.getPath().endsWith("/") ? remoteMavenRepo
              : new URI(remoteMavenRepo.getScheme(),
                      remoteMavenRepo.getUserInfo(),
                      remoteMavenRepo.getHost(),
                      remoteMavenRepo.getPort(), remoteMavenRepo.getPath() + '/',
                      remoteMavenRepo.getQuery(), remoteMavenRepo.getFragment());
    } catch (URISyntaxException ex) {
     throw new IllegalArgumentException("Invalid repo url: " +  remoteMavenRepo, ex);
    }
    this.localMavenRepo = localMavenRepo;
    this.client = client;
    this.memoryCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<String, Schema>() {
      @Override
      public Schema load(final String key) throws Exception {
        return loadSchema(key);
      }
    });
  }

  @Override
  public Schema resolveSchema(final String id) {
    try {
      return memoryCache.get(id);
    } catch (ExecutionException | com.google.common.util.concurrent.UncheckedExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else {
        throw new UncheckedExecutionException(cause);
      }
    }
  }

  Schema loadSchema(final String id) throws IOException {
    SchemaRef sr = new SchemaRef(id);
    Path schemaPackage = getSchemaPackage(sr);

    URI zipUri = URI.create("jar:" + schemaPackage.toUri().toURL());
    FileSystem zipFs;
    synchronized (zipUri.toString().intern()) { // newFileSystem fails if already one there...
      try {
        zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap());
      } catch (FileSystemAlreadyExistsException ex) {
        zipFs = FileSystems.getFileSystem(zipUri);
      } catch (ZipError ze) {
        Logger logger = Logger.getLogger(SchemaClient.class.getName());
        logger.log(Level.FINE, "zip error with {0}", zipUri);
        logger.log(Level.FINE, "zip error detail", ze);
        Files.delete(schemaPackage);
        return loadSchema(id);
      }
    }
    for (Path root : zipFs.getRootDirectories()) {
      Path index = root.resolve("schema_index.properties");
      if (Files.exists(index)) {
        Properties prop = new Properties();
        try (BufferedReader indexReader = Files.newBufferedReader(index)) {
          prop.load(indexReader);
        }
        String schemaName = prop.getProperty(sr.getRef());
        if (schemaName == null) {
          throw new IOException("unable to resolve schema: " + id + " missing from index " + index);
        }
        Path schemaPath = root.resolve(schemaName.replace('.', '/') + ".avsc");
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(schemaPath))) {
          return new Schema.Parser().parse(bis);
        }
      }
    }
    throw new IOException("unable to resolve schema: " + id);
  }

  /**
   * Retrieves a schema package locally if needed.
   *
   * sample repo url: https://dl.bintray.com/zolyfarkas/core/org/spf4j/avro/core-schema/0.1/core-schema-0.1.jar
   *
   * @param ref
   * @return
   * @throws IOException
   */
  Path getSchemaPackage(final SchemaRef ref) throws IOException {
    String groupPath = ref.getGroupId().replace('.', '/') + '/';
    String artifactId = ref.getArtifactId();
    String version = ref.getVersion();
    Path folder = localMavenRepo.resolve(groupPath)
            .resolve(artifactId).resolve(version);
    String fileName = artifactId + '-' + version
            + (schemaArtifactClassifier.isEmpty() ? "" : ('-' + schemaArtifactClassifier))
            + '.' + schemaArtifactExtension;
    Path result = folder.resolve(fileName);
    if (Files.isReadable(result) && Files.size(result) > 0) {
      if (!version.contains("SNAPSHOT")) {
        return result;
      }
      FileTime lastModifiedTime = Files.getLastModifiedTime(result);
      if (lastModifiedTime.toMillis() + snapshotCacheMillis > System.currentTimeMillis()) {
        return result;
      }
    } else {
      Path ftsFile = folder.resolve(fileName + ".fts");
      if (Files.exists(ftsFile)) {
        Instant lastModifiedTime = Instant.parse(Files.readAllLines(ftsFile, StandardCharsets.UTF_8).get(0));
        long time = lastModifiedTime.toEpochMilli() + failureCacheMillis - System.currentTimeMillis();
        if (time > 0) {
          throw new NotFoundException("Artifact " + ref + " not available, re-attempt in " + time + " ms");
        }
      }
    }
    URI mUri = remoteMavenRepo.resolve(groupPath).resolve(artifactId + '/').resolve(version + '/')
            .resolve(fileName);
    Files.createDirectories(folder);
    Path tmpDownload = Files.createTempFile(folder, ".schArtf", ".tmp");
    try {
      try (InputStream is = client.target(mUri).request(MediaType.WILDCARD_TYPE).get(InputStream.class);
              BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tmpDownload))) {
        Streams.copy(is, bos);
      }
      Files.move(tmpDownload, result, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException | RuntimeException ex) {
      Logger logger = Logger.getLogger(SchemaClient.class.getName());
      logger.log(Level.FINE, "Cannot download {0}", mUri);
      logger.log(Level.FINE, "Exception detail", ex);
      Files.write(tmpDownload, java.util.Arrays.asList(Instant.now().toString()), StandardCharsets.UTF_8);
      Files.move(tmpDownload, folder.resolve(fileName + ".fts"), StandardCopyOption.ATOMIC_MOVE);
      throw ex;
    }
    return result;
  }

  @Override
  public String getId(Schema schema) {
    return schema.getProp("mvnId");
  }

}
