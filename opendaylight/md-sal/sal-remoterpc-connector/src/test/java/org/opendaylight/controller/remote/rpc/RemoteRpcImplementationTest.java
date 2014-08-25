/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import akka.testkit.JavaTestKit;

import com.google.common.util.concurrent.ListenableFuture;

/***
 * Unit tests for RemoteRpcImplementation.
 *
 * @author Thomas Pantelis
 */
public class RemoteRpcImplementationTest extends AbstractRpcTest {

    @Test
    public void testInvokeRpc() throws Exception {
        final AtomicReference<AssertionError> assertError = new AtomicReference<>();
        try {
            RemoteRpcImplementation rpcImpl = new RemoteRpcImplementation(
                    probeReg1.getRef(), schemaContext);

            final CompositeNode input = makeRPCInput("foo");
            final CompositeNode output = makeRPCOutput("bar");
            final AtomicReference<InvokeRpc> invokeRpcMsg = new AtomicReference<>();

            new Thread() {
                @Override
                public void run() {
                    try {
                        invokeRpcMsg.set(probeReg1.expectMsgClass(
                                JavaTestKit.duration("5 seconds"), InvokeRpc.class));

                        probeReg1.reply(new RpcResponse(XmlUtils.outputCompositeNodeToXml(
                                output, schemaContext)));

                    } catch(AssertionError e) {
                        assertError.set(e);
                    }
                }

            }.start();

            ListenableFuture<RpcResult<CompositeNode>> future = rpcImpl.invokeRpc(TEST_RPC, input);

            RpcResult<CompositeNode> rpcResult = future.get(5, TimeUnit.SECONDS);

            assertSuccessfulRpcResult(rpcResult, (CompositeNode)output.getValue().get(0));

            assertEquals("getRpc", TEST_RPC, invokeRpcMsg.get().getRpc());
            assertEquals("getInput", input, invokeRpcMsg.get().getInput());
        } finally {
            if(assertError.get() != null) {
                throw assertError.get();
            }
        }
    }

    @Test
    public void testInvokeRpcWithRpcErrorsException() throws Exception {
        final AtomicReference<AssertionError> assertError = new AtomicReference<>();
        try {
            RemoteRpcImplementation rpcImpl = new RemoteRpcImplementation(
                    probeReg1.getRef(), schemaContext);

            final CompositeNode input = makeRPCInput("foo");

            new Thread() {
                @Override
                public void run() {
                    try {
                        probeReg1.expectMsgClass(JavaTestKit.duration("5 seconds"), InvokeRpc.class);

                        probeReg1.reply(new akka.actor.Status.Failure(new RpcErrorsException(
                                "mock", Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC, "tag",
                                        "error", "appTag", "info", null)))));

                    } catch(AssertionError e) {
                        assertError.set(e);
                    }
                }

            }.start();

            ListenableFuture<RpcResult<CompositeNode>> future = rpcImpl.invokeRpc(TEST_RPC, input);

            RpcResult<CompositeNode> rpcResult = future.get(5, TimeUnit.SECONDS);

            assertFailedRpcResult(rpcResult, ErrorSeverity.ERROR, ErrorType.RPC, "tag",
                    "error", "appTag", "info", null);
        } finally {
            if(assertError.get() != null) {
                throw assertError.get();
            }
        }
    }

    static void assertFailedRpcResult(RpcResult<CompositeNode> rpcResult, ErrorSeverity severity,
            ErrorType errorType, String tag, String message, String applicationTag, String info,
            String causeMsg) {

        assertNotNull("RpcResult was null", rpcResult);
        assertEquals("isSuccessful", false, rpcResult.isSuccessful());
        Collection<RpcError> rpcErrors = rpcResult.getErrors();
        assertEquals("RpcErrors count", 1, rpcErrors.size());
        assertRpcErrorEquals(rpcErrors.iterator().next(), severity, errorType, tag, message,
                applicationTag, info, causeMsg);
    }

    static void assertSuccessfulRpcResult(RpcResult<CompositeNode> rpcResult,
            CompositeNode expOutput) {

        assertNotNull("RpcResult was null", rpcResult);
        assertEquals("isSuccessful", true, rpcResult.isSuccessful());
        assertCompositeNodeEquals(expOutput, rpcResult.getResult());
    }
}
