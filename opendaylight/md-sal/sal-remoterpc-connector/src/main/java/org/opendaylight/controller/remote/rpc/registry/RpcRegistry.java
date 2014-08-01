/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.Props;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.japi.Creator;
import org.opendaylight.controller.remote.rpc.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.ActorConstants;
import org.opendaylight.controller.remote.rpc.messages.AddRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.AddRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpcReply;
import org.opendaylight.controller.remote.rpc.messages.GetRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRpcReply;
import org.opendaylight.controller.remote.rpc.messages.RemoveRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.RemoveRpc;
import org.opendaylight.controller.remote.rpc.messages.RoutingTableData;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This Actor maintains the routing table state and sync it with other nodes in the cluster.
 *
 * A scheduler runs after an interval of time, which pick a random member from the cluster
 * and send the current state of routing table to the member.
 *
 * when a message of routing table data is received, it gets merged with the local routing table
 * to keep the latest data.
 */

public class RpcRegistry extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcRegistry.class);
  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final ClusterWrapper clusterWrapper;
  private final ScheduledFuture<?> syncScheduler;

  private RpcRegistry(ClusterWrapper clusterWrapper){
    this.routingTable = new RoutingTable<>();
    this.clusterWrapper = clusterWrapper;
    this.syncScheduler = scheduler.scheduleAtFixedRate(new SendRoutingTable(), 10, 10, TimeUnit.SECONDS);
  }

  public static Props props(final ClusterWrapper clusterWrapper){
    return Props.create(new Creator<RpcRegistry>(){

      @Override
      public RpcRegistry create() throws Exception {
        return new RpcRegistry(clusterWrapper);
      }
    });
  }

  @Override
  protected void handleReceive(Object message) throws Exception {
    LOG.debug("Received message {}", message);
    if(message instanceof RoutingTableData) {
      syncRoutingTable((RoutingTableData) message);
    } else if(message instanceof GetRoutedRpc) {
      getRoutedRpc((GetRoutedRpc) message);
    } else if(message instanceof GetRpc) {
      getRpc((GetRpc) message);
    } else if(message instanceof AddRpc) {
      addRpc((AddRpc) message);
    } else if(message instanceof RemoveRpc) {
      removeRpc((RemoveRpc) message);
    } else if(message instanceof AddRoutedRpc) {
      addRoutedRpc((AddRoutedRpc) message);
    } else if(message instanceof RemoveRoutedRpc) {
      removeRoutedRpc((RemoveRoutedRpc) message);
    }
  }

  private void getRoutedRpc(GetRoutedRpc rpcMsg){
    LOG.debug("Get latest routed Rpc location from routing table {}", rpcMsg);
    String remoteActorPath = routingTable.getLastAddedRoutedRpc(rpcMsg.getRouteId());
    GetRoutedRpcReply routedRpcReply = new GetRoutedRpcReply(remoteActorPath);

    getSender().tell(routedRpcReply, self());
  }

  private void getRpc(GetRpc rpcMsg) {
    LOG.debug("Get global Rpc location from routing table {}", rpcMsg);
    String remoteActorPath = routingTable.getGlobalRoute(rpcMsg.getRouteId());
    GetRpcReply rpcReply = new GetRpcReply(remoteActorPath);

    getSender().tell(rpcReply, self());
  }

  private void addRpc(AddRpc rpcMsg) {
    LOG.debug("Add Rpc to routing table {}", rpcMsg);
    routingTable.addGlobalRoute(rpcMsg.getRouteId(), rpcMsg.getActorPath());

    getSender().tell("Success", self());
  }

  private void removeRpc(RemoveRpc rpcMsg) {
    LOG.debug("Removing Rpc to routing table {}", rpcMsg);
    routingTable.removeGlobalRoute(rpcMsg.getRouteId());

    getSender().tell("Success", self());
  }

  private void addRoutedRpc(AddRoutedRpc rpcMsg) {
    routingTable.addRoutedRpcs(rpcMsg.getAnnouncements(), rpcMsg.getActorPath());
    getSender().tell("Success", self());
  }

  private void removeRoutedRpc(RemoveRoutedRpc rpcMsg) {
    routingTable.removeRoutes(rpcMsg.getAnnouncements(), rpcMsg.getActorPath());
    getSender().tell("Success", self());
  }

  private void syncRoutingTable(RoutingTableData routingTableData) {
    LOG.debug("Syncing routing table {}", routingTableData);

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

  private ActorSelection getRandomRegistryActor() {
    ClusterEvent.CurrentClusterState clusterState = clusterWrapper.getState();
    ActorSelection actor = null;
    Set<Member> members = JavaConversions.asJavaSet(clusterState.members());
    int memberSize = members.size();
    // Don't select yourself
    if(memberSize > 1) {
      Address currentNodeAddress = clusterWrapper.getAddress();
      int index = new Random().nextInt(memberSize);
      int i = 0;
      // keeping previous member, in case when random index member is same as current actor
      // and current actor member is last in set
      Member previousMember = null;
      for(Member member : members){
        if(i == index-1) {
          previousMember = member;
        }
        if(i == index) {
          if(!currentNodeAddress.equals(member.address())) {
            actor = this.context().actorSelection(member.address() + ActorConstants.RPC_REGISTRY_PATH);
            break;
          } else if(index < memberSize-1){ // pick the next element in the set
            index++;
          }
        }
        i++;
      }
      if(actor == null && previousMember != null) {
        actor = this.context().actorSelection(previousMember.address() + ActorConstants.RPC_REGISTRY_PATH);
      }
    }
    return actor;
  }

  private class SendRoutingTable implements Runnable {

    @Override
    public void run() {
      RoutingTableData routingTableData =
          new RoutingTableData(routingTable.getGlobalRpcMap(), routingTable.getRoutedRpcMap());
      LOG.debug("Sending routing table for sync {}", routingTableData);
      ActorSelection actor = getRandomRegistryActor();
      if(actor != null) {
        actor.tell(routingTableData, self());
      }
    }
  }
}
