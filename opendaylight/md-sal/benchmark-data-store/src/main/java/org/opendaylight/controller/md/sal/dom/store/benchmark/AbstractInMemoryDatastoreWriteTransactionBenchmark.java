/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Lukas Sedlak <lsedlak@cisco.com>
 */
public abstract class AbstractInMemoryDatastoreWriteTransactionBenchmark {

    private static final int WARMUP_ITERATIONS = 20;
    private static final int MEASUREMENT_ITERATIONS = 20;

    private static final int OUTER_LIST_100K = 100000;
    private static final int OUTER_LIST_50K = 50000;
    private static final int OUTER_LIST_10K = 10000;

    private static final YangInstanceIdentifier[] OUTER_LIST_100K_PATHS = initOuterListPaths(OUTER_LIST_100K);
    private static final YangInstanceIdentifier[] OUTER_LIST_50K_PATHS = initOuterListPaths(OUTER_LIST_50K);
    private static final YangInstanceIdentifier[] OUTER_LIST_10K_PATHS = initOuterListPaths(OUTER_LIST_10K);

    private static YangInstanceIdentifier[] initOuterListPaths(final int outerListPathsCount) {
        final YangInstanceIdentifier[] paths = new YangInstanceIdentifier[outerListPathsCount];

        for (int outerListKey = 0; outerListKey < outerListPathsCount; ++outerListKey) {
            paths[outerListKey] = YangInstanceIdentifier.builder(BenchmarkModel.OUTER_LIST_PATH)
                .nodeWithKey(BenchmarkModel.OUTER_LIST_QNAME, BenchmarkModel.ID_QNAME, outerListKey)
                .build();
        }
        return paths;
    }

    private static final MapNode ONE_ITEM_INNER_LIST = initInnerListItems(1);
    private static final MapNode TWO_ITEM_INNER_LIST = initInnerListItems(2);
    private static final MapNode TEN_ITEM_INNER_LIST = initInnerListItems(10);

    private static MapNode initInnerListItems(final int count) {
        final CollectionNodeBuilder<MapEntryNode, MapNode> mapEntryBuilder = ImmutableNodes
            .mapNodeBuilder(BenchmarkModel.INNER_LIST_QNAME);

        for (int i = 1; i <= count; ++i) {
            mapEntryBuilder
                .withChild(ImmutableNodes.mapEntry(BenchmarkModel.INNER_LIST_QNAME, BenchmarkModel.NAME_QNAME, i));
        }
        return mapEntryBuilder.build();
    }

    private static final NormalizedNode<?, ?>[] OUTER_LIST_ONE_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_100K, ONE_ITEM_INNER_LIST);
    private static final NormalizedNode<?, ?>[] OUTER_LIST_TWO_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_50K, TWO_ITEM_INNER_LIST);
    private static final NormalizedNode<?, ?>[] OUTER_LIST_TEN_ITEM_INNER_LIST = initOuterListItems(OUTER_LIST_10K, TEN_ITEM_INNER_LIST);

    private static NormalizedNode<?,?>[] initOuterListItems(int outerListItemsCount, MapNode innerList) {
        final NormalizedNode<?,?>[] outerListItems = new NormalizedNode[outerListItemsCount];

        for (int i = 0; i < outerListItemsCount; ++i) {
            int outerListKey = i;
            outerListItems[i] = ImmutableNodes.mapEntryBuilder(BenchmarkModel.OUTER_LIST_QNAME, BenchmarkModel.ID_QNAME, outerListKey)
                .withChild(innerList).build();
        }
        return outerListItems;
    }

    protected SchemaContext schemaContext;
    protected InMemoryDOMDataStore domStore;

    abstract public void setUp() throws Exception;

    abstract public void tearDown();

    protected void initTestNode() throws Exception {
        final YangInstanceIdentifier testPath = YangInstanceIdentifier.builder(BenchmarkModel.TEST_PATH)
            .build();
        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        writeTx.write(testPath, provideOuterListNode());

        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit().get();
    }

    private DataContainerChild<?, ?> provideOuterListNode() {
        return ImmutableContainerNodeBuilder
            .create()
            .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(BenchmarkModel.TEST_QNAME))
            .withChild(
                ImmutableNodes.mapNodeBuilder(BenchmarkModel.OUTER_LIST_QNAME)
                    .build()).build();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write100KSingleNodeWithOneInnerItemInOneCommitBenchmark() throws Exception {
        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_100K; ++outerListKey) {
            writeTx.write(OUTER_LIST_100K_PATHS[outerListKey], OUTER_LIST_ONE_ITEM_INNER_LIST[outerListKey]);
        }
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write100KSingleNodeWithOneInnerItemInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_100K; ++outerListKey) {
            DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
            writeTx.write(OUTER_LIST_100K_PATHS[outerListKey], OUTER_LIST_ONE_ITEM_INNER_LIST[outerListKey]);

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
            cohort.canCommit().get();
            cohort.preCommit().get();
            cohort.commit().get();
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write50KSingleNodeWithTwoInnerItemsInOneCommitBenchmark() throws Exception {
        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_50K; ++outerListKey) {
            writeTx.write(OUTER_LIST_50K_PATHS[outerListKey], OUTER_LIST_TWO_ITEM_INNER_LIST[outerListKey]);
        }
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write50KSingleNodeWithTwoInnerItemsInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_50K; ++outerListKey) {
            DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
            writeTx.write(OUTER_LIST_50K_PATHS[outerListKey], OUTER_LIST_TWO_ITEM_INNER_LIST[outerListKey]);
            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
            cohort.canCommit().get();
            cohort.preCommit().get();
            cohort.commit().get();
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write10KSingleNodeWithTenInnerItemsInOneCommitBenchmark() throws Exception {
        DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_10K; ++outerListKey) {
            writeTx.write(OUTER_LIST_10K_PATHS[outerListKey], OUTER_LIST_TEN_ITEM_INNER_LIST[outerListKey]);
        }
        DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
        cohort.canCommit().get();
        cohort.preCommit().get();
        cohort.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write10KSingleNodeWithTenInnerItemsInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_10K; ++outerListKey) {
            DOMStoreReadWriteTransaction writeTx = domStore.newReadWriteTransaction();
            writeTx.write(OUTER_LIST_10K_PATHS[outerListKey], OUTER_LIST_TEN_ITEM_INNER_LIST[outerListKey]);
            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();
            cohort.canCommit().get();
            cohort.preCommit().get();
            cohort.commit().get();
        }
    }
}
