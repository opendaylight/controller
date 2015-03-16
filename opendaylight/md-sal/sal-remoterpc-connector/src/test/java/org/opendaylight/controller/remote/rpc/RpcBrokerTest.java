/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import akka.japi.Pair;
import akka.testkit.JavaTestKit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRouters;
import org.opendaylight.controller.sal.connector.api.RpcRouter.RouteIdentifier;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;

public class RpcBrokerTest extends AbstractRpcTest {

//    @Test
//    public void testInvokeRpcWithNoRemoteActor() throws Exception {
//        new JavaTestKit(node1) {{
//            CompositeNode input = makeRPCInput("foo");
//
//            InvokeRpc invokeMsg = new InvokeRpc(TEST_RPC, null, input);
//            rpcBroker1.tell(invokeMsg, getRef());
//
//            probeReg1.expectMsgClass(duration("5 seconds"), RpcRegistry.Messages.FindRouters.class);
//            probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(
//                    Collections.<Pair<ActorRef, Long>>emptyList()));
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", RpcErrorsException.class, failure.cause().getClass());
//        }};
//    }
//
//
//    /**
//     * This test method invokes and executes the remote rpc
//     */
//    //@Test
//    public void testInvokeRpc() throws URISyntaxException {
//        new JavaTestKit(node1) {{
//            QName instanceQName = new QName(new URI("ns"), "instance");
//
//            CompositeNode invokeRpcResult = makeRPCOutput("bar");
//            RpcResult<CompositeNode> rpcResult =
//                               RpcResultBuilder.<CompositeNode>success(invokeRpcResult).build();
//            ArgumentCaptor<CompositeNode> inputCaptor = new ArgumentCaptor<>();
//            when(brokerSession.rpc(eq(TEST_RPC), inputCaptor.capture()))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            // invoke rpc
//            CompositeNode input = makeRPCInput("foo");
//            YangInstanceIdentifier instanceID = YangInstanceIdentifier.of(instanceQName);
//            InvokeRpc invokeMsg = new InvokeRpc(TEST_RPC, instanceID, input);
//            rpcBroker1.tell(invokeMsg, getRef());
//
//            FindRouters findRouters = probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
//            RouteIdentifier<?, ?, ?> routeIdentifier = findRouters.getRouteIdentifier();
//            assertEquals("getType", TEST_RPC, routeIdentifier.getType());
//            assertEquals("getRoute", instanceID, routeIdentifier.getRoute());
//
//            probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(
//                    Arrays.asList(new Pair<ActorRef, Long>(rpcBroker2, 200L))));
//
//            RpcResponse rpcResponse = expectMsgClass(duration("5 seconds"), RpcResponse.class);
//            assertCompositeNodeEquals((CompositeNode)invokeRpcResult.getValue().get(0),
//                    XmlUtils.xmlToCompositeNode(rpcResponse.getResultCompositeNode()));
//            assertCompositeNodeEquals(input, inputCaptor.getValue());
//        }};
//    }
//
//    @Test
//    public void testInvokeRpcWithNoOutput() {
//        new JavaTestKit(node1) {{
//
//            RpcResult<CompositeNode> rpcResult = RpcResultBuilder.<CompositeNode>success().build();
//            when(brokerSession.rpc(eq(TEST_RPC), any(CompositeNode.class)))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            InvokeRpc invokeMsg = new InvokeRpc(TEST_RPC, null, makeRPCInput("foo"));
//            rpcBroker1.tell(invokeMsg, getRef());
//
//            probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
//            probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(
//                    Arrays.asList(new Pair<ActorRef, Long>(rpcBroker2, 200L))));
//
//            RpcResponse rpcResponse = expectMsgClass(duration("5 seconds"), RpcResponse.class);
//
//            assertEquals("getResultCompositeNode", "", rpcResponse.getResultCompositeNode());
//        }};
//    }
//
//    @Test
//    public void testInvokeRpcWithExecuteFailure() {
//        new JavaTestKit(node1) {{
//
//            RpcResult<CompositeNode> rpcResult = RpcResultBuilder.<CompositeNode>failed()
//                    .withError(ErrorType.RPC, "tag", "error", "appTag", "info",
//                            new Exception("mock"))
//                    .build();
//            when(brokerSession.rpc(eq(TEST_RPC), any(CompositeNode.class)))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            InvokeRpc invokeMsg = new InvokeRpc(TEST_RPC, null, makeRPCInput("foo"));
//            rpcBroker1.tell(invokeMsg, getRef());
//
//            probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
//            probeReg1.reply(new RpcRegistry.Messages.FindRoutersReply(
//                    Arrays.asList(new Pair<ActorRef, Long>(rpcBroker2, 200L))));
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", RpcErrorsException.class, failure.cause().getClass());
//
//            RpcErrorsException errorsEx = (RpcErrorsException)failure.cause();
//            List<RpcError> rpcErrors = Lists.newArrayList(errorsEx.getRpcErrors());
//            assertEquals("RpcErrors count", 1, rpcErrors.size());
//            assertRpcErrorEquals(rpcErrors.get(0), ErrorSeverity.ERROR, ErrorType.RPC, "tag",
//                    "error", "appTag", "info", "mock");
//        }};
//    }
//
//    @Test
//    public void testInvokeRpcWithFindRoutersFailure() {
//        new JavaTestKit(node1) {{
//
//            InvokeRpc invokeMsg = new InvokeRpc(TEST_RPC, null, makeRPCInput("foo"));
//            rpcBroker1.tell(invokeMsg, getRef());
//
//            probeReg1.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
//            probeReg1.reply(new akka.actor.Status.Failure(new TestException()));
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", TestException.class, failure.cause().getClass());
//        }};
//    }
//
//    @Test
//    public void testExecuteRpc() {
//        new JavaTestKit(node1) {{
//
//            String xml = "<input xmlns=\"urn:test\"><input-data>foo</input-data></input>";
//
//            CompositeNode invokeRpcResult = makeRPCOutput("bar");
//            RpcResult<CompositeNode> rpcResult =
//                               RpcResultBuilder.<CompositeNode>success(invokeRpcResult).build();
//            ArgumentCaptor<CompositeNode> inputCaptor = new ArgumentCaptor<>();
//            when(brokerSession.rpc(eq(TEST_RPC), inputCaptor.capture()))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            ExecuteRpc executeMsg = new ExecuteRpc(xml, TEST_RPC);
//
//            rpcBroker1.tell(executeMsg, getRef());
//
//            RpcResponse rpcResponse = expectMsgClass(duration("5 seconds"), RpcResponse.class);
//
//            assertCompositeNodeEquals((CompositeNode)invokeRpcResult.getValue().get(0),
//                    XmlUtils.xmlToCompositeNode(rpcResponse.getResultCompositeNode()));
//        }};
//    }
//
//    @Test
//    public void testExecuteRpcFailureWithRpcErrors() {
//        new JavaTestKit(node1) {{
//
//            String xml = "<input xmlns=\"urn:test\"><input-data>foo</input-data></input>";
//
//            RpcResult<CompositeNode> rpcResult = RpcResultBuilder.<CompositeNode>failed()
//                    .withError(ErrorType.RPC, "tag1", "error", "appTag1", "info1",
//                            new Exception("mock"))
//                    .withWarning(ErrorType.PROTOCOL, "tag2", "warning", "appTag2", "info2", null)
//                    .build();
//            when(brokerSession.rpc(eq(TEST_RPC), any(CompositeNode.class)))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            ExecuteRpc executeMsg = new ExecuteRpc(xml, TEST_RPC);
//
//            rpcBroker1.tell(executeMsg, getRef());
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", RpcErrorsException.class, failure.cause().getClass());
//
//            RpcErrorsException errorsEx = (RpcErrorsException)failure.cause();
//            List<RpcError> rpcErrors = Lists.newArrayList(errorsEx.getRpcErrors());
//            assertEquals("RpcErrors count", 2, rpcErrors.size());
//            assertRpcErrorEquals(rpcErrors.get(0), ErrorSeverity.ERROR, ErrorType.RPC, "tag1",
//                    "error", "appTag1", "info1", "mock");
//            assertRpcErrorEquals(rpcErrors.get(1), ErrorSeverity.WARNING, ErrorType.PROTOCOL, "tag2",
//                    "warning", "appTag2", "info2", null);
//        }};
//    }
//
//    @Test
//    public void testExecuteRpcFailureWithNoRpcErrors() {
//        new JavaTestKit(node1) {{
//
//            String xml = "<input xmlns=\"urn:test\"><input-data>foo</input-data></input>";
//
//            RpcResult<CompositeNode> rpcResult = RpcResultBuilder.<CompositeNode>failed().build();
//            when(brokerSession.rpc(eq(TEST_RPC), any(CompositeNode.class)))
//                    .thenReturn(Futures.immediateFuture(rpcResult));
//
//            ExecuteRpc executeMsg = new ExecuteRpc(xml, TEST_RPC);
//
//            rpcBroker1.tell(executeMsg, getRef());
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", RpcErrorsException.class, failure.cause().getClass());
//
//            RpcErrorsException errorsEx = (RpcErrorsException)failure.cause();
//            List<RpcError> rpcErrors = Lists.newArrayList(errorsEx.getRpcErrors());
//            assertEquals("RpcErrors count", 1, rpcErrors.size());
//            assertRpcErrorEquals(rpcErrors.get(0), ErrorSeverity.ERROR, ErrorType.RPC,
//                    "operation-failed", "failed", null, null, null);
//        }};
//    }
//
//    @Test
//    public void testExecuteRpcFailureWithException() {
//        new JavaTestKit(node1) {{
//
//            String xml = "<input xmlns=\"urn:test\"><input-data>foo</input-data></input>";
//
//            when(brokerSession.rpc(eq(TEST_RPC), any(CompositeNode.class)))
//                    .thenReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(
//                            new TestException()));
//
//            ExecuteRpc executeMsg = new ExecuteRpc(xml, TEST_RPC);
//
//            rpcBroker1.tell(executeMsg, getRef());
//
//            akka.actor.Status.Failure failure = expectMsgClass(duration("5 seconds"),
//                    akka.actor.Status.Failure.class);
//
//            assertEquals("failure.cause()", TestException.class, failure.cause().getClass());
//        }};
//    }
}
