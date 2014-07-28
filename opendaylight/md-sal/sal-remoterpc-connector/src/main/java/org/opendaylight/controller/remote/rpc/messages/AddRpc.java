/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.messages;

import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;

import java.io.Serializable;

public class AddRpc implements Serializable {

  RouteIdentifierImpl routeId;
  String actorPath;

  public AddRpc(RouteIdentifierImpl routeId, String actorPath) {
    this.routeId = routeId;
    this.actorPath = actorPath;
  }

  public RouteIdentifierImpl getRouteId() {
    return routeId;
  }

  public String getActorPath() {
    return actorPath;
  }
}