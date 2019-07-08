/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Abstract class to handle transaction benchmarks.
 *
 * @author Lukas Sedlak
 */
public abstract class AbstractInMemoryBrokerWriteTransactionBenchmark
        extends AbstractInMemoryWriteTransactionBenchmark {

    protected SerializedDOMDataBroker domBroker;

    protected void initTestNode() throws Exception {
        final YangInstanceIdentifier testPath = YangInstanceIdentifier.builder(BenchmarkModel.TEST_PATH).build();
        DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, testPath, provideOuterListNode());

        writeTx.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write100KSingleNodeWithOneInnerItemInOneCommitBenchmark() throws Exception {

        DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_100K; ++outerListKey) {
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_100K_PATHS[outerListKey],
                    OUTER_LIST_ONE_ITEM_INNER_LIST[outerListKey]);
        }

        writeTx.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write100KSingleNodeWithOneInnerItemInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_100K; ++outerListKey) {
            DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_100K_PATHS[outerListKey],
                    OUTER_LIST_ONE_ITEM_INNER_LIST[outerListKey]);

            writeTx.commit().get();
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write50KSingleNodeWithTwoInnerItemsInOneCommitBenchmark() throws Exception {
        DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_50K; ++outerListKey) {
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_50K_PATHS[outerListKey],
                    OUTER_LIST_TWO_ITEM_INNER_LIST[outerListKey]);
        }

        writeTx.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write50KSingleNodeWithTwoInnerItemsInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_50K; ++outerListKey) {
            DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_50K_PATHS[outerListKey],
                    OUTER_LIST_TWO_ITEM_INNER_LIST[outerListKey]);
            writeTx.commit().get();
        }
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write10KSingleNodeWithTenInnerItemsInOneCommitBenchmark() throws Exception {
        DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        for (int outerListKey = 0; outerListKey < OUTER_LIST_10K; ++outerListKey) {
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_10K_PATHS[outerListKey],
                    OUTER_LIST_TEN_ITEM_INNER_LIST[outerListKey]);
        }
        writeTx.commit().get();
    }

    @Benchmark
    @Warmup(iterations = WARMUP_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = MEASUREMENT_ITERATIONS, timeUnit = TimeUnit.MILLISECONDS)
    public void write10KSingleNodeWithTenInnerItemsInCommitPerWriteBenchmark() throws Exception {
        for (int outerListKey = 0; outerListKey < OUTER_LIST_10K; ++outerListKey) {
            DOMDataTreeReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, OUTER_LIST_10K_PATHS[outerListKey],
                    OUTER_LIST_TEN_ITEM_INNER_LIST[outerListKey]);
            writeTx.commit().get();
        }
    }
}
