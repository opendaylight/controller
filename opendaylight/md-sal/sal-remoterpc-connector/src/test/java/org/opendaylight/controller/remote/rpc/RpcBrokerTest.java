/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.Futures;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.remote.rpc.messages.AddRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.AddRpc;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.InvokeRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.registry.ClusterWrapper;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistryOld;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RpcBrokerTest {

  static ActorSystem system;


  @BeforeClass
  public static void setup() {
    system = ActorSystem.create();
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testInvokeRpcError() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(Mockito.mock(ClusterWrapper.class)));
      Broker.ProviderSession brokerSession = Mockito.mock(Broker.ProviderSession.class);
      SchemaContext schemaContext = mock(SchemaContext.class);
      ActorRef rpcBroker = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext));
      QName rpc = new QName(new URI("noactor1"), "noactor1");
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "no child"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);
      InvokeRpc invokeMsg = new InvokeRpc(rpc, input);
      rpcBroker.tell(invokeMsg, getRef());

      Boolean getMsg = new ExpectMsg<Boolean>("ErrorResponse") {
        protected Boolean match(Object in) {
          if (in instanceof ErrorResponse) {
            ErrorResponse reply = (ErrorResponse)in;
            return reply.getException().getMessage().contains("No remote actor found for rpc execution of :");
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);
    }};
  }

  /**
   * This test method invokes and executes the remote rpc
   */

  @Test
  public void testInvokeRpc() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(mock(ClusterWrapper.class)));
      Broker.ProviderSession brokerSession = mock(Broker.ProviderSession.class);
      SchemaContext schemaContext = mock(SchemaContext.class);
      ActorRef rpcBroker = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext));
      ActorRef rpcBrokerRemote = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext), "actor1");
      // Add RPC in table
      QName rpc = new QName(new URI("actor1"), "actor1");
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, rpc, null);
      final String route = rpcBrokerRemote.path().toString();
      AddRpc rpcMsg = new AddRpc(routeId, route);
      rpcRegistry.tell(rpcMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      // invoke rpc
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "child1"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);
      CompositeNode invokeRpcResult = mock(CompositeNode.class);
      Collection<RpcError> errors = new ArrayList<>();
      RpcResult<CompositeNode> result = Rpcs.getRpcResult(true, invokeRpcResult, errors);
      Future<RpcResult<CompositeNode>> rpcResult = Futures.immediateFuture(result);
      when(brokerSession.rpc(rpc, input)).thenReturn(rpcResult);
      InvokeRpc invokeMsg = new InvokeRpc(rpc, input);
      rpcBroker.tell(invokeMsg, getRef());

      //verify response msg
      Boolean getMsg = new ExpectMsg<Boolean>("RpcResponse") {
        protected Boolean match(Object in) {
          if (in instanceof RpcResponse) {
            return true;
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);
    }};
  }

  @Test
  public void testInvokeRoutedRpcError() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(Mockito.mock(ClusterWrapper.class)));
      Broker.ProviderSession brokerSession = Mockito.mock(Broker.ProviderSession.class);
      SchemaContext schemaContext = mock(SchemaContext.class);
      ActorRef rpcBroker = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext));
      QName rpc = new QName(new URI("actor1"), "actor1");
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "child1"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);
      InvokeRoutedRpc invokeMsg = new InvokeRoutedRpc(rpc, YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(rpc)), input);
      rpcBroker.tell(invokeMsg, getRef());

      Boolean getMsg = new ExpectMsg<Boolean>("ErrorResponse") {
        protected Boolean match(Object in) {
          if (in instanceof ErrorResponse) {
            ErrorResponse reply = (ErrorResponse)in;
            return "No remote actor found for rpc execution.".equals(reply.getException().getMessage());
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);
    }};
  }

  /**
   * This test method invokes and executes the remote routed rpc
   */

  @Test
  public void testInvokeRoutedRpc() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(mock(ClusterWrapper.class)));
      Broker.ProviderSession brokerSession = mock(Broker.ProviderSession.class);
      SchemaContext schemaContext = mock(SchemaContext.class);
      ActorRef rpcBroker = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext));
      ActorRef rpcBrokerRemote = system.actorOf(RpcBroker.props(brokerSession, rpcRegistry, schemaContext), "actor2");
      // Add Routed RPC in table
      QName rpc = new QName(new URI("actor2"), "actor2");
      YangInstanceIdentifier identifier = YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(rpc));
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, rpc, identifier);
      final String route = rpcBrokerRemote.path().toString();
      Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = new HashSet<>();
      routeIds.add(routeId);

      AddRoutedRpc rpcMsg = new AddRoutedRpc(routeIds, route);
      rpcRegistry.tell(rpcMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      // invoke rpc
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "child1"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);
      CompositeNode invokeRpcResult = mock(CompositeNode.class);
      Collection<RpcError> errors = new ArrayList<>();
      RpcResult<CompositeNode> result = Rpcs.getRpcResult(true, invokeRpcResult, errors);
      Future<RpcResult<CompositeNode>> rpcResult = Futures.immediateFuture(result);
      when(brokerSession.rpc(rpc, input)).thenReturn(rpcResult);
      InvokeRoutedRpc invokeMsg = new InvokeRoutedRpc(rpc, identifier, input);
      rpcBroker.tell(invokeMsg, getRef());

      //verify response msg
      Boolean getMsg = new ExpectMsg<Boolean>("RpcResponse") {
        protected Boolean match(Object in) {
          if (in instanceof RpcResponse) {
            return true;
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);
    }};
  }

}
