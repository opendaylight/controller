/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.remote.rpc.messages.AddRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.RemoveRoutedRpc;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RoutedRpcListener implements RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>{
  private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcListener.class);
  private final ActorRef rpcRegistry;
  private final String actorPath;

  public RoutedRpcListener(ActorRef rpcRegistry, String actorPath) {
    Preconditions.checkNotNull(rpcRegistry, "rpc registry actor should not be null");
    Preconditions.checkNotNull(actorPath, "actor path of rpc broker on current node should not be null");

    this.rpcRegistry = rpcRegistry;
    this.actorPath = actorPath;
  }

  @Override
  public void onRouteChange(RouteChange<RpcRoutingContext, YangInstanceIdentifier> routeChange) {
    Map<RpcRoutingContext, Set<YangInstanceIdentifier>> announcements = routeChange.getAnnouncements();
    announce(getRouteIdentifiers(announcements));

    Map<RpcRoutingContext, Set<YangInstanceIdentifier>> removals = routeChange.getRemovals();
    remove(getRouteIdentifiers(removals));
  }

  /**
   *
   * @param announcements
   */
  private void announce(Set<RpcRouter.RouteIdentifier<?, ?, ?>> announcements) {
    LOG.debug("Announcing [{}]", announcements);
    AddRoutedRpc addRpcMsg = new AddRoutedRpc(announcements, actorPath);
    try {
      ActorUtil.executeLocalOperation(rpcRegistry, addRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error(e.toString());
    }
  }

  /**
   *
   * @param removals
   */
  private void remove(Set<RpcRouter.RouteIdentifier<?, ?, ?>> removals){
    LOG.debug("Removing [{}]", removals);
    RemoveRoutedRpc removeRpcMsg = new RemoveRoutedRpc(removals, actorPath);
    try {
      ActorUtil.executeLocalOperation(rpcRegistry, removeRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error(e.toString());
    }
  }

  /**
   *
   * @param changes
   * @return
   */
  private Set<RpcRouter.RouteIdentifier<?, ?, ?>> getRouteIdentifiers(Map<RpcRoutingContext, Set<YangInstanceIdentifier>> changes) {
    RouteIdentifierImpl routeId = null;
    Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdSet = new HashSet<>();

    for (RpcRoutingContext context : changes.keySet()){
      for (YangInstanceIdentifier instanceId : changes.get(context)){
        routeId = new RouteIdentifierImpl(null, context.getRpc(), instanceId);
        routeIdSet.add(routeId);
      }
    }
    return routeIdSet;
  }
}
