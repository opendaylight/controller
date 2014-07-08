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
import java.util.Set;

public class RemoveRoutedRpc implements Serializable {

  Set<RpcRouter.RouteIdentifier<?, ?, ?>> announcements;
  String actorPath;

  public RemoveRoutedRpc(Set<RpcRouter.RouteIdentifier<?, ?, ?>> announcements, String actorPath) {
    this.announcements = announcements;
    this.actorPath = actorPath;
  }

  public Set<RpcRouter.RouteIdentifier<?, ?, ?>> getAnnouncements() {
    return announcements;
  }

  public String getActorPath() {
    return actorPath;
  }
}
