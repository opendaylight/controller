/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RoutedRpcListener implements RouteChangeListener<RpcRoutingContext, InstanceIdentifier>{
  private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcListener.class);
  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private String actorPath;

  public RoutedRpcListener(RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable, String actorPath) {
    this.routingTable = routingTable;
    this.actorPath = actorPath;
  }

  @Override
  public void onRouteChange(RouteChange<RpcRoutingContext, InstanceIdentifier> routeChange) {
    Map<RpcRoutingContext, Set<InstanceIdentifier>> announcements = routeChange.getAnnouncements();
    announce(getRouteIdentifiers(announcements));

    Map<RpcRoutingContext, Set<InstanceIdentifier>> removals = routeChange.getRemovals();
    remove(getRouteIdentifiers(removals));
  }

  /**
   *
   * @param announcements
   */
  private void announce(Set<RpcRouter.RouteIdentifier<?, ?, ?>> announcements) {
    LOG.debug("Announcing [{}]", announcements);
    routingTable.addRoutedRpcs(announcements, actorPath);
  }

  /**
   *
   * @param removals
   */
  private void remove(Set<RpcRouter.RouteIdentifier<?, ?, ?>> removals){
    LOG.debug("Removing [{}]", removals);
    routingTable.removeRoutes(removals, actorPath);
  }

  /**
   *
   * @param changes
   * @return
   */
  private Set<RpcRouter.RouteIdentifier<?, ?, ?>> getRouteIdentifiers(Map<RpcRoutingContext, Set<InstanceIdentifier>> changes) {
    RouteIdentifierImpl routeId = null;
    Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdSet = new HashSet<>();

    for (RpcRoutingContext context : changes.keySet()){
      routeId = new RouteIdentifierImpl();
      routeId.setType(context.getRpc());
      //routeId.setContext(context.getContext());

      for (InstanceIdentifier instanceId : changes.get(context)){
        routeId.setRoute(instanceId);
        routeIdSet.add(routeId);
      }
    }
    return routeIdSet;
  }
}
