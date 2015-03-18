/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RouteIdentifierImpl implements RpcRouter.RouteIdentifier<QName, QName, YangInstanceIdentifier>,Serializable {
  private static final long serialVersionUID = 1L;

  private final QName context;
  private final QName type;
  private final YangInstanceIdentifier route;

  public RouteIdentifierImpl(final QName context, final QName type, final YangInstanceIdentifier route) {
    Preconditions.checkNotNull(type, "Rpc type should not be null");
    this.context = context;
    this.type = type;
    this.route = route;
  }

  @Override
  public QName getContext() {
    return context;
  }

  @Override
  public QName getType() {
    return type;
  }

  @Override
  public YangInstanceIdentifier getRoute() {
    return route;
  }


  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final RouteIdentifierImpl that = (RouteIdentifierImpl) o;

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
    final int prime = 31;
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
