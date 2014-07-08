/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.remote.rpc.messages.RoutingTableData;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RpcRegistry extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcRegistry.class);
  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private ClusterHandler clusterHandler;
  final ScheduledFuture<?> syncScheduler;
  private RpcRegistry(RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable,
                      ClusterHandler clusterHandler){
    this.routingTable = routingTable;
    this.clusterHandler = clusterHandler;
    this.syncScheduler = scheduler.scheduleAtFixedRate(new SendRoutingTable(), 10, 10, TimeUnit.SECONDS);
  }

  public static Props props(final RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable,
                            final ClusterHandler clusterHandler){
    return Props.create(new Creator<RpcRegistry>(){

      @Override
      public RpcRegistry create() throws Exception {
        return new RpcRegistry(routingTable, clusterHandler);
      }
    });
  }

  @Override
  protected void handleReceive(Object message) throws Exception {
    LOG.debug("Received message {}", message);
    if(message instanceof RoutingTableData) {
      syncRoutingTable((RoutingTableData) message);
    }

  }

  private void syncRoutingTable(RoutingTableData routingTableData) {
    Map<RpcRouter.RouteIdentifier<?, ?, ?>, String> newRpcMap = routingTableData.getRpcMap();
    Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = newRpcMap.keySet();
    for(RpcRouter.RouteIdentifier<?, ?, ?> routeId : routeIds) {
      routingTable.addGlobalRoute(routeId, newRpcMap.get(routeId));
    }
    Map<RpcRouter.RouteIdentifier<?, ?, ?>, LinkedHashSet<String>> newRoutedRpcMap =
        routingTableData.getRoutedRpcMap();
    routeIds = newRoutedRpcMap.keySet();
    for(RpcRouter.RouteIdentifier<?, ?, ?> routeId : routeIds) {
      Set<String> routeAddresses = newRoutedRpcMap.get(routeId);
      for(String routeAddress : routeAddresses) {
        routingTable.addRoutedRpc(routeId, routeAddress);
      }
    }
  }

  private class SendRoutingTable implements Runnable {
    @Override
    public void run() {
      RoutingTableData routingTableData =
          new RoutingTableData(routingTable.getGlobalRpcMap(), routingTable.getRoutedRpcMap());
      LOG.debug("Sending routing table for sync {}", routingTableData);
      ActorSelection actor = clusterHandler.getRandomRegistryActor();
      if(actor != null) {
        actor.tell(routingTableData, self());
      }
    }
  }
}
