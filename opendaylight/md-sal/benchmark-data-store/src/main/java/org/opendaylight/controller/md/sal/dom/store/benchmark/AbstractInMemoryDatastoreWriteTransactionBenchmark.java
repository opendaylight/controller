/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Lukas Sedlak
 */
public abstract class AbstractInMemoryDatastoreWriteTransactionBenchmark extends AbstractInMemoryWriteTransactionBenchmark {

    protected InMemoryDOMDataStore domStore;

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
