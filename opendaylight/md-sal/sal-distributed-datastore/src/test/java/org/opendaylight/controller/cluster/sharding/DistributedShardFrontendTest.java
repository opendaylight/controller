/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

@Deprecated(forRemoval = true)
public class DistributedShardFrontendTest {

    private static final DOMDataTreeIdentifier ROOT =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty());
    private static final ListenableFuture<Object> SUCCESS_FUTURE = Futures.immediateFuture(null);

    private ShardedDOMDataTree shardedDOMDataTree;

    private DataStoreClient client;
    private ClientLocalHistory clientHistory;
    private ClientTransaction clientTransaction;
    private DOMDataTreeWriteCursor cursor;

    private static final YangInstanceIdentifier OUTER_LIST_YID = TestModel.OUTER_LIST_PATH.node(
            NodeIdentifierWithPredicates.of(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1));
    private static final DOMDataTreeIdentifier OUTER_LIST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, OUTER_LIST_YID);

    @Captor
    private ArgumentCaptor<PathArgument> pathArgumentCaptor;
    @Captor
    private ArgumentCaptor<NormalizedNode<?, ?>> nodeCaptor;

    private DOMStoreThreePhaseCommitCohort commitCohort;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        shardedDOMDataTree = new ShardedDOMDataTree();
        client = mock(DataStoreClient.class);
        cursor = mock(DOMDataTreeWriteCursor.class);
        clientTransaction = mock(ClientTransaction.class);
        clientHistory = mock(ClientLocalHistory.class);
        commitCohort = mock(DOMStoreThreePhaseCommitCohort.class);

        doReturn(SUCCESS_FUTURE).when(commitCohort).canCommit();
        doReturn(SUCCESS_FUTURE).when(commitCohort).preCommit();
        doReturn(SUCCESS_FUTURE).when(commitCohort).commit();
        doReturn(SUCCESS_FUTURE).when(commitCohort).abort();

        doReturn(clientTransaction).when(client).createTransaction();
        doReturn(clientTransaction).when(clientHistory).createTransaction();
        doNothing().when(clientHistory).close();

        doNothing().when(client).close();
        doReturn(clientHistory).when(client).createLocalHistory();

        doReturn(cursor).when(clientTransaction).openCursor();
        doNothing().when(cursor).close();
        doNothing().when(cursor).write(any(), any());
        doNothing().when(cursor).merge(any(), any());
        doNothing().when(cursor).delete(any());

        doReturn(commitCohort).when(clientTransaction).ready();
    }

    @Test
    public void testClientTransaction() throws Exception {
        final DistributedDataStore distributedDataStore = mock(DistributedDataStore.class);
        final ActorUtils context = mock(ActorUtils.class);
        doReturn(context).when(distributedDataStore).getActorUtils();
        doReturn(SchemaContextHelper.full()).when(context).getSchemaContext();

        final DistributedShardFrontend rootShard = new DistributedShardFrontend(distributedDataStore, client, ROOT);

        try (DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singletonList(ROOT))) {
            shardedDOMDataTree.registerDataTreeShard(ROOT, rootShard, producer);
        }

        final DataStoreClient outerListClient = mock(DataStoreClient.class);
        final ClientTransaction outerListClientTransaction = mock(ClientTransaction.class);
        final ClientLocalHistory outerListClientHistory = mock(ClientLocalHistory.class);
        final DOMDataTreeWriteCursor outerListCursor = mock(DOMDataTreeWriteCursor.class);

        doNothing().when(outerListCursor).close();
        doNothing().when(outerListCursor).write(any(), any());
        doNothing().when(outerListCursor).merge(any(), any());
        doNothing().when(outerListCursor).delete(any());

        doReturn(outerListCursor).when(outerListClientTransaction).openCursor();
        doReturn(outerListClientTransaction).when(outerListClient).createTransaction();
        doReturn(outerListClientHistory).when(outerListClient).createLocalHistory();
        doReturn(outerListClientTransaction).when(outerListClientHistory).createTransaction();

        doReturn(commitCohort).when(outerListClientTransaction).ready();

        doNothing().when(outerListClientHistory).close();
        doNothing().when(outerListClient).close();

        final DistributedShardFrontend outerListShard = new DistributedShardFrontend(
                distributedDataStore, outerListClient, OUTER_LIST_ID);
        try (DOMDataTreeProducer producer =
                     shardedDOMDataTree.createProducer(Collections.singletonList(OUTER_LIST_ID))) {
            shardedDOMDataTree.registerDataTreeShard(OUTER_LIST_ID, outerListShard, producer);
        }

        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singletonList(ROOT));
        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(false);
        final DOMDataTreeWriteCursor txCursor = tx.createCursor(ROOT);

        assertNotNull(txCursor);
        txCursor.write(TestModel.TEST_PATH.getLastPathArgument(), createCrossShardContainer());

        //check the lower shard got the correct modification
        verify(outerListCursor, times(2)).write(pathArgumentCaptor.capture(), nodeCaptor.capture());

        final List<PathArgument> capturedArgs = pathArgumentCaptor.getAllValues();
        assertEquals(2, capturedArgs.size());
        assertThat(capturedArgs,
            hasItems(new NodeIdentifier(TestModel.ID_QNAME), new NodeIdentifier(TestModel.INNER_LIST_QNAME)));

        final List<NormalizedNode<?, ?>> capturedValues = nodeCaptor.getAllValues();
        assertEquals(2, capturedValues.size());
        assertThat(capturedValues,
            hasItems(ImmutableNodes.leafNode(TestModel.ID_QNAME, 1), createInnerMapNode(1)));

        txCursor.close();
        tx.commit().get();

        verify(commitCohort, times(2)).canCommit();
        verify(commitCohort, times(2)).preCommit();
        verify(commitCohort, times(2)).commit();
    }

    private static MapNode createInnerMapNode(final int id) {
        final MapEntryNode listEntry = ImmutableNodes
                .mapEntryBuilder(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "name-" + id)
                .withChild(ImmutableNodes.leafNode(TestModel.NAME_QNAME, "name-" + id))
                .withChild(ImmutableNodes.leafNode(TestModel.VALUE_QNAME, "value-" + id))
                .build();

        return ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME).withChild(listEntry).build();
    }

    private static ContainerNode createCrossShardContainer() {

        final MapEntryNode outerListEntry1 =
                ImmutableNodes.mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1)
                        .withChild(createInnerMapNode(1))
                        .build();
        final MapEntryNode outerListEntry2 =
                ImmutableNodes.mapEntryBuilder(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2)
                        .withChild(createInnerMapNode(2))
                        .build();

        final MapNode outerList = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .withChild(outerListEntry1)
                .withChild(outerListEntry2)
                .build();

        final ContainerNode testContainer = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
                .withChild(outerList)
                .build();

        return testContainer;
    }
}
