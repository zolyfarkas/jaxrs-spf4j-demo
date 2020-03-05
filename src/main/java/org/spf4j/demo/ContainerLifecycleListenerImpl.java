/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.demo;

import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

/**
 *
 * @author Zoltan Farkas
 */
public class ContainerLifecycleListenerImpl implements ContainerLifecycleListener {

  @Override
  public void onStartup(Container container) {
    container.getApplicationHandler().getInjectionManager().getInstance(MetricsQueryRegister.class);
  }

  @Override
  public void onReload(Container container) {
  }

  @Override
  public void onShutdown(Container container) {
  }

}
