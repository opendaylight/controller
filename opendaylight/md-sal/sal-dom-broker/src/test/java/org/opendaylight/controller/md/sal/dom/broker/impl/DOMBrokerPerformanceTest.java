/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DOMBrokerPerformanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(DOMBrokerPerformanceTest.class);

    private static NormalizedNode<?, ?> outerList(final int i) {
        return ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i);
    }

    private static YangInstanceIdentifier outerListPath(final int i) {
        return YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)//
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i) //
                .build();
    }

    private SchemaContext schemaContext;
    private AbstractDOMDataBroker domBroker;

    private static <V> V measure(final String name, final Callable<V> callable) throws Exception {
        // TODO Auto-generated method stub
        LOG.debug("Measurement:{} Start", name);
        long startNano = System.nanoTime();
        try {
            return callable.call();
        } finally {
            long endNano = System.nanoTime();
            LOG.info("Measurement:\"{}\" Time:{} ms", name, (endNano - startNano) / 1000000.0d);
        }
    }

    @Before
    public void setupStore() {
        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER", MoreExecutors.newDirectExecutorService());
        InMemoryDOMDataStore configStore = new InMemoryDOMDataStore("CFG", MoreExecutors.newDirectExecutorService());
        schemaContext = TestModel.createTestContext();

        operStore.onGlobalContextUpdated(schemaContext);
        configStore.onGlobalContextUpdated(schemaContext);

        ImmutableMap<LogicalDatastoreType, DOMStore> stores = ImmutableMap.<LogicalDatastoreType, DOMStore> builder() //
                .put(CONFIGURATION, configStore) //
                .put(OPERATIONAL, operStore) //
                .build();
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
        domBroker = new SerializedDOMDataBroker(stores, executor);
    }

    @Test
    public void testPerformance() throws Exception {
        measure("Test Suite (all tests)", (Callable<Void>) () -> {
            smallTestSuite(10, 1000);
            //smallTestSuite(10, 100);
            smallTestSuite(100, 100);
            //smallTestSuite(100, 100);
            //smallTestSuite(1000, 10);
            smallTestSuite(1000, 10);
            //smallTestSuite(1000, 1000);
            return null;
        });
    }

    private void smallTestSuite(final int txNum, final int innerListWriteNum) throws Exception {
        measure("TestSuite (Txs:" + txNum + " innerWrites:" + innerListWriteNum + ")", (Callable<Void>) () -> {
            measureOneTransactionTopContainer();
            measureSeparateWritesOneLevel(txNum, innerListWriteNum);
            return null;
        });
    }

    private void measureSeparateWritesOneLevel(final int txNum, final int innerNum) throws Exception {
        final List<DOMDataReadWriteTransaction> transactions = measure("Txs:"+ txNum + " Allocate",
                () -> {
                    List<DOMDataReadWriteTransaction> builder = new ArrayList<>(txNum);
                    for (int i = 0; i < txNum; i++) {
                        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
                        builder.add(writeTx);
                    }
                    return builder;
                });
        assertEquals(txNum, transactions.size());
        measure("Txs:"+ txNum + " Writes:1", (Callable<Void>) () -> {
            int i = 0;
            for (DOMDataReadWriteTransaction writeTx :transactions) {
                // Writes /test/outer-list/i in writeTx
                writeTx.put(OPERATIONAL, outerListPath(i), outerList(i));
                i++;
            }
            return null;
        });

        measure("Txs:"+ txNum +  " Writes:" + innerNum, (Callable<Void>) () -> {
            int i = 0;
            for (DOMDataReadWriteTransaction writeTx :transactions) {
                // Writes /test/outer-list/i in writeTx
                YangInstanceIdentifier path = YangInstanceIdentifier.builder(outerListPath(i))
                        .node(TestModel.INNER_LIST_QNAME).build();
                writeTx.put(OPERATIONAL, path, ImmutableNodes.mapNodeBuilder(TestModel.INNER_LIST_QNAME).build());
                for (int j = 0; j < innerNum; j++) {
                    YangInstanceIdentifier innerPath = YangInstanceIdentifier.builder(path)
                            .nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, String.valueOf(j))
                            .build();
                    writeTx.put(
                            OPERATIONAL,
                            innerPath,
                            ImmutableNodes.mapEntry(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME,
                                    String.valueOf(j)));
                }
                i++;
            }
            return null;
        });

        measure("Txs:" + txNum + " Submit, Finish", (Callable<Void>) () -> {
            List<ListenableFuture<?>> allFutures = measure(txNum + " Submits",
                    () -> {
                        List<ListenableFuture<?>> builder = new ArrayList<>(txNum);
                        for (DOMDataReadWriteTransaction tx :transactions) {
                            builder.add(tx.submit());
                        }
                        return builder;
                    });
            Futures.allAsList(allFutures).get();
            return null;
        });

        final DOMDataReadTransaction readTx = measure("Txs:1 (ro), Allocate",
                (Callable<DOMDataReadTransaction>) () -> domBroker.newReadOnlyTransaction());


        measure("Txs:1 (ro) Reads:" + txNum + " (1-level)" , (Callable<Void>) () -> {
            for (int i = 0; i < txNum; i++) {
                ListenableFuture<Optional<NormalizedNode<?, ?>>> potential = readTx.read(OPERATIONAL,
                        outerListPath(i));
                assertTrue("outerList/" + i, potential.get().isPresent());
            }
            return null;
        });

        measure("Txs:1 (ro) Reads:" + txNum * innerNum + " (2-level)", (Callable<Void>) () -> {
            for (int i = 0; i < txNum; i++) {
                for (int j = 0; j < innerNum; j++) {
                    YangInstanceIdentifier path = YangInstanceIdentifier
                            .builder(outerListPath(i))
                            //
                            .node(TestModel.INNER_LIST_QNAME)
                            .nodeWithKey(TestModel.INNER_LIST_QNAME, TestModel.NAME_QNAME, String.valueOf(j))
                            .build();
                    ListenableFuture<Optional<NormalizedNode<?, ?>>> potential = readTx.read(OPERATIONAL, path);
                    assertTrue("outer-list/" + i + "/inner-list/" + j, potential.get().isPresent());
                }
            }
            return null;
        });
    }

    private void measureOneTransactionTopContainer() throws Exception {

        final DOMDataReadWriteTransaction writeTx = measure("Txs:1 Allocate", () -> domBroker.newReadWriteTransaction());

        measure("Txs:1 Write", (Callable<Void>) () -> {
            writeTx.put(OPERATIONAL, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            writeTx.put(OPERATIONAL, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
            return null;
        });

        measure("Txs:1 Reads:1", (Callable<Void>) () -> {
            // Reads /test in writeTx
            ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(OPERATIONAL,
                    TestModel.TEST_PATH);
            assertTrue(writeTxContainer.get().isPresent());
            return null;
        });

        measure("Txs:1 Reads:1", (Callable<Void>) () -> {
            // Reads /test in writeTx
            ListenableFuture<Optional<NormalizedNode<?, ?>>> writeTxContainer = writeTx.read(OPERATIONAL,
                    TestModel.TEST_PATH);
            assertTrue(writeTxContainer.get().isPresent());
            return null;
        });

        measure("Txs:1 Submit, Finish", (Callable<Void>) () -> {
            measure("Txs:1 Submit", (Callable<ListenableFuture<?>>) () -> writeTx.submit()).get();
            return null;
        });
    }
}
