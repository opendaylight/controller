package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.opendaylight.controller.md.sal.dom.store.impl.SchemaContextConfigProxy;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class ConfigStatementCheckTest {

    private static final String DATASTORE_TEST_YANG = "/config-statement-test-model.yang";
    private static final QName TEST_MODEL_NAME = QName.create("urn:opendaylight:configtest", "2015-01-13",
            "config-statement-test-model");
    private static final QName CONFIG_CONT = QName.create(TEST_MODEL_NAME, "config-cont");
    private static final QName OPER_CONT = QName.create(TEST_MODEL_NAME, "oper-cont");
    private static final QName OPER_LEAF = QName.create(TEST_MODEL_NAME, "oper-leaf");

    private static final YangInstanceIdentifier CONFIG_CONT_PATH = YangInstanceIdentifier.of(CONFIG_CONT);
    private static final YangInstanceIdentifier OPER_CONT_PATH = YangInstanceIdentifier.of(OPER_CONT);
    private static final YangInstanceIdentifier OPER_LEAF_PATH = CONFIG_CONT_PATH.node(OPER_LEAF);

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
        configStore.onGlobalContextUpdated(new SchemaContextConfigProxy(schemaContext));

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
    public void insertIntoOperOnlyLeaf() throws ReadFailedException, ExecutionException, InterruptedException {
        assertNotNull(domBroker);

        DOMDataReadWriteTransaction writeTx = domBroker.newReadWriteTransaction();
        assertNotNull(writeTx);
        /**
         *
         * Writes /test in writeTx
         *
         */
        writeTx.put(CONFIGURATION, CONFIG_CONT_PATH, ImmutableNodes.containerNode(CONFIG_CONT));

        writeTx.submit().get();

        writeTx = domBroker.newReadWriteTransaction();
        writeTx.put(CONFIGURATION, CONFIG_CONT_PATH, ImmutableNodes.containerNode(CONFIG_CONT));
        writeTx.put(CONFIGURATION, OPER_LEAF_PATH, ImmutableNodes.leafNode(OPER_LEAF, "liif"));
        writeTx.submit().get();

        DOMDataReadTransaction readTx = domBroker.newReadOnlyTransaction();
        assertNotNull(readTx);
        Optional<NormalizedNode<?, ?>> read = readTx.read(CONFIGURATION, CONFIG_CONT_PATH).checkedGet();
        assertTrue(read.isPresent());
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
