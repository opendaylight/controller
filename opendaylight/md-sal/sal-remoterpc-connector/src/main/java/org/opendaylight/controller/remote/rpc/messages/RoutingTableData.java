/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.messages;

import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Map;

public class RoutingTableData implements Serializable {
  private Map<RpcRouter.RouteIdentifier<?, ?, ?>, String> rpcMap;
  private Map<RpcRouter.RouteIdentifier<?, ?, ?>, LinkedHashSet<String>> routedRpcMap;

  public RoutingTableData(Map<RpcRouter.RouteIdentifier<?, ?, ?>, String> rpcMap,
                          Map<RpcRouter.RouteIdentifier<?, ?, ?>, LinkedHashSet<String>> routedRpcMap) {
    this.rpcMap = rpcMap;
    this.routedRpcMap = routedRpcMap;
  }

  public Map<RpcRouter.RouteIdentifier<?, ?, ?>, String> getRpcMap() {
    return rpcMap;
  }

  public Map<RpcRouter.RouteIdentifier<?, ?, ?>, LinkedHashSet<String>> getRoutedRpcMap() {
    return routedRpcMap;
  }
}
