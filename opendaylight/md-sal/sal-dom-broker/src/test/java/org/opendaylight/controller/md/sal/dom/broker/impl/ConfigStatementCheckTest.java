/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.ConfigInMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.ProxyConfigSchemaContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class ConfigStatementCheckTest {

    private static final String DATASTORE_TEST_YANG = "/config-statement-test-model.yang";
    private static final QName TEST_MODEL_NAME = QName.create("urn:opendaylight:configtest", "2015-01-13",
            "config-statement-test-model");
    private static final QName CONFIG_CONT_1 = QName.create(TEST_MODEL_NAME, "config-cont-1");
    private static final QName CONFIG_LIST_2 = QName.create(TEST_MODEL_NAME, "config-list-2");
    private static final QName CONFIG_LEAF_3 = QName.create(TEST_MODEL_NAME, "config-leaf-3");
    private static final QName OPER_LIST_3 = QName.create(TEST_MODEL_NAME, "oper-list-3");
    private static final QName OPER_CONT_1 = QName.create(TEST_MODEL_NAME, "oper-cont-1");
    private static final QName OPER_LEAF_2 = QName.create(TEST_MODEL_NAME, "oper-leaf-2");
    private static final QName OPER_LEAF_4 = QName.create(TEST_MODEL_NAME, "oper-leaf-4");
    private static final QName CONFIG_CONT_GROUPING_2 = QName.create(TEST_MODEL_NAME, "config-cont-grouping-2");
    private static final QName OPER_LEAF_LIST_2 = QName.create(TEST_MODEL_NAME, "oper-leaf-list-2");
    private static final QName OPER_LEAF_IN_GROUPING = QName.create(TEST_MODEL_NAME, "oper-leaf-in-grouping");
    private static final QName CONFIG_LEAF_IN_GROUPING = QName.create(TEST_MODEL_NAME, "config-leaf-in-grouping");

    private static final YangInstanceIdentifier CONFIG_CONT_1_PATH = YangInstanceIdentifier.of(CONFIG_CONT_1);
    private static final YangInstanceIdentifier CONFIG_LIST_2_PATH = CONFIG_CONT_1_PATH.node(CONFIG_LIST_2);
    private static final YangInstanceIdentifier CONFIG_LEAF_3_PATH = CONFIG_LIST_2_PATH.node(CONFIG_LEAF_3);
    private static final YangInstanceIdentifier CONFIG_CONT_GROUPING_2_PATH = CONFIG_CONT_1_PATH.node(CONFIG_CONT_GROUPING_2);
    private static final YangInstanceIdentifier OPER_LIST_3_PATH = CONFIG_LIST_2_PATH.node(OPER_LIST_3);
    private static final YangInstanceIdentifier OPER_CONT_1_PATH = YangInstanceIdentifier.of(OPER_CONT_1);
    private static final YangInstanceIdentifier OPER_LEAF_2_PATH = CONFIG_CONT_1_PATH.node(OPER_LEAF_2);
    private static final YangInstanceIdentifier OPER_LEAF_4_PATH = OPER_LIST_3_PATH.node(OPER_LEAF_4);
    private static final YangInstanceIdentifier OPER_LEAF_LIST_2_PATH = CONFIG_CONT_1_PATH.node(OPER_LEAF_LIST_2);
    private static final YangInstanceIdentifier OPER_LEAF_IN_GROUPING_PATH = CONFIG_CONT_GROUPING_2_PATH.node(OPER_LEAF_IN_GROUPING);
    private static final YangInstanceIdentifier CONFIG_LEAF_IN_GROUPING_PATH = CONFIG_CONT_GROUPING_2_PATH.node(CONFIG_LEAF_IN_GROUPING);


    private SchemaContext schemaContext;
    private AbstractDOMDataBroker domBroker;
    private ListeningExecutorService executor;
    private ExecutorService futureExecutor;
    private CommitExecutorService commitExecutor;

    @Before
    public void setupStore() {

        InMemoryDOMDataStore operStore = new InMemoryDOMDataStore("OPER",
                MoreExecutors.sameThreadExecutor());
        InMemoryDOMDataStore configStore = new ConfigInMemoryDOMDataStore("CFG",
                MoreExecutors.sameThreadExecutor());
        schemaContext = createTestContext();

        operStore.onGlobalContextUpdated(schemaContext);
        configStore.onGlobalContextUpdated(new ProxyConfigSchemaContext(schemaContext));

        ImmutableMap<LogicalDatastoreType, DOMStore> stores = ImmutableMap.<LogicalDatastoreType, DOMStore>builder() //
                .put(CONFIGURATION, configStore) //
                .put(OPERATIONAL, operStore) //
                .build();

        commitExecutor = new CommitExecutorService(Executors.newSingleThreadExecutor());
        futureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(1, 5, "FCB");
        executor = new DeadlockDetectingListeningExecutorService(commitExecutor,
                TransactionCommitDeadlockException.DEADLOCK_EXCEPTION_SUPPLIER, futureExecutor);
        domBroker = new SerializedDOMDataBroker(stores, executor);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }

        if (futureExecutor != null) {
            futureExecutor.shutdownNow();
        }
    }

    public static final InputStream getDatastoreTestInputStream() {
        return getInputStream(DATASTORE_TEST_YANG);
    }

    private static InputStream getInputStream(final String resourceName) {
        return ConfigStatementCheckTest.class.getResourceAsStream(DATASTORE_TEST_YANG);
    }

    public static SchemaContext createTestContext() {
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(getDatastoreTestInputStream()));
        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void insertIntoOperCont1OperDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(OPERATIONAL, OPER_CONT_1_PATH, ImmutableNodes.containerNode(OPER_CONT_1));
        writeTx.submit().get();

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);
        Optional<NormalizedNode<?, ?>> read = readTx.read(OPERATIONAL, OPER_CONT_1_PATH).checkedGet();
        assertTrue(read.isPresent());
    }

    @Test
    public void insertIntoOperCont1ConfDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        try {
            writeTx.put(CONFIGURATION, OPER_CONT_1_PATH, ImmutableNodes.containerNode(OPER_CONT_1));
        } catch (Exception e) {
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void insertIntoOperLeaf2OperDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(OPERATIONAL, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        writeTx.put(OPERATIONAL, OPER_LEAF_2_PATH, ImmutableNodes.leafNode(OPER_LEAF_2, "leaf"));
        writeTx.submit().get();

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);
        Optional<NormalizedNode<?, ?>> read = readTx.read(OPERATIONAL, OPER_LEAF_2_PATH).checkedGet();
        assertTrue(read.isPresent());
    }

    @Test
    public void insertIntoOperLeaf2ConfDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(CONFIGURATION, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        try {
            writeTx.put(CONFIGURATION, OPER_LEAF_2_PATH, ImmutableNodes.leafNode(OPER_LEAF_2, "leaf"));
        } catch (Exception e) {
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void insertIntoOperLeafList2OperDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        LeafSetNode<Object> leafSet = Builders.leafSetBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(OPER_LEAF_LIST_2))
                .withChildValue("leaf set").build();

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(OPERATIONAL, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        writeTx.put(OPERATIONAL, OPER_LEAF_LIST_2_PATH, leafSet);
        writeTx.submit().get();

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);
        Optional<NormalizedNode<?, ?>> read = readTx.read(OPERATIONAL, OPER_LEAF_LIST_2_PATH).checkedGet();
        assertTrue(read.isPresent());
    }

    @Test
    public void insertIntoOperLeafList2ConfDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        LeafSetNode<Object> leafSet = Builders.leafSetBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(OPER_LEAF_LIST_2))
                .withChildValue("leaf set").build();

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(CONFIGURATION, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        try {
            writeTx.put(CONFIGURATION, OPER_LEAF_LIST_2_PATH, leafSet);
        } catch (Exception e) {
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void insertIntoConfigContGrouping2OperDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(OPERATIONAL, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        writeTx.put(OPERATIONAL, CONFIG_CONT_GROUPING_2_PATH, ImmutableNodes.containerNode(CONFIG_CONT_GROUPING_2));
        writeTx.put(OPERATIONAL, CONFIG_LEAF_IN_GROUPING_PATH, ImmutableNodes.leafNode(CONFIG_LEAF_IN_GROUPING, "confgroupleaf"));
        writeTx.put(OPERATIONAL, OPER_LEAF_IN_GROUPING_PATH, ImmutableNodes.leafNode(OPER_LEAF_IN_GROUPING, "opergroupleaf"));
        writeTx.submit().get();

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);
        Optional<NormalizedNode<?, ?>> read = readTx.read(OPERATIONAL, CONFIG_LEAF_IN_GROUPING_PATH).checkedGet();
        assertTrue(read.isPresent());
        read = readTx.read(OPERATIONAL, OPER_LEAF_IN_GROUPING_PATH).checkedGet();
        assertTrue(read.isPresent());
    }

    @Test
    public void insertIntoConfigContGrouping2ConfDS() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(CONFIGURATION, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
        writeTx.put(CONFIGURATION, CONFIG_CONT_GROUPING_2_PATH, ImmutableNodes.containerNode(CONFIG_CONT_GROUPING_2));
        writeTx.put(CONFIGURATION, CONFIG_LEAF_IN_GROUPING_PATH, ImmutableNodes.leafNode(CONFIG_LEAF_IN_GROUPING, "confgroupleaf"));
        try {
            writeTx.put(CONFIGURATION, OPER_LEAF_IN_GROUPING_PATH, ImmutableNodes.leafNode(OPER_LEAF_IN_GROUPING, "opergroupleaf"));
        } catch (Exception e) {
            return;
        }
        fail("Exception should have been thrown.");
    }

    @Test
    public void insertIntoOperList3OperDS() throws ReadFailedException, ExecutionException, InterruptedException {
//        assertNotNull(domBroker);
//
//        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
//        assertNotNull(writeTx);
//
//        writeTx = domBroker.newReadWriteTransaction();
//        writeTx.put(OPERATIONAL, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
//        writeTx.put(OPERATIONAL, CONFIG_LIST_2_PATH, ImmutableNodes.mapNodeBuilder(CONFIG_LIST_2).build());
//        writeTx.put(OPERATIONAL, OPER_LIST_3_PATH, ImmutableNodes.mapNodeBuilder(OPER_LIST_3).build());
//        writeTx.submit().get();
//
//        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
//        assertNotNull(readTx);
//        Optional<NormalizedNode<?, ?>> read = readTx.read(OPERATIONAL, OPER_LIST_3_PATH).checkedGet();
//        assertTrue(read.isPresent());
    }

    @Test
    public void insertIntoOperList3ConfDS() throws ReadFailedException, ExecutionException, InterruptedException {
//        assertNotNull(domBroker);
//
//        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
//        assertNotNull(writeTx);
//
//        writeTx = domBroker.newReadWriteTransaction();
//        writeTx.put(CONFIGURATION, CONFIG_CONT_1_PATH, ImmutableNodes.containerNode(CONFIG_CONT_1));
//        writeTx.put(CONFIGURATION, CONFIG_LIST_2_PATH, ImmutableNodes.mapNodeBuilder(CONFIG_LIST_2).build());
//        try {
//            writeTx.put(CONFIGURATION, OPER_LIST_3_PATH, ImmutableNodes.mapNodeBuilder(OPER_LIST_3).build());
//        } catch (Exception e) {
//            return;
//        }
//        fail("Exception should have been thrown.");
    }


    static class CommitExecutorService extends ForwardingExecutorService {

        ExecutorService delegate;

        public CommitExecutorService(final ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        protected ExecutorService delegate() {
            return delegate;
        }
    }
}
