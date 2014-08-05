/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.utils.ActorUtil;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RpcListener implements RpcRegistrationListener{

  private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);
  private final ActorRef rpcRegistry;

  public RpcListener(ActorRef rpcRegistry) {
    this.rpcRegistry = rpcRegistry;
  }

  @Override
  public void onRpcImplementationAdded(QName rpc) {
    LOG.debug("Adding registration for [{}]", rpc);
    RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(null, rpc, null);
    List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>();
    routeIds.add(routeId);
    RpcRegistry.Messages.AddOrUpdateRoutes addRpcMsg = new RpcRegistry.Messages.AddOrUpdateRoutes(routeIds);
    try {
      ActorUtil.executeOperation(rpcRegistry, addRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
      LOG.debug("Route added [{}]", routeId);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error("onRpcImplementationAdded: {}", e);
    }

  }

  @Override
  public void onRpcImplementationRemoved(QName rpc) {
    LOG.debug("Removing registration for [{}]", rpc);
    RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(null, rpc, null);
    List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>();
    routeIds.add(routeId);
    RpcRegistry.Messages.RemoveRoutes removeRpcMsg = new RpcRegistry.Messages.RemoveRoutes(routeIds);
    try {
      ActorUtil.executeOperation(rpcRegistry, removeRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error("onRpcImplementationRemoved: {}", e);
    }
  }
}
