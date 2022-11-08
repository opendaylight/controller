/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import akka.actor.Status.Failure;
import java.time.Duration;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.mdsal.dom.api.DOMRpcException;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public class OpsBrokerTest extends AbstractOpsTest {

    @Test
    public void testExecuteRpc() {
        final ContainerNode invokeRpcResult = makeRPCOutput("bar");
        final DOMRpcResult rpcResult = new DefaultDOMRpcResult(invokeRpcResult);
        doReturn(FluentFutures.immediateFluentFuture(rpcResult)).when(domRpcService1)
            .invokeRpc(eq(TEST_RPC), any());
        final ExecuteRpc executeRpc = ExecuteRpc.from(TEST_RPC_ID, null);

        rpcInvoker1.tell(executeRpc, rpcRegistry1Probe.getRef());

        final RpcResponse rpcResponse = rpcRegistry1Probe.expectMsgClass(Duration.ofSeconds(5), RpcResponse.class);

        assertEquals(rpcResult.value(), rpcResponse.getOutput());
    }

    @Test
    public void testExecuteRpcFailureWithException() {
        when(domRpcService1.invokeRpc(eq(TEST_RPC), any())).thenReturn(FluentFutures.immediateFailedFluentFuture(
            new DOMRpcImplementationNotAvailableException("NOT FOUND")));

        final ExecuteRpc executeMsg = ExecuteRpc.from(TEST_RPC_ID, null);

        rpcInvoker1.tell(executeMsg, rpcRegistry1Probe.getRef());

        final Failure rpcResponse = rpcRegistry1Probe.expectMsgClass(Duration.ofSeconds(5), Failure.class);

        assertTrue(rpcResponse.cause() instanceof DOMRpcException);
    }
}
