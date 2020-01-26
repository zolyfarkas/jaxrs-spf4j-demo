package org.spf4j.demo;

import java.io.IOException;
import java.security.Principal;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.actuator.cluster.ClusterActuatorFeature;
import org.spf4j.base.Env;
import org.spf4j.grizzly.JerseyService;
import org.spf4j.grizzly.JerseyServiceBuilder;
import org.spf4j.grizzly.JvmServices;
import org.spf4j.grizzly.JvmServicesBuilder;
import org.spf4j.grizzly.SingleNodeClusterFeature;
import org.spf4j.jaxrs.AvroSqlFeatures;
import org.spf4j.security.AbacAuthorizer;
import org.spf4j.kube.cluster.KubernetesClusterFeature;

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
    org.spf4j.base.Runtime.getMainClass(); //cacge the main class.
    int appPort = Env.getValue("APP_SERVICE_PORT", 8080);
    JvmServices jvm = new JvmServicesBuilder()
            .withHostName(Env.getValue("KUBE_POD_NAME", "127.0.0.1"))
            .build().start().closeOnShutdown();
    startServices(jvm, appPort);
    Logger.getLogger(Main.class.getName()).log(Level.INFO, "Server started and listening at {0}", appPort);
  }

  public static JerseyService startServices(final JvmServices jvm, final int appPort) throws IOException {
    JerseyService svc = new JerseyServiceBuilder(jvm)
            .withMavenRepoURL("https://dl.bintray.com/zolyfarkas/core")
            .withFeature(ClusterActuatorFeature.class)
            .withFeature(AvroSqlFeatures.class)
            .withFeature(System.getenv("KUBE_NAME_SPACE") == null
                    ? SingleNodeClusterFeature.class : KubernetesClusterFeature.class)
            .withProviderPackage("org.spf4j.demo.resources")
            .withBinder(new AbstractBinder() {
              @Override
              protected void configure() {
                bind(AbacAuthorizer.ALL_ACCESS).to(AbacAuthorizer.class);
              }
            })
            .withPort(appPort)
            .build();
    svc.start();
    return svc;
  }

}
