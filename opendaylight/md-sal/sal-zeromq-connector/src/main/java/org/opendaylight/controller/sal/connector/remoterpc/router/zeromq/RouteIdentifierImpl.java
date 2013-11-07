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

/**
 * User: abhishk2
 */
public class RouteIdentifierImpl implements RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>,Serializable {

  private QName context;
  private QName type;
  private InstanceIdentifier route;

  @Override
  public QName getContext() {
    return this.context;
  }

  @Override
  public QName getType() {
    return this.type;
  }

  @Override
  public InstanceIdentifier getRoute() {
    return this.route;
  }

  public void setContext(QName context) {
    this.context = context;
  }

  public void setType(QName type) {
    this.type = type;
  }

  public void setRoute(InstanceIdentifier route) {
    this.route = route;
  }

  @Override
  public String toString() {
    return "RouteIdentifierImpl{" +
        "context=" + context +
        ", type=" + type +
        ", route=" + route +
        '}';
  }
}
