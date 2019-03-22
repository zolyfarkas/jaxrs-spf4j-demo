/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.actuator.health;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.spf4j.base.Pair;
import org.spf4j.base.avro.HealthCheckInfo;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;

/**
 *
 * @author Zoltan Farkas
 */
public class HealthOrgNode {

  private final Map<String, HealthOrgNode> subnodes;

  private final HealthCheck check;

  private HealthOrgNode(final HealthCheck check) {
    this.subnodes = Collections.EMPTY_MAP;
    this.check = check;
  }

  private HealthOrgNode() {
    this.subnodes = new HashMap<>(4);
    this.check = null;
  }

  public static HealthOrgNode newHealthChecks() {
    return new HealthOrgNode();
  }

  public static HealthOrgNode newHealthCheck(final HealthCheck check) {
    return new HealthOrgNode(check);
  }

  public static HealthOrgNode newHealthChecks(final String name, final HealthCheck check) {
    HealthOrgNode healthOrgNode = new HealthOrgNode(check);
    healthOrgNode.subnodes.put(name, newHealthCheck(check));
    return healthOrgNode;
  }

  public void addHealthCheck(final HealthCheck check, final String name) {
    if (this.check != null) {
      throw new IllegalAccessError("Not a directory " + this);
    }
    HealthOrgNode ex = subnodes.put(name, newHealthCheck(check));
    if (ex != null) {
      throw new IllegalAccessError("Unable to register " + check + " at " + name + ", existing " + ex);
    }
  }

  public void addHealthCheck(final HealthCheck check, String ... path) {
    if (this.check != null) {
      throw new IllegalAccessError("Not a directory " + this);
    }
    if (path.length <= 0) {
      throw new IllegalArgumentException("path must have at least one element" + Arrays.toString(path));
    }
    String first = path[0];
    if (path.length == 1) {
      addHealthCheck(check, first);
    } else {
      HealthOrgNode dirNode= this.subnodes.get(first);
      if (dirNode == null) {
        dirNode = new HealthOrgNode();
        this.subnodes.put(first, dirNode);
      }
      dirNode.addHealthCheck(check, Arrays.copyOfRange(path, 1, path.length));
    }
  }

  @Nullable
  public HealthCheck getHealthCheck(String ... fromPath) {
    if (fromPath.length <= 0) {
      return this.check;
    }
    String first = fromPath[0];
    if (fromPath.length == 1) {
      return subnodes.get(first).check;
    } else {
      HealthOrgNode dirNode= this.subnodes.get(first);
      if (dirNode == null) {
        return null;
      }
      return dirNode.getHealthCheck(Arrays.copyOfRange(fromPath, 1, fromPath.length));
    }
  }

  @Nullable
  public HealthOrgNode getHealthNode(String... fromPath) {
    if (fromPath.length <= 0) {
      return this;
    }
    String first = fromPath[0];
    if (fromPath.length == 1) {
      return subnodes.get(first);
    } else {
      HealthOrgNode dirNode= this.subnodes.get(first);
      if (dirNode == null) {
        return null;
      }
      return dirNode.getHealthNode(Arrays.copyOfRange(fromPath, 1, fromPath.length));
    }
  }



  public HealthCheckInfo getHealthCheckInfo(final String name, final int maxDepth) {
    if (maxDepth < 0) {
      throw new IllegalArgumentException("maxDepth must be at leat " + maxDepth);
    }
    if (this.check != null) {
      return new HealthCheckInfo(name, check.info(), Collections.EMPTY_LIST);
    } else if (maxDepth > 0) {
      List<HealthCheckInfo> infos = new ArrayList<>(subnodes.size());
      for (Map.Entry<String, HealthOrgNode> entry : subnodes.entrySet()) {
        infos.add(entry.getValue().getHealthCheckInfo(entry.getKey(), maxDepth - 1));
      }
      return new HealthCheckInfo(name, "Composite health check", infos);
    } else {
      return new HealthCheckInfo(name, "Composite health check", Collections.EMPTY_LIST);
    }
  }

  public HealthRecord getHealthRecord(final String name, final String origin,  final Logger logger,
            final boolean isDebug, final boolean isDebugOnError) {
    if (this.check != null) {
      logger.debug("Getting health record for {}", name);
      return this.check.getRecord(name, origin, logger, isDebug, isDebugOnError);
    } else {
      logger.debug("Getting health records for {}", name);
      List<HealthRecord> infos = new ArrayList<>(subnodes.size());
      HealthStatus aggStatus  = HealthStatus.HEALTHY;
      for (Map.Entry<String, HealthOrgNode> entry : subnodes.entrySet()) {
        HealthRecord healthRecord = entry.getValue().getHealthRecord(
                entry.getKey(), origin, logger, isDebug, isDebugOnError);
        HealthStatus rStatus = healthRecord.getStatus();
        if (aggStatus.ordinal() < rStatus.ordinal()) {
          aggStatus = rStatus;
        }
        infos.add(healthRecord);
      }
      return new HealthRecord(origin, name, aggStatus, null, infos);
    }
  }



  public void traverse(BiConsumer<String[], HealthCheck> consumer, String ... fromPath) {
    HealthOrgNode node = getHealthNode(fromPath);
    if (node == null) {
      return;
    }
    node.traverse(consumer);
  }

  public void traverse(BiConsumer<String[], HealthCheck> consumer) {
    ArrayDeque<Pair<String[], HealthOrgNode>> traversalQueue = new ArrayDeque<>();
    traversalQueue.addLast(Pair.of(new String[] { "" }, this));
    Pair<String[], HealthOrgNode> t;
    while  ((t = traversalQueue.pollFirst()) != null) {
      HealthOrgNode on = t.getSecond();
      HealthCheck c = on.check;
      String[] path = t.getFirst();
      if (c != null) {
        consumer.accept(path, c);
      } else {
        for (Map.Entry<String, HealthOrgNode> entry : on.subnodes.entrySet()) {
          traversalQueue.add(Pair.of(org.spf4j.base.Arrays.append(path, entry.getKey()), entry.getValue()));
        }
      }
    }
  }

  @Override
  public String toString() {
    return "HealthOrgNode{" +  (check == null ? ("subnodes=" + subnodes) : ("check=" + check)) + '}';
  }
}
