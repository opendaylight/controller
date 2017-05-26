/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.benchmark;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class InMemoryBrokerWriteTransactionBenchmark extends AbstractInMemoryBrokerWriteTransactionBenchmark {
    private ListeningExecutorService executor;

    @Setup(Level.Trial)
    @Override
    public void setUp() throws Exception {
        ListeningExecutorService dsExec = MoreExecutors.sameThreadExecutor();
        executor = MoreExecutors.listeningDecorator(
            MoreExecutors.getExitingExecutorService((ThreadPoolExecutor)Executors.newFixedThreadPool(1), 1L, TimeUnit.SECONDS));

        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", dsExec);
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", dsExec);
        Map<LogicalDatastoreType, DOMStore> datastores = ImmutableMap.of(
            LogicalDatastoreType.OPERATIONAL, (DOMStore)operStore,
            LogicalDatastoreType.CONFIGURATION, configStore);

        domBroker = new SerializedDOMDataBroker(datastores, executor);
        schemaContext = BenchmarkModel.createTestContext();
        configStore.onGlobalContextUpdated(schemaContext);
        operStore.onGlobalContextUpdated(schemaContext);
        initTestNode();
    }

    @Override
    public void tearDown() {
        domBroker.close();
        executor.shutdown();
    }
}
