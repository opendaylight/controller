/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;
import org.opendaylight.controller.remote.rpc.messages.AddRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.AddRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRoutedRpcReply;
import org.opendaylight.controller.remote.rpc.messages.GetRpc;
import org.opendaylight.controller.remote.rpc.messages.GetRpcReply;
import org.opendaylight.controller.remote.rpc.messages.RemoveRoutedRpc;
import org.opendaylight.controller.remote.rpc.messages.RemoveRpc;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class RpcRegistryTest {

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

  /**
   This test add, read and remove an entry in global rpc
   */
  @Test
  public void testGlobalRpc() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(Mockito.mock(ClusterWrapper.class)));
      QName type = new QName(new URI("actor1"), "actor1");
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
      final String route = "actor1";

      AddRpc rpcMsg = new AddRpc(routeId, route);
      rpcRegistry.tell(rpcMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      GetRpc getRpc = new GetRpc(routeId);
      rpcRegistry.tell(getRpc, getRef());

      Boolean getMsg = new ExpectMsg<Boolean>("GetRpcReply") {
        protected Boolean match(Object in) {
          if (in instanceof GetRpcReply) {
            GetRpcReply reply = (GetRpcReply)in;
            return route.equals(reply.getRoutePath());
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);

      RemoveRpc removeMsg = new RemoveRpc(routeId);
      rpcRegistry.tell(removeMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      rpcRegistry.tell(getRpc, getRef());

      Boolean getNullMsg = new ExpectMsg<Boolean>("GetRpcReply") {
        protected Boolean match(Object in) {
          if (in instanceof GetRpcReply) {
            GetRpcReply reply = (GetRpcReply)in;
            return reply.getRoutePath() == null;
          } else {
            throw noMatch();
          }
        }
      }.get();
      Assert.assertTrue(getNullMsg);
    }};

  }

  /**
   This test add, read and remove an entry in routed rpc
   */
  @Test
  public void testRoutedRpc() throws URISyntaxException {
    new JavaTestKit(system) {{
      ActorRef rpcRegistry = system.actorOf(RpcRegistryOld.props(Mockito.mock(ClusterWrapper.class)));
      QName type = new QName(new URI("actor1"), "actor1");
      RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, type, null);
      final String route = "actor1";

      Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIds = new HashSet<>();
      routeIds.add(routeId);

      AddRoutedRpc rpcMsg = new AddRoutedRpc(routeIds, route);
      rpcRegistry.tell(rpcMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      GetRoutedRpc getRpc = new GetRoutedRpc(routeId);
      rpcRegistry.tell(getRpc, getRef());

      Boolean getMsg = new ExpectMsg<Boolean>("GetRoutedRpcReply") {
        protected Boolean match(Object in) {
          if (in instanceof GetRoutedRpcReply) {
            GetRoutedRpcReply reply = (GetRoutedRpcReply)in;
            return route.equals(reply.getRoutePath());
          } else {
            throw noMatch();
          }
        }
      }.get(); // this extracts the received message

      Assert.assertTrue(getMsg);

      RemoveRoutedRpc removeMsg = new RemoveRoutedRpc(routeIds, route);
      rpcRegistry.tell(removeMsg, getRef());
      expectMsgEquals(duration("2 second"), "Success");

      rpcRegistry.tell(getRpc, getRef());

      Boolean getNullMsg = new ExpectMsg<Boolean>("GetRoutedRpcReply") {
        protected Boolean match(Object in) {
          if (in instanceof GetRoutedRpcReply) {
            GetRoutedRpcReply reply = (GetRoutedRpcReply)in;
            return reply.getRoutePath() == null;
          } else {
            throw noMatch();
          }
        }
      }.get();
      Assert.assertTrue(getNullMsg);
    }};

  }

}
