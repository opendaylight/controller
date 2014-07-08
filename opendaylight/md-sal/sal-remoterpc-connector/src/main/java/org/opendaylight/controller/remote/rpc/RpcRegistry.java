/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.Props;
import akka.japi.Creator;

public class RpcRegistry extends AbstractUntypedActor {

  private RoutingTable routingTable;
  private RpcRegistry(RoutingTable routingTable){
    this.routingTable = routingTable;
  }

  public static Props props(final RoutingTable routingTable){
    return Props.create(new Creator<RpcRegistry>(){

      @Override
      public RpcRegistry create() throws Exception {
        return new RpcRegistry(routingTable);
      }
    });
  }

  @Override
  protected void handleReceive(Object message) throws Exception {

  }
}
