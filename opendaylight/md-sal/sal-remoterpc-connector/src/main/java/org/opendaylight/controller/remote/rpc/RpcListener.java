/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcListener implements RpcRegistrationListener{

  private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);
  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private String actorPath;

  public RpcListener(RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable, String actorPath) {
    this.routingTable = routingTable;
    this.actorPath = actorPath;
  }

  @Override
  public void onRpcImplementationAdded(QName rpc) {
    LOG.debug("Adding registration for [{}]", rpc);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(rpc);
    routingTable.addGlobalRoute(routeId, this.actorPath);

    LOG.debug("Route added [{}-{}]", routeId, this.actorPath);

  }

  @Override
  public void onRpcImplementationRemoved(QName rpc) {
    LOG.debug("Removing registration for [{}]", rpc);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(rpc);
    routingTable.removeGlobalRoute(routeId);
  }
}
