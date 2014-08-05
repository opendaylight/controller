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
import akka.japi.Pair;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.Futures;
import com.typesafe.config.ConfigFactory;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.remote.rpc.messages.ErrorResponse;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RpcBrokerTest {

  static ActorSystem node1;
  static ActorSystem node2;
  private ActorRef rpcBroker1;
  private JavaTestKit probeReg1;
  private ActorRef rpcBroker2;
  private JavaTestKit probeReg2;
  private Broker.ProviderSession brokerSession;


  @BeforeClass
  public static void setup() throws InterruptedException {
    node1 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberA"));
    node2 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberB"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(node1);
    JavaTestKit.shutdownActorSystem(node2);
    node1 = null;
    node2 = null;
  }

  @Before
  public void createActor() {
    brokerSession = Mockito.mock(Broker.ProviderSession.class);
    SchemaContext schemaContext = mock(SchemaContext.class);
    probeReg1 = new JavaTestKit(node1);
    rpcBroker1 = node1.actorOf(RpcBroker.props(brokerSession, probeReg1.getRef(), schemaContext));
    probeReg2 = new JavaTestKit(node2);
    rpcBroker2 = node2.actorOf(RpcBroker.props(brokerSession, probeReg2.getRef(), schemaContext));

  }
  @Test
  public void testInvokeRpcError() throws Exception {
    new JavaTestKit(node1) {{
      QName rpc = new QName(new URI("noactor1"), "noactor1");
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "no child"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);


      InvokeRpc invokeMsg = new InvokeRpc(rpc, null, input);
      rpcBroker1.tell(invokeMsg, getRef());
      probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
      probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(new ArrayList<Pair<ActorRef, Long>>()));

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
    new JavaTestKit(node1) {{
      QName rpc = new QName(new URI("noactor1"), "noactor1");
      // invoke rpc
      CompositeNode input = new ImmutableCompositeNode(QName.create("ns", "2013-12-09", "child1"), new ArrayList<Node<?>>(), ModifyAction.REPLACE);
      InvokeRpc invokeMsg = new InvokeRpc(rpc, null, input);
      rpcBroker1.tell(invokeMsg, getRef());

      probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
      List<Pair<ActorRef, Long>> routerList = new ArrayList<Pair<ActorRef, Long>>();

      routerList.add(new Pair<ActorRef, Long>(rpcBroker2, 200L));

      probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(routerList));

      CompositeNode invokeRpcResult = mock(CompositeNode.class);
      Collection<RpcError> errors = new ArrayList<>();
      RpcResult<CompositeNode> result = Rpcs.getRpcResult(true, invokeRpcResult, errors);
      Future<RpcResult<CompositeNode>> rpcResult = Futures.immediateFuture(result);
      when(brokerSession.rpc(rpc, input)).thenReturn(rpcResult);

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
