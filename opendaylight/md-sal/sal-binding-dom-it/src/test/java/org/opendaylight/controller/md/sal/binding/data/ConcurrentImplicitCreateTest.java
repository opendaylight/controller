/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/*
 * FIXME: THis test should be moved to sal-binding-broker and rewriten
 * to use new DataBroker API
 */
public class ConcurrentImplicitCreateTest extends AbstractDataServiceTest {

    private static final NodeKey NODE_FOO_KEY = new NodeKey(new NodeId("foo"));
    private static final NodeKey NODE_BAR_KEY = new NodeKey(new NodeId("foo"));
    private static final InstanceIdentifier<Nodes> NODES_PATH = InstanceIdentifier.builder(Nodes.class).build();
    private static final InstanceIdentifier<Node> NODE_FOO_PATH = NODES_PATH.child(Node.class, NODE_FOO_KEY);
    private static final InstanceIdentifier<Node> NODE_BAR_PATH = NODES_PATH.child(Node.class, NODE_FOO_KEY);

    @Test
    public void testConcurrentCreate() throws InterruptedException, ExecutionException {

        DataModificationTransaction fooTx = baDataService.beginTransaction();
        DataModificationTransaction barTx = baDataService.beginTransaction();

        fooTx.putOperationalData(NODE_FOO_PATH, new NodeBuilder().setKey(NODE_FOO_KEY).build());
        barTx.putOperationalData(NODE_BAR_PATH, new NodeBuilder().setKey(NODE_BAR_KEY).build());

        Future<RpcResult<TransactionStatus>> fooFuture = fooTx.commit();
        Future<RpcResult<TransactionStatus>> barFuture = barTx.commit();

        RpcResult<TransactionStatus> fooResult = fooFuture.get();
        RpcResult<TransactionStatus> barResult = barFuture.get();

        assertTrue(fooResult.isSuccessful());
        assertTrue(barResult.isSuccessful());

        assertEquals(TransactionStatus.COMMITED, fooResult.getResult());
        assertEquals(TransactionStatus.COMMITED, barResult.getResult());

    }
}
