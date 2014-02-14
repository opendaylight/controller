/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.dto;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RouteIdentifierImpl that = (RouteIdentifierImpl) o;

    if (context == null){
      if (that.getContext() != null)  return false;
    }else
      if (!context.equals(that.context)) return false;

    if (route == null){
      if (that.getRoute() != null) return false;
    }else
      if (!route.equals(that.route)) return false;

    if (type == null){
      if (that.getType() != null) return false;
    }else
      if (!type.equals(that.type)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 0;
    result = prime * result + (context == null ? 0:context.hashCode());
    result = prime * result + (type    == null ? 0:type.hashCode());
    result = prime * result + (route   == null ? 0:route.hashCode());
    return result;
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
