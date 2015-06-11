/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import akka.japi.Pair;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRouters;
import org.opendaylight.controller.sal.connector.api.RpcRouter.RouteIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/***
 * Unit tests for RemoteRpcImplementation.
 *
 * @author Thomas Pantelis
 */
public class RemoteRpcImplementationTest extends AbstractRpcTest {



    @Test(expected = DOMRpcImplementationNotAvailableException.class)
    public void testInvokeRpcWithNoRemoteActor() throws Exception {
        final ContainerNode input = makeRPCInput("foo");
        final CheckedFuture<DOMRpcResult, DOMRpcException> failedFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, input);
        rpcRegistry1Probe.expectMsgClass(JavaTestKit.duration("5 seconds"), RpcRegistry.Messages.FindRouters.class);
        rpcRegistry1Probe
                .reply(new RpcRegistry.Messages.FindRoutersReply(Collections.<Pair<ActorRef, Long>>emptyList()));
        failedFuture.checkedGet(5, TimeUnit.SECONDS);
    }


    /**
     * This test method invokes and executes the remote rpc
     */
    @Test
    public void testInvokeRpc() throws Exception {
        final ContainerNode rpcOutput = makeRPCOutput("bar");
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        final NormalizedNode<?, ?> invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode<?, ?>> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        when(domRpcService2.invokeRpc(eq(TEST_RPC_TYPE), inputCaptor.capture())).thenReturn(
                Futures.<DOMRpcResult, DOMRpcException>immediateCheckedFuture(rpcResult));

        final CheckedFuture<DOMRpcResult, DOMRpcException> frontEndFuture =
                remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);
        final FindRouters findRouters = rpcRegistry1Probe.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
        final RouteIdentifier<?, ?, ?> routeIdentifier = findRouters.getRouteIdentifier();
        assertEquals("getType", TEST_RPC, routeIdentifier.getType());
        assertEquals("getRoute", TEST_PATH, routeIdentifier.getRoute());

        rpcRegistry1Probe.reply(new RpcRegistry.Messages.FindRoutersReply(Arrays.asList(new Pair<ActorRef, Long>(
                rpcBroker2, 200L))));

        final DOMRpcResult result = frontEndFuture.checkedGet(5, TimeUnit.SECONDS);
        assertEquals(rpcOutput, result.getResult());
    }


    /**
     * This test method invokes and executes the remote rpc
     */
    @Test
    public void testInvokeRpcWithNoOutput() throws Exception {
        final ContainerNode rpcOutput = null;
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        final NormalizedNode<?, ?> invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode<?, ?>> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        when(domRpcService2.invokeRpc(eq(TEST_RPC_TYPE), inputCaptor.capture())).thenReturn(
                Futures.<DOMRpcResult, DOMRpcException>immediateCheckedFuture(rpcResult));

        final CheckedFuture<DOMRpcResult, DOMRpcException> frontEndFuture =
                remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);
        final FindRouters findRouters = rpcRegistry1Probe.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
        final RouteIdentifier<?, ?, ?> routeIdentifier = findRouters.getRouteIdentifier();
        assertEquals("getType", TEST_RPC, routeIdentifier.getType());
        assertEquals("getRoute", TEST_PATH, routeIdentifier.getRoute());

        rpcRegistry1Probe.reply(new RpcRegistry.Messages.FindRoutersReply(Arrays.asList(new Pair<ActorRef, Long>(
                rpcBroker2, 200L))));

        final DOMRpcResult result = frontEndFuture.checkedGet(5, TimeUnit.SECONDS);
        assertNull(result.getResult());
    }


    /**
     * This test method invokes and executes the remote rpc
     */
    @Test(expected = DOMRpcException.class)
    public void testInvokeRpcWithRemoteFailedFuture() throws Exception {
        final ContainerNode rpcOutput = null;
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        final NormalizedNode<?, ?> invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode<?, ?>> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        when(domRpcService2.invokeRpc(eq(TEST_RPC_TYPE), inputCaptor.capture())).thenReturn(
                Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcException(
                        "Test Exception") {}));

        final CheckedFuture<DOMRpcResult, DOMRpcException> frontEndFuture =
                remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);
        final FindRouters findRouters = rpcRegistry1Probe.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
        final RouteIdentifier<?, ?, ?> routeIdentifier = findRouters.getRouteIdentifier();
        assertEquals("getType", TEST_RPC, routeIdentifier.getType());
        assertEquals("getRoute", TEST_PATH, routeIdentifier.getRoute());

        rpcRegistry1Probe.reply(new RpcRegistry.Messages.FindRoutersReply(Arrays.asList(new Pair<ActorRef, Long>(
                rpcBroker2, 200L))));
        frontEndFuture.checkedGet(5, TimeUnit.SECONDS);
    }

    /**
     * This test method invokes and executes the remote rpc
     */
    @Test(expected = DOMRpcException.class)
    public void testInvokeRpcWithRemoteRuntimeException() throws Exception {
        final NormalizedNode<?, ?> invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode<?, ?>> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        when(domRpcService2.invokeRpc(eq(TEST_RPC_TYPE), inputCaptor.capture())).thenThrow(
                IllegalArgumentException.class);

        final CheckedFuture<DOMRpcResult, DOMRpcException> frontEndFuture =
                remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);
        final FindRouters findRouters = rpcRegistry1Probe.expectMsgClass(RpcRegistry.Messages.FindRouters.class);
        final RouteIdentifier<?, ?, ?> routeIdentifier = findRouters.getRouteIdentifier();
        assertEquals("getType", TEST_RPC, routeIdentifier.getType());
        assertEquals("getRoute", TEST_PATH, routeIdentifier.getRoute());

        rpcRegistry1Probe.reply(new RpcRegistry.Messages.FindRoutersReply(Arrays.asList(new Pair<ActorRef, Long>(
                rpcBroker2, 200L))));

        frontEndFuture.checkedGet(5, TimeUnit.SECONDS);
    }


    private RemoteRpcProviderConfig getConfig() {
        return new RemoteRpcProviderConfig.Builder("unit-test").build();
    }
}
