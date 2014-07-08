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
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class RpcBroker extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcBroker.class);
  private RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable;
  private Broker.ProviderSession brokerSession;

  private RpcBroker(RoutingTable<RpcRouter.RouteIdentifier<?, ?, ?>, String> routingTable,
                    Broker.ProviderSession brokerSession){
    this.routingTable = routingTable;
    this.brokerSession = brokerSession;
  }

  public static Props props(final RoutingTable routingTable, final Broker.ProviderSession brokerSession){
    return Props.create(new Creator<RpcBroker>(){

      @Override
      public RpcBroker create() throws Exception {
        return new RpcBroker(routingTable, brokerSession);
      }
    });
  }
  @Override
  protected void handleReceive(Object message) throws Exception {
    if(message instanceof InvokeRoutedRpc) {
      invokeRemoteRoutedRpc((InvokeRoutedRpc) message);
    } else if(message instanceof InvokeRpc) {
      invokeRemoteRpc((InvokeRpc) message);
    } else if(message instanceof ExecuteRpc) {
      executeRpc((ExecuteRpc) message);
    }
  }

  private void invokeRemoteRoutedRpc(InvokeRoutedRpc msg) {
    // Look up the remote actor to execute rpc
    LOG.debug("Looking up the remote actor for route {}", msg);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(msg.getRpc());
    routeId.setRoute(msg.getIdentifier());
    String remoteActorPath = routingTable.getLastAddedRoutedRpc(routeId);
    if(remoteActorPath == null) {
      LOG.debug("No remote actor found for rpc execution.");
      getSender().tell(new ErrorResponse(
          new IllegalStateException("No remote actor found for rpc execution.")), self());
    } else {
      ExecuteRpc executeMsg = new ExecuteRpc(msg.getRpc(), msg.getInput());
      try {
        Object operationRes = ActorUtil.executeRemoteOperation(this.context().actorSelection(remoteActorPath),
            executeMsg, ActorUtil.ASK_DURATION);
        getSender().tell(operationRes, self());
      } catch (Exception e) {
        LOG.error(e.toString());
        getSender().tell(new ErrorResponse(e), self());
      }
    }
  }

  private void invokeRemoteRpc(InvokeRpc msg) {
    // Look up the remote actor to execute rpc
    LOG.debug("Looking up the remote actor for route {}", msg);
    RouteIdentifierImpl routeId = new RouteIdentifierImpl();
    routeId.setType(msg.getRpc());
    String remoteActorPath = routingTable.getGlobalRoute(routeId);
    if(remoteActorPath == null) {
      LOG.debug("No remote actor found for rpc execution.");
      getSender().tell(new ErrorResponse(
          new IllegalStateException("No remote actor found for rpc execution.")), self());
    } else {
      ExecuteRpc executeMsg = new ExecuteRpc(msg.getRpc(), msg.getInput());
      try {
        Object operationRes = ActorUtil.executeRemoteOperation(this.context().actorSelection(remoteActorPath),
            executeMsg, ActorUtil.ASK_DURATION);
        getSender().tell(operationRes, self());
      } catch (Exception e) {
        LOG.error(e.toString());
        getSender().tell(new ErrorResponse(e), self());
      }
    }
  }

  private void executeRpc(ExecuteRpc msg) {
    LOG.debug("Executing rpc for rpc {}", msg.getRpc());
    try {
      Future<RpcResult<CompositeNode>> rpc = brokerSession.rpc(msg.getRpc(), msg.getInput());
      RpcResult<CompositeNode> rpcResult = rpc != null ? rpc.get():null;
      CompositeNode result = rpcResult != null ? rpcResult.getResult() : null;
      getSender().tell(new RpcResponse(result), self());
    } catch (Exception e) {
      LOG.error(e.toString());
      getSender().tell(new ErrorResponse(e), self());
    }
  }

}
