/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcListener implements RpcRegistrationListener{

  private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);
  private final ActorRef rpcRegistry;

  public RpcListener(ActorRef rpcRegistry) {
    this.rpcRegistry = rpcRegistry;
  }

  private volatile boolean first = true;

  @Override
  public void onRpcImplementationAdded(QName rpc) {
    LOG.info("onRpcImplementationAdded: {}",rpc);
    if(first) {
        first = false;
        Thread.dumpStack();
    }
    if(LOG.isDebugEnabled()) {
        LOG.debug("Adding registration for [{}]", rpc);
    }
    RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(null, rpc, null);
    List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>();
    routeIds.add(routeId);
    RpcRegistry.Messages.AddOrUpdateRoutes addRpcMsg = new RpcRegistry.Messages.AddOrUpdateRoutes(routeIds);
    rpcRegistry.tell(addRpcMsg, ActorRef.noSender());
  }

  @Override
  public void onRpcImplementationRemoved(QName rpc) {
    if(LOG.isDebugEnabled()) {
        LOG.debug("Removing registration for [{}]", rpc);
    }
    RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(null, rpc, null);
    List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>();
    routeIds.add(routeId);
    RpcRegistry.Messages.RemoveRoutes removeRpcMsg = new RpcRegistry.Messages.RemoveRoutes(routeIds);
    rpcRegistry.tell(removeRpcMsg, ActorRef.noSender());
  }
}
