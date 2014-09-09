/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

/**
 *
 * @author Lukas Sedlak <lsedlak@cisco.com>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class InMemoryDataStoreWithExecutorServiceBenchmark extends AbstractInMemoryDatastoreWriteTransactionBenchmark  {

    private static final int MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE = 20;
    private static final int MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE = 1000;
    private static final int MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE = 5000;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        final String name = "DS_BENCHMARK";
        final ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
            MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE, MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE, name + "-DCL");

        final ExecutorService domStoreExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(
            MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE, "DOMStore-" + name );

        domStore = new InMemoryDOMDataStore(name, domStoreExecutor,
            dataChangeListenerExecutor);
        schemaContext = BenchmarkModel.createTestContext();
        domStore.onGlobalContextUpdated(schemaContext);
        initTestNode();
    }

    @TearDown
    public void tearDown() {
        schemaContext = null;
        domStore = null;
    }
}
