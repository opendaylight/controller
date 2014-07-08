/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import org.opendaylight.controller.remote.rpc.messages.AddRpc;
import org.opendaylight.controller.remote.rpc.messages.RemoveRpc;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcListener implements RpcRegistrationListener{

  private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);
  private final ActorRef rpcRegistry;
  private final String actorPath;

  public RpcListener(ActorRef rpcRegistry, String actorPath) {
    this.rpcRegistry = rpcRegistry;
    this.actorPath = actorPath;
  }

  @Override
  public void onRpcImplementationAdded(QName rpc) {
    LOG.debug("Adding registration for [{}]", rpc);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, rpc, null);
    AddRpc addRpcMsg = new AddRpc(routeId, actorPath);
    try {
      ActorUtil.executeLocalOperation(rpcRegistry, addRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
      LOG.debug("Route added [{}-{}]", routeId, this.actorPath);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error(e.toString());
    }

  }

  @Override
  public void onRpcImplementationRemoved(QName rpc) {
    LOG.debug("Removing registration for [{}]", rpc);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, rpc, null);
    RemoveRpc removeRpcMsg = new RemoveRpc(routeId);
    try {
      ActorUtil.executeLocalOperation(rpcRegistry, removeRpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);
    } catch (Exception e) {
      // Just logging it because Akka API throws this exception
      LOG.error(e.toString());
    }
  }
}
