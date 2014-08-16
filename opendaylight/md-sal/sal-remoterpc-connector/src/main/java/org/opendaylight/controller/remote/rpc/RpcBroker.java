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
import akka.japi.Pair;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.utils.LatestEntryRoutingLogic;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.utils.ActorUtil;
import org.opendaylight.controller.remote.rpc.utils.RoutingLogic;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Actor to initiate execution of remote RPC on other nodes of the cluster.
 */

public class RpcBroker extends AbstractUntypedActor {

  private static final Logger LOG = LoggerFactory.getLogger(RpcBroker.class);
  private final Broker.ProviderSession brokerSession;
  private final ActorRef rpcRegistry;
  private SchemaContext schemaContext;

  private RpcBroker(Broker.ProviderSession brokerSession, ActorRef rpcRegistry, SchemaContext schemaContext){
    this.brokerSession = brokerSession;
    this.rpcRegistry = rpcRegistry;
    this.schemaContext = schemaContext;
  }

  public static Props props(final Broker.ProviderSession brokerSession, final ActorRef rpcRegistry, final SchemaContext schemaContext){
    return Props.create(new Creator<RpcBroker>(){

      @Override
      public RpcBroker create() throws Exception {
        return new RpcBroker(brokerSession, rpcRegistry, schemaContext);
      }
    });
  }
  @Override
  protected void handleReceive(Object message) throws Exception {
   if(message instanceof InvokeRpc) {
      invokeRemoteRpc((InvokeRpc) message);
    } else if(message instanceof ExecuteRpc) {
      executeRpc((ExecuteRpc) message);
    }
  }

  private void invokeRemoteRpc(InvokeRpc msg) {
    // Look up the remote actor to execute rpc
    LOG.debug("Looking up the remote actor for route {}", msg);
    try {
      // Find router
      RpcRouter.RouteIdentifier<?,?,?> routeId = new RouteIdentifierImpl(null, msg.getRpc(), msg.getIdentifier());
      RpcRegistry.Messages.FindRouters rpcMsg = new RpcRegistry.Messages.FindRouters(routeId);
      RpcRegistry.Messages.FindRoutersReply rpcReply =
          (RpcRegistry.Messages.FindRoutersReply) ActorUtil.executeOperation(rpcRegistry, rpcMsg, ActorUtil.LOCAL_ASK_DURATION, ActorUtil.LOCAL_AWAIT_DURATION);

      List<Pair<ActorRef, Long>> actorRefList = rpcReply.getRouterWithUpdateTime();

      if(actorRefList == null || actorRefList.isEmpty()) {
        LOG.debug("No remote actor found for rpc {{}}.", msg.getRpc());

        getSender().tell(new ErrorResponse(
            new IllegalStateException("No remote actor found for rpc execution of : " + msg.getRpc())), self());
      } else {
        RoutingLogic logic = new LatestEntryRoutingLogic(actorRefList);

        ExecuteRpc executeMsg = new ExecuteRpc(XmlUtils.inputCompositeNodeToXml(msg.getInput(), schemaContext), msg.getRpc());
        Object operationRes = ActorUtil.executeOperation(logic.select(),
            executeMsg, ActorUtil.REMOTE_ASK_DURATION, ActorUtil.REMOTE_AWAIT_DURATION);

        getSender().tell(operationRes, self());
      }
    } catch (Exception e) {
        LOG.error("invokeRemoteRpc: {}", e);
        getSender().tell(new ErrorResponse(e), self());
    }
  }



  private void executeRpc(ExecuteRpc msg) {
    LOG.debug("Executing rpc for rpc {}", msg.getRpc());
    try {
      Future<RpcResult<CompositeNode>> rpc = brokerSession.rpc(msg.getRpc(),
          XmlUtils.inputXmlToCompositeNode(msg.getRpc(), msg.getInputCompositeNode(), schemaContext));
      RpcResult<CompositeNode> rpcResult = rpc != null ? rpc.get():null;
      CompositeNode result = rpcResult != null ? rpcResult.getResult() : null;
      getSender().tell(new RpcResponse(XmlUtils.outputCompositeNodeToXml(result, schemaContext)), self());
    } catch (Exception e) {
      LOG.error("executeRpc: {}", e);
      getSender().tell(new ErrorResponse(e), self());
    }
  }

}
