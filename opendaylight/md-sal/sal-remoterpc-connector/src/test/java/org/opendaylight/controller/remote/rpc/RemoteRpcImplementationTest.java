/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.messages.InvokeRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.controller.xml.codec.XmlUtils;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

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
                    probeReg1.getRef(), schemaContext, getConfig());

            final CompositeNode input = makeRPCInput("foo");
            final CompositeNode output = makeRPCOutput("bar");
            final AtomicReference<InvokeRpc> invokeRpcMsg = setupInvokeRpcReply(assertError, output);

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
    public void testInvokeRpcWithIdentifier() throws Exception {
        final AtomicReference<AssertionError> assertError = new AtomicReference<>();
        try {
            RemoteRpcImplementation rpcImpl = new RemoteRpcImplementation(
                    probeReg1.getRef(), schemaContext, getConfig());

            QName instanceQName = new QName(new URI("ns"), "instance");
            YangInstanceIdentifier identifier = YangInstanceIdentifier.of(instanceQName);

            CompositeNode input = makeRPCInput("foo");
            CompositeNode output = makeRPCOutput("bar");
            final AtomicReference<InvokeRpc> invokeRpcMsg = setupInvokeRpcReply(assertError, output);

            ListenableFuture<RpcResult<CompositeNode>> future = rpcImpl.invokeRpc(
                    TEST_RPC, identifier, input);

            RpcResult<CompositeNode> rpcResult = future.get(5, TimeUnit.SECONDS);

            assertSuccessfulRpcResult(rpcResult, (CompositeNode)output.getValue().get(0));

            assertEquals("getRpc", TEST_RPC, invokeRpcMsg.get().getRpc());
            assertEquals("getInput", input, invokeRpcMsg.get().getInput());
            assertEquals("getRoute", identifier, invokeRpcMsg.get().getIdentifier());
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
                    probeReg1.getRef(), schemaContext, getConfig());

            final CompositeNode input = makeRPCInput("foo");

            setupInvokeRpcErrorReply(assertError, new RpcErrorsException(
                    "mock", Arrays.asList(RpcResultBuilder.newError(ErrorType.RPC, "tag",
                            "error", "appTag", "info", null))));

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

    @Test
    public void testInvokeRpcWithOtherException() throws Exception {
        final AtomicReference<AssertionError> assertError = new AtomicReference<>();
        try {
            RemoteRpcImplementation rpcImpl = new RemoteRpcImplementation(
                    probeReg1.getRef(), schemaContext, getConfig());

            final CompositeNode input = makeRPCInput("foo");

            setupInvokeRpcErrorReply(assertError, new TestException());

            ListenableFuture<RpcResult<CompositeNode>> future = rpcImpl.invokeRpc(TEST_RPC, input);

            RpcResult<CompositeNode> rpcResult = future.get(5, TimeUnit.SECONDS);

            assertFailedRpcResult(rpcResult, ErrorSeverity.ERROR, ErrorType.RPC, "operation-failed",
                    TestException.MESSAGE, null, null, TestException.MESSAGE);
        } finally {
            if(assertError.get() != null) {
                throw assertError.get();
            }
        }
    }

    private AtomicReference<InvokeRpc> setupInvokeRpcReply(
            final AtomicReference<AssertionError> assertError, final CompositeNode output) {
        return setupInvokeRpcReply(assertError, output, null);
    }

    private AtomicReference<InvokeRpc> setupInvokeRpcErrorReply(
            final AtomicReference<AssertionError> assertError, final Exception error) {
        return setupInvokeRpcReply(assertError, null, error);
    }

    private AtomicReference<InvokeRpc> setupInvokeRpcReply(
            final AtomicReference<AssertionError> assertError, final CompositeNode output,
            final Exception error) {
        final AtomicReference<InvokeRpc> invokeRpcMsg = new AtomicReference<>();

        new Thread() {
            @Override
            public void run() {
                try {
                    invokeRpcMsg.set(probeReg1.expectMsgClass(
                            JavaTestKit.duration("5 seconds"), InvokeRpc.class));

                    if(output != null) {
                        probeReg1.reply(new RpcResponse(XmlUtils.outputCompositeNodeToXml(
                                output, schemaContext)));
                    } else {
                        probeReg1.reply(new akka.actor.Status.Failure(error));
                    }

                } catch(AssertionError e) {
                    assertError.set(e);
                }
            }

        }.start();

        return invokeRpcMsg;
    }

    private RemoteRpcProviderConfig getConfig(){
        return new RemoteRpcProviderConfig.Builder("unit-test").build();
    }
}
