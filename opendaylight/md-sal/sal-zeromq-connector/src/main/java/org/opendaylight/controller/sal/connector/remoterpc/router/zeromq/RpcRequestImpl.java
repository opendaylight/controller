/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.router.zeromq;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;

public class RpcRequestImpl implements RpcRouter.RpcRequest<QName, QName, InstanceIdentifier, Object>,Serializable {

  private RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier;
  private Object payload;

  @Override
  public RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> getRoutingInformation() {
    return routeIdentifier;
  }

  public void setRouteIdentifier(RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> routeIdentifier) {
    this.routeIdentifier = routeIdentifier;
  }

  @Override
  public Object getPayload() {
    return payload;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

}
