/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpcReply;
import org.opendaylight.controller.remote.rpc.messages.GetRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRpcReply;
import org.opendaylight.controller.remote.rpc.messages.InvokeRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 * Actor to initiate execution of remote RPC on other nodes of the cluster.
 */

public class RpcBroker extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcBroker.class);
  private final Broker.ProviderSession brokerSession;
  private final ActorRef rpcRegistry;

  private RpcBroker(Broker.ProviderSession brokerSession, ActorRef rpcRegistry){
    this.brokerSession = brokerSession;
    this.rpcRegistry = rpcRegistry;
  }

  public static Props props(final Broker.ProviderSession brokerSession, final ActorRef rpcRegistry){
    return Props.create(new Creator<RpcBroker>(){

      @Override
      public RpcBroker create() throws Exception {
        return new RpcBroker(brokerSession, rpcRegistry);
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
    try {
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, msg.getRpc(), msg.getIdentifier());
      GetRoutedRpc routedRpcMsg = new GetRoutedRpc(routeId);
      GetRoutedRpcReply rpcReply = (GetRoutedRpcReply)ActorUtil.executeLocalOperation(rpcRegistry, routedRpcMsg, ActorUtil.ASK_DURATION);

      String remoteActorPath = rpcReply.getRoutePath();
      if(remoteActorPath == null) {
        LOG.debug("No remote actor found for rpc execution.");

        getSender().tell(new ErrorResponse(
          new IllegalStateException("No remote actor found for rpc execution.")), self());
      } else {
        ExecuteRpc executeMsg = new ExecuteRpc(msg.getRpc(), msg.getInput());

        Object operationRes = ActorUtil.executeRemoteOperation(this.context().actorSelection(remoteActorPath),
            executeMsg, ActorUtil.ASK_DURATION);

        getSender().tell(operationRes, self());
      }
    } catch (Exception e) {
        LOG.error(e.toString());
        getSender().tell(new ErrorResponse(e), self());
    }
  }

  private void invokeRemoteRpc(InvokeRpc msg) {
    // Look up the remote actor to execute rpc
    LOG.debug("Looking up the remote actor for route {}", msg);
    try {
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, msg.getRpc(), null);
      GetRpc rpcMsg = new GetRpc(routeId);
      GetRpcReply rpcReply = (GetRpcReply)ActorUtil.executeLocalOperation(rpcRegistry, rpcMsg, ActorUtil.ASK_DURATION);
      String remoteActorPath = rpcReply.getRoutePath();

      if(remoteActorPath == null) {
        LOG.debug("No remote actor found for rpc execution.");

        getSender().tell(new ErrorResponse(
          new IllegalStateException("No remote actor found for rpc execution.")), self());
      } else {
        ExecuteRpc executeMsg = new ExecuteRpc(msg.getRpc(), msg.getInput());
        Object operationRes = ActorUtil.executeRemoteOperation(this.context().actorSelection(remoteActorPath),
            executeMsg, ActorUtil.ASK_DURATION);

        getSender().tell(operationRes, self());
      }
    } catch (Exception e) {
        LOG.error(e.toString());
        getSender().tell(new ErrorResponse(e), self());
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
