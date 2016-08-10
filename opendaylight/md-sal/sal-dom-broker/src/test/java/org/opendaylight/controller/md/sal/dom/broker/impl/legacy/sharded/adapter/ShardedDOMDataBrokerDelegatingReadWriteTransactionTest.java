/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

public class ShardedDOMDataBrokerDelegatingReadWriteTransactionTest {

    @Mock
    private DOMDataWriteTransaction writeTx;

    @Mock
    private DOMDataReadOnlyTransaction readTx;

    private ShardedDOMDataBrokerDelegatingReadWriteTransaction rwTx;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(writeTx).put(any(), any(), any());
        doNothing().when(writeTx).merge(any(), any(), any());
        doNothing().when(writeTx).delete(any(), any());
        rwTx = new ShardedDOMDataBrokerDelegatingReadWriteTransaction("TEST-TX", TestModel.createTestContext(), readTx, writeTx);
    }

    @Test(expected = NullPointerException.class)
    public void testFirstReadShouldFail() {
        rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH);
    }

    @Test
    public void testGetIdentifier() {
        assertEquals("TEST-TX", rwTx.getIdentifier());
    }

    @Test
    public void testReadWriteOperations() throws Exception {
        doReturn(Futures.immediateCheckedFuture(Optional.absent())).when(readTx)
                .read(any(), any());
        rwTx.put(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH,
                testNodeWithOuter(1, 2, 3));

        verify(writeTx).put(eq(LogicalDatastoreType.OPERATIONAL), Matchers.eq(TestModel.TEST_PATH),
                Matchers.eq(testNodeWithOuter(1, 2, 3)));
        verify(readTx).read(eq(LogicalDatastoreType.OPERATIONAL), Matchers.eq(TestModel.TEST_PATH));

        assertEquals(testNodeWithOuter(1, 2, 3),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet().get());

        rwTx.merge(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH,
                testNodeWithOuter(4, 5, 6));
        assertEquals(testNodeWithOuter(1, 2, 3, 4, 5, 6),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet().get());

        rwTx.delete(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH);

        verify(writeTx).delete(eq(LogicalDatastoreType.OPERATIONAL), Matchers.eq(TestModel.TEST_PATH));
        assertEquals(Optional.absent(),
                rwTx.read(LogicalDatastoreType.OPERATIONAL, TestModel.TEST_PATH).checkedGet());
    }

    private DataContainerChild<?, ?> outerNode(int... ids) {
        CollectionNodeBuilder<MapEntryNode, MapNode> outer = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME);
        for(int id: ids) {
            outer.addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, id));
        }

        return outer.build();
    }

    private NormalizedNode<?, ?> testNodeWithOuter(int... ids) {
        return testNodeWithOuter(outerNode(ids));
    }

    private NormalizedNode<?, ?> testNodeWithOuter(DataContainerChild<?, ?> outer) {
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).withChild(outer).build();
    }
}