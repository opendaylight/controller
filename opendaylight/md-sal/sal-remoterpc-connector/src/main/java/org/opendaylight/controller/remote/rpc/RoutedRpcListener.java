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
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class RoutedRpcListener implements RouteChangeListener<RpcRoutingContext, InstanceIdentifier>{
  @Override
  public void onRouteChange(RouteChange<RpcRoutingContext, InstanceIdentifier> change) {
   // TODO : change routing registry
  }
}
