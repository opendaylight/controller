/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Benchmark for testing of performance of write operations for
 * InMemoryDataStore. The instance of benchmark creates InMemoryDataStore with
 * Data Change Listener Executor Service as Same Thread Executor and DOM Store
 * Executor Service as Same Thread Executor.
 *
 * @author Lukas Sedlak
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class InMemoryDataStoreWriteTransactionBenchmark extends AbstractInMemoryDatastoreWriteTransactionBenchmark {

    @Override
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        domStore = new InMemoryDOMDataStore("SINGLE_THREADED_DS_BENCHMARK", Executors.newSingleThreadExecutor());
        schemaContext = BenchmarkModel.createTestContext();
        domStore.onGlobalContextUpdated(schemaContext);
        initTestNode();
    }

    @Override
    @TearDown
    public void tearDown() {
        schemaContext = null;
        domStore = null;
    }
}
