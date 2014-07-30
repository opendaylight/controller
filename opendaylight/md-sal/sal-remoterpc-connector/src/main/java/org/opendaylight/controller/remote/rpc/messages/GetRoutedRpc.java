/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.messages;


import com.google.common.base.Preconditions;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;

import java.io.Serializable;

public class GetRoutedRpc implements Serializable {

  private final RouteIdentifierImpl routeId;

  public GetRoutedRpc(final RouteIdentifierImpl routeId) {
    Preconditions.checkNotNull(routeId, "route id should not be null");
    this.routeId = routeId;
  }

  public RouteIdentifierImpl getRouteId() {
    return routeId;
  }
}
