package org.spf4j.demo;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.actuator.cluster.ClusterActuatorFeature;
import org.spf4j.apiBrowser.ApiBrowserFeature;
import org.spf4j.base.Env;
import org.spf4j.demo.resources.live.FileStore;
import org.spf4j.demo.resources.live.FSFileStore;
import org.spf4j.demo.resources.live.ReplicatedFileStoreResource;
import org.spf4j.grizzly.JerseyService;
import org.spf4j.grizzly.JerseyServiceBuilder;
import org.spf4j.grizzly.JvmServices;
import org.spf4j.grizzly.JvmServicesBuilder;
import org.spf4j.grizzly.SingleNodeClusterFeature;
import org.spf4j.jaxrs.AvroSqlFeatures;
import org.spf4j.security.AbacAuthorizer;
import org.spf4j.kube.cluster.KubernetesClusterFeature;
import org.spf4j.log.LogbackService;

/**
 * Main class.
 *
 */
public class Main {

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    System.setProperty("spf4j.failsafe.retryLogLevel", "DEBUG");
    System.setProperty("spf4j.throwables.defaultMaxSuppressChain", "10");
    LogbackService.redirecJDKLogging2Slf4j();
    org.spf4j.base.Runtime.getMainClass(); //cache the main class.
    Schema.MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    int appPort = Env.getValue("APP_SERVICE_PORT", 8080);
    int actuatorPort = Env.getValue("APP_ACTUATOR_PORT", 9090);
    String hostName = Env.getValue("KUBE_POD_NAME", "127.0.0.1");
    String logFolder = Env.getValue("LOG_FOLDER", "/var/log");
    JvmServices jvm = new JvmServicesBuilder()
            .withHostName(hostName)
            .withLogFolder(logFolder)
            .withMetricsStore("WRAPPER@org.spf4j.demo.MetricsQueryRegister$Store(TSDB_AVRO@"
                    + logFolder + '/' + hostName + ")")
            .build().start().closeOnShutdown();
    startServices(jvm, appPort, logFolder);
    startActuator(jvm, actuatorPort);
    Logger.getLogger(Main.class.getName()).log(Level.INFO,
            "Server started and listening at {0,number,######} and actuator at {1,number,######}"
                    + " in {2} ms", new Object[] {
              appPort, actuatorPort,
                      (System.currentTimeMillis() -  ManagementFactory.getRuntimeMXBean().getStartTime())
            });
  }

  public static JerseyService startServices(final JvmServices jvm,
          final int appPort, String logFolder) throws IOException {
    JerseyService svc = new JerseyServiceBuilder(jvm)
            .withMavenRepoURL("https://dl.bintray.com/zolyfarkas/core")
            .withFeature(ClusterActuatorFeature.class)
            .withFeature(ApiBrowserFeature.class)
            .withFeature(AvroSqlFeatures.class)
            .withFeature(System.getenv("KUBE_NAME_SPACE") == null
                    ? SingleNodeClusterFeature.class : KubernetesClusterFeature.class)
            .withProviderPackage("org.spf4j.demo.resources")
            .withServiceProvider(ContainerLifecycleListenerImpl.class)
            .withBinder(new AbstractBinder() {
              @Override
              protected void configure() {
                bind(AbacAuthorizer.ALL_ACCESS).to(AbacAuthorizer.class);
                bindAsContract(MetricsQueryRegister.class);
                Path logPath = Path.of(logFolder);
                Path videoPath = logPath.resolve("videoRepo");
                try {
                  Files.createDirectories(videoPath);
                } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
                }
                bind(new FSFileStore(videoPath, 30, TimeUnit.MINUTES))
                         .named("local")
                        .to(FileStore.class);
                bind(ReplicatedFileStoreResource.class)
                        .named("replicated")
                        .to(FileStore.class);
              }
            })
            .withPort(appPort)
            .build();
    svc.start();
    return svc;
  }

  public static JerseyService startActuator(final JvmServices jvm, final int appPort) throws IOException {
    JerseyService svc = new JerseyServiceBuilder(jvm)
            .withMavenRepoURL("https://dl.bintray.com/zolyfarkas/core")
            .withFeature(ClusterActuatorFeature.class)
            .withFeature(System.getenv("KUBE_NAME_SPACE") == null
                    ? SingleNodeClusterFeature.class : KubernetesClusterFeature.class)
            .withPort(appPort)
            .withKernelThreadsCoreSize(1)
            .withKernelThreadsMaxSize(2)
            .withWorkerThreadsCoreSize(1)
            .withWorkerThreadsMaxSize(4)
            .build();
    svc.start();
    return svc;
  }

}
