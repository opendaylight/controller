/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class WriteTransactionTest extends AbstractDataServiceTest {

    private DataBroker dataBroker;

    private static final InstanceIdentifier<Nodes> NODES_PATH = InstanceIdentifier.create(Nodes.class);

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId("foo"));

    private static final InstanceIdentifier<Node> NODE_PATH = NODES_PATH.child(Node.class, NODE_KEY);

    @Override
    public void setUp() {
        super.setUp();

        dataBroker = testContext.getDataBroker();
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, NODES_PATH, new NodesBuilder().build());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, NODE_PATH, new NodeBuilder().setKey(NODE_KEY).build());
        assertEquals(TransactionStatus.COMMITED, writeTx.commit().get().getResult());
    }

}
