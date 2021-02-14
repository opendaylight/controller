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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.mdsal.dom.api.DOMActionException;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

/**
 * Unit tests for RemoteRpcImplementation.
 *
 * @author Thomas Pantelis
 */
public class RemoteOpsImplementationTest extends AbstractOpsTest {

    /**
     * This test method invokes and executes the remote rpc.
     */
    @Test
    public void testInvokeRpc() throws Exception {
        final ContainerNode rpcOutput = makeRPCOutput("bar");
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        final NormalizedNode invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode> inputCaptor =
                ArgumentCaptor.forClass(NormalizedNode.class);

        doReturn(FluentFutures.immediateFluentFuture(rpcResult)).when(domRpcService2)
            .invokeRpc(eq(TEST_RPC), inputCaptor.capture());

        final ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        final DOMRpcResult result = frontEndFuture.get(5, TimeUnit.SECONDS);
        assertEquals(rpcOutput, result.getResult());
    }

    /**
     * This test method invokes and executes the remote action.
     */
    @Test
    public void testInvokeAction() throws Exception {
        final ContainerNode actionOutput = makeRPCOutput("bar");
        final DOMActionResult actionResult = new SimpleDOMActionResult(actionOutput, Collections.emptyList());
        final NormalizedNode invokeActionInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<ContainerNode> inputCaptor =
                ArgumentCaptor.forClass(ContainerNode.class);
        doReturn(FluentFutures.immediateFluentFuture(actionResult)).when(domActionService2).invokeAction(
                eq(TEST_RPC_TYPE), eq(TEST_DATA_TREE_ID), inputCaptor.capture());
        final ListenableFuture<DOMActionResult> frontEndFuture = remoteActionImpl1.invokeAction(TEST_RPC_TYPE,
                TEST_DATA_TREE_ID, (ContainerNode) invokeActionInput);
        assertTrue(frontEndFuture instanceof RemoteDOMActionFuture);
        final DOMActionResult result = frontEndFuture.get(5, TimeUnit.SECONDS);
        assertEquals(actionOutput, result.getOutput().get());

    }

    /**
     * This test method invokes and executes the remote rpc.
     */
    @Test
    public void testInvokeRpcWithNullInput() throws Exception {
        final ContainerNode rpcOutput = makeRPCOutput("bar");
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        doReturn(FluentFutures.immediateFluentFuture(rpcResult)).when(domRpcService2)
            .invokeRpc(eq(TEST_RPC), inputCaptor.capture());

        ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, null);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        final DOMRpcResult result = frontEndFuture.get(5, TimeUnit.SECONDS);
        assertEquals(rpcOutput, result.getResult());
    }

    /**
     * This test method invokes and executes the remote action.
     */
    @Test
    public void testInvokeActionWithNullInput() throws Exception {
        final ContainerNode actionOutput = makeRPCOutput("bar");
        final DOMActionResult actionResult = new SimpleDOMActionResult(actionOutput);

        @SuppressWarnings({"unchecked", "rawtypes"})
            final ArgumentCaptor<ContainerNode> inputCaptor =
                  ArgumentCaptor.forClass(ContainerNode.class);
        doReturn(FluentFutures.immediateFluentFuture(actionResult)).when(domActionService2).invokeAction(
                eq(TEST_RPC_TYPE), eq(TEST_DATA_TREE_ID), inputCaptor.capture());

        ListenableFuture<DOMActionResult> frontEndFuture = remoteActionImpl1.invokeAction(TEST_RPC_TYPE,
                TEST_DATA_TREE_ID, actionOutput);
        assertTrue(frontEndFuture instanceof RemoteDOMActionFuture);

        final DOMActionResult result = frontEndFuture.get(5, TimeUnit.SECONDS);
        assertEquals(actionOutput, result.getOutput().get());
    }

    /**
     * This test method invokes and executes the remote rpc.
     */
    @Test
    public void testInvokeRpcWithNoOutput() throws Exception {
        final ContainerNode rpcOutput = null;
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(rpcOutput);

        final NormalizedNode invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        doReturn(FluentFutures.immediateFluentFuture(rpcResult)).when(domRpcService2)
            .invokeRpc(eq(TEST_RPC), inputCaptor.capture());

        final ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        final DOMRpcResult result = frontEndFuture.get(5, TimeUnit.SECONDS);
        assertNull(result.getResult());
    }

    /**
     * This test method invokes and executes the remote rpc.
     */
    @SuppressWarnings({"checkstyle:AvoidHidingCauseException", "checkstyle:IllegalThrows"})
    @Test(expected = DOMRpcException.class)
    public void testInvokeRpcWithRemoteFailedFuture() throws Throwable {
        final NormalizedNode invokeRpcInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<NormalizedNode> inputCaptor =
                (ArgumentCaptor) ArgumentCaptor.forClass(NormalizedNode.class);

        when(domRpcService2.invokeRpc(eq(TEST_RPC), inputCaptor.capture())).thenReturn(
                FluentFutures.immediateFailedFluentFuture(new RemoteDOMRpcException("Test Exception", null)));

        final ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        try {
            frontEndFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    /**
     * This test method invokes and executes the remote rpc.
     */
    @SuppressWarnings({"checkstyle:AvoidHidingCauseException", "checkstyle:IllegalThrows"})
    @Test(expected = DOMActionException.class)
    public void testInvokeActionWithRemoteFailedFuture() throws Throwable {
        final ContainerNode invokeActionInput = makeRPCInput("foo");
        @SuppressWarnings({"unchecked", "rawtypes"})
        final ArgumentCaptor<ContainerNode> inputCaptor =
                ArgumentCaptor.forClass(ContainerNode.class);

        when(domActionService2.invokeAction(eq(TEST_RPC_TYPE), eq(TEST_DATA_TREE_ID),
                inputCaptor.capture())).thenReturn(FluentFutures.immediateFailedFluentFuture(
                        new RemoteDOMRpcException("Test Exception", null)));

        final ListenableFuture<DOMActionResult> frontEndFuture = remoteActionImpl1.invokeAction(TEST_RPC_TYPE,
                TEST_DATA_TREE_ID, invokeActionInput);
        assertTrue(frontEndFuture instanceof RemoteDOMActionFuture);

        try {
            frontEndFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    /**
     * This test method invokes and tests exceptions when akka timeout occured
     * Currently ignored since this test with current config takes around 15 seconds to complete.
     */
    @Ignore
    @Test(expected = RemoteDOMRpcException.class)
    public void testInvokeRpcWithAkkaTimeoutException() throws Exception {
        final NormalizedNode invokeRpcInput = makeRPCInput("foo");
        final ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        frontEndFuture.get(20, TimeUnit.SECONDS);
    }

    /**
     * This test method invokes remote rpc and lookup failed
     * with runtime exception.
     */
    @Test(expected = DOMRpcException.class)
    @SuppressWarnings({"checkstyle:AvoidHidingCauseException", "checkstyle:IllegalThrows"})
    public void testInvokeRpcWithLookupException() throws Throwable {
        final NormalizedNode invokeRpcInput = makeRPCInput("foo");

        doThrow(new RuntimeException("test")).when(domRpcService2).invokeRpc(any(QName.class),
            any(NormalizedNode.class));

        final ListenableFuture<DOMRpcResult> frontEndFuture = remoteRpcImpl1.invokeRpc(TEST_RPC_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMRpcFuture);

        try {
            frontEndFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    /**
     * This test method invokes remote rpc and lookup failed
     * with runtime exception.
     */
    @Test(expected = DOMActionException.class)
    @SuppressWarnings({"checkstyle:AvoidHidingCauseException", "checkstyle:IllegalThrows"})
    public void testInvokeActionWithLookupException() throws Throwable {
        final ContainerNode invokeRpcInput = makeRPCInput("foo");

        doThrow(new RuntimeException("test")).when(domActionService2).invokeAction(any(Absolute.class),
                any(DOMDataTreeIdentifier.class), any(ContainerNode.class));

        final ListenableFuture<DOMActionResult> frontEndFuture = remoteActionImpl1.invokeAction(TEST_RPC_TYPE,
                TEST_DATA_TREE_ID, invokeRpcInput);
        assertTrue(frontEndFuture instanceof RemoteDOMActionFuture);

        try {
            frontEndFuture.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
