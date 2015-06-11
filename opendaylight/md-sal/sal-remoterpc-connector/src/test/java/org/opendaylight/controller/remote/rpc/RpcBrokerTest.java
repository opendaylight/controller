/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import akka.actor.Status.Failure;
import akka.testkit.JavaTestKit;
import com.google.common.util.concurrent.Futures;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.remote.rpc.messages.ExecuteRpc;
import org.opendaylight.controller.remote.rpc.messages.RpcResponse;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class RpcBrokerTest extends AbstractRpcTest {

    @Test
    public void testExecuteRpc() {
        new JavaTestKit(node1) {
            {

                final ContainerNode invokeRpcResult = makeRPCOutput("bar");
                final DOMRpcResult rpcResult = new DefaultDOMRpcResult(invokeRpcResult);
                when(domRpcService1.invokeRpc(eq(TEST_RPC_TYPE), Mockito.<NormalizedNode<?, ?>>any())).thenReturn(
                        Futures.<DOMRpcResult, DOMRpcException>immediateCheckedFuture(rpcResult));

                final ExecuteRpc executeMsg = ExecuteRpc.from(TEST_RPC_ID, null);

                rpcBroker1.tell(executeMsg, getRef());

                final RpcResponse rpcResponse = expectMsgClass(duration("5 seconds"), RpcResponse.class);

                assertEquals(rpcResult.getResult(),
                        NormalizedNodeSerializer.deSerialize(rpcResponse.getResultNormalizedNode()));
            }
        };
    }

    @Test
    public void testExecuteRpcFailureWithException() {

        new JavaTestKit(node1) {
            {

                when(domRpcService1.invokeRpc(eq(TEST_RPC_TYPE), Mockito.<NormalizedNode<?, ?>>any()))
                        .thenReturn(
                                Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException(
                                        "NOT FOUND")));

                final ExecuteRpc executeMsg = ExecuteRpc.from(TEST_RPC_ID, null);

                rpcBroker1.tell(executeMsg, getRef());

                final Failure rpcResponse = expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);

                Assert.assertTrue(rpcResponse.cause() instanceof DOMRpcException);
            }
        };

    }

}
