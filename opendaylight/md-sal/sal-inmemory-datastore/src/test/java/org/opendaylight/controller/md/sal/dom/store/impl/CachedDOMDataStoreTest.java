/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CachedDOMDataStoreTest {

    private SchemaContext schemaContext;
    private InMemoryDOMDataStore domStore;
    private CachedInMemoryDataStoreDecorator cachedDomStore;

    public static final YangInstanceIdentifier OUTER_LIST_1_PATH = TestModel.OUTER_LIST_PATH.node(
            new YangInstanceIdentifier.NodeIdentifierWithPredicates(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1 ));

    public static final YangInstanceIdentifier OUTER_LIST_2_PATH = TestModel.OUTER_LIST_PATH.node(
            new YangInstanceIdentifier.NodeIdentifierWithPredicates(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2 ));

    @Before
    public void setupStore() {
        domStore = new InMemoryDOMDataStore("TEST", MoreExecutors.sameThreadExecutor(),
                MoreExecutors.sameThreadExecutor());
        cachedDomStore = new CachedInMemoryDataStoreDecorator(domStore, TestModel.TEST_PATH);
        schemaContext = TestModel.createTestContext();
        cachedDomStore.onGlobalContextUpdated(schemaContext);
    }

    // when cached data store is not used, two identical normalized nodes in tree are different objects in memory
    @Test
    public void objectNotReusedInVanillaDOMDatastoreTest() throws IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {

        DOMStoreWriteTransaction writeTx = domStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);

        ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).build();

        MapNode outerList1 = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                        TestModel.ID_QNAME, 1)).build();

        MapEntryNode outerList2 = ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                TestModel.ID_QNAME, 2);

        MapNode innnerList1 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        MapNode innnerList2 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        writeTx.write(TestModel.TEST_PATH, containerNode);
        writeTx.write(TestModel.OUTER_LIST_PATH, outerList1);
        writeTx.write(OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME), innnerList1);
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        assertThreePhaseCommit(cohort);

        DOMStoreWriteTransaction writeTx2 = cachedDomStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);
        writeTx2.write(OUTER_LIST_2_PATH, outerList2);
        writeTx2.write(OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME), innnerList2);
        DOMStoreThreePhaseCommitCohort cohort2 = writeTx2.ready();
        assertThreePhaseCommit(cohort2);


        Optional<NormalizedNode<?, ?>> afterCommitInnerList1 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        Optional<NormalizedNode<?, ?>> afterCommitInnerList2 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        assertNotSame(afterCommitInnerList1.get(), afterCommitInnerList2.get());
    }

    // when cached data store is used, two identical normalized nodes in tree are references to the same object in memory
    @Test
    public void objectReusedInCachedDOMDatastoreTest() throws IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {

        DOMStoreWriteTransaction writeTx = cachedDomStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);

        ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).build();

        MapNode outerList1 = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                        TestModel.ID_QNAME, 1)).build();

        MapEntryNode outerList2 = ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                TestModel.ID_QNAME, 2);

        MapNode innnerList1 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        MapNode innnerList2 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        writeTx.write(TestModel.TEST_PATH, containerNode);
        writeTx.write(TestModel.OUTER_LIST_PATH, outerList1);
        writeTx.write(OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME), innnerList1);
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        assertThreePhaseCommit(cohort);

        DOMStoreWriteTransaction writeTx2 = cachedDomStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);
        writeTx2.write(OUTER_LIST_2_PATH, outerList2);
        writeTx2.write(OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME), innnerList2);
        DOMStoreThreePhaseCommitCohort cohort2 = writeTx2.ready();
        assertThreePhaseCommit(cohort2);

        Optional<NormalizedNode<?, ?>> afterCommitInnerList1 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        Optional<NormalizedNode<?, ?>> afterCommitInnerList2 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        assertSame(afterCommitInnerList1.get(), afterCommitInnerList2.get());
    }

    // test that there is new object created when one of the shared versions of new
    @Test
    public void objectReferenceDOMDatastoreTest() throws IllegalAccessException, NoSuchFieldException, ExecutionException, InterruptedException {
        DOMStoreWriteTransaction writeTx = cachedDomStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);

        ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).build();

        MapNode outerList1 = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                        TestModel.ID_QNAME, 1)).build();

        MapEntryNode outerList2 = ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME,
                TestModel.ID_QNAME, 2);

        MapNode innnerList1 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        MapNode innnerList2 = ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry( TestModel.INNER_LIST_QNAME,
                        TestModel.NAME_QNAME, "one" )).build();

        LeafNode innerListLeafValue = ImmutableNodes.leafNode(TestModel.VALUE_QNAME, "value");

        writeTx.write(TestModel.TEST_PATH, containerNode);
        writeTx.write(TestModel.OUTER_LIST_PATH, outerList1);
        writeTx.write(OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME), innnerList1);
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        assertThreePhaseCommit(cohort);

        DOMStoreWriteTransaction writeTx2 = cachedDomStore.newWriteOnlyTransaction();
        assertNotNull(writeTx);
        writeTx2.write(OUTER_LIST_2_PATH, outerList2);
        writeTx2.write(OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME), innnerList2);
        DOMStoreThreePhaseCommitCohort cohort2 = writeTx2.ready();
        assertThreePhaseCommit(cohort2);

        Optional<NormalizedNode<?, ?>> afterCommitInnerList1 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        Optional<NormalizedNode<?, ?>> afterCommitInnerList2 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        assertSame(afterCommitInnerList1.get(), afterCommitInnerList2.get());

        DOMStoreWriteTransaction writeTx3 = cachedDomStore.newWriteOnlyTransaction();

        writeTx3.write(OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME)
                .node(new YangInstanceIdentifier.NodeIdentifierWithPredicates(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, "one"))
                .node(TestModel.VALUE_QNAME), innerListLeafValue);

        DOMStoreThreePhaseCommitCohort cohort3 = writeTx3.ready();
        assertThreePhaseCommit(cohort3);

        afterCommitInnerList1 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_1_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        afterCommitInnerList2 = cachedDomStore.newReadOnlyTransaction().read(
                OUTER_LIST_2_PATH.node(TestModel.INNER_LIST_QNAME)).get();

        assertNotSame(afterCommitInnerList1.get(), afterCommitInnerList2.get());
        assertNotEquals(afterCommitInnerList1.get(), afterCommitInnerList2.get());
    }

    private static void assertThreePhaseCommit(final DOMStoreThreePhaseCommitCohort cohort)
            throws InterruptedException, ExecutionException {
        assertTrue(cohort.canCommit().get().booleanValue());
        cohort.preCommit().get();
        cohort.commit().get();
    }

    private static Optional<NormalizedNode<?, ?>> assertTestContainerWrite(final DOMStoreReadWriteTransaction writeTx)
            throws InterruptedException, ExecutionException {
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        return assertTestContainerExists(writeTx);
    }

    /**
     * Reads /test from readTx Read should return container.
     */
    private static Optional<NormalizedNode<?, ?>> assertTestContainerExists(final DOMStoreReadTransaction readTx)
            throws InterruptedException, ExecutionException {

        ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = readTx.read(TestModel.TEST_PATH);
        assertTrue(writeTxContainer.get().isPresent());
        return writeTxContainer.get();
    }

}
