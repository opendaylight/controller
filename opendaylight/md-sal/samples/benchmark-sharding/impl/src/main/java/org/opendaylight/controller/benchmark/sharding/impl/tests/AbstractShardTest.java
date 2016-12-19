/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl.tests;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.benchmark.sharding.impl.DomListBuilder;
import org.opendaylight.controller.benchmark.sharding.impl.ShardFactory;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper;
import org.opendaylight.controller.benchmark.sharding.impl.ShardHelper.ShardData;
import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractShardTest implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractShardTest.class);

    private final List<ListenerRegistration<ShardTestListener>> testListenerRegs = new ArrayList<>();
    private ListenerRegistration<ValidationListener> validationListenerReg = null;
    private final LogicalDatastoreType datastoreType;

    private final DOMDataTreeService dataTreeService;
    private final ShardFactory shardFactory;
    private final long numListeners;
    protected final long numShards;
    protected final long numItems;
    private final ShardHelper shardHelper;

    protected final boolean preCreateTestData;
    protected final long opsPerTx;
    protected final List<ShardData> shardData = new ArrayList<>();

    private final List<ShardFactory.ShardRegistration> shardRegistrations = new ArrayList<>();
    static final YangInstanceIdentifier TEST_DATA_ROOT_YID =
            YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

    /**
     * Constructor for the ShardTest class.
     *
     * @param numShards number of shards to use in the test
     * @param numItems number of data items to store
     * @param dataStoreType CONFIG or OPERATIONAL
     * @param shardHelper reference to the Shard Helper
     * @param dataTreeService reference to the MD-SAL dataTreeService
     * @throws ShardTestException when shards or data tree listeners could not be created/registered
     */
    AbstractShardTest(final long numShards, final long numItems, final long numListeners, final long opsPerTx,
            final LogicalDatastoreType dataStoreType, final boolean precreateTestData, final ShardHelper shardHelper,
            final DOMDataTreeService dataTreeService, final ShardFactory shardFactory) throws ShardTestException {
        this.shardHelper = shardHelper;
        LOG.info("Creating ShardTest");

        this.dataTreeService = dataTreeService;
        this.numShards = numShards;
        this.numItems = numItems;
        this.preCreateTestData = precreateTestData;
        this.opsPerTx = opsPerTx;
        this.shardFactory = shardFactory;
        this.datastoreType = dataStoreType;
        this.numListeners = numListeners;
    }

    List<SingleShardTest> createTestShardLayout() throws ShardTestException, DOMDataTreeShardCreationFailedException,
            DOMDataTreeProducerException, DOMDataTreeShardingConflictException {
        final List<SingleShardTest> singleShardTests = new ArrayList<>();
        final ArrayList<DOMDataTreeIdentifier> treeIds = new ArrayList<>();

        // TODO we should catch all exceptions and at least LOG what happened
        for (long i = 0L; i < numShards; i++) {
            final YangInstanceIdentifier yiId =
                    TEST_DATA_ROOT_YID.node(new NodeIdentifierWithPredicates(
                            OuterList.QNAME, QName.create(OuterList.QNAME, "oid"), i));
            shardRegistrations.add(shardFactory.createShard(new DOMDataTreeIdentifier(datastoreType, yiId)));
            singleShardTests.add(
                    new SingleShardTest(dataTreeService, new DOMDataTreeIdentifier(datastoreType, yiId), opsPerTx));
            treeIds.add(new DOMDataTreeIdentifier(datastoreType, yiId));
        }

        try {
            for (long i = 0; i < numListeners; i++) {
                testListenerRegs.add(dataTreeService.registerListener(new ShardTestListener(),
                        treeIds, false, Collections.emptyList()));
            }
        } catch (DOMDataTreeLoopException e) {
            LOG.error("Failed to register a test listener, exception {}", e);
            throw new ShardTestException(e.getMessage(), e.getCause());
        }

        createListAnchors(singleShardTests);

        singleShardTests.forEach(SingleShardTest::initCursor);

        return singleShardTests;
    }

    /**
     * Pre-creates test data (InnerList elements) before the measured test
     * run and puts them in an array list for quick retrieval during the
     * test run.
     * @return the list of pre-created test elements that will be pushed
     *          into the data store during the test run.
     */
    protected List<MapEntryNode> preCreateTestData() {
        final List<MapEntryNode> testData;
        if (preCreateTestData) {
            LOG.info("Pre-creating test data...");
            testData = new ArrayList<>();
            for (long i = 0; i < numItems; i++) {
                for (int s = 0; s < numShards; s++) {
                    NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                            DomListBuilder.IL_NAME, i);
                    testData.add(createListEntry(nodeId, s, i));
                }
            }
            LOG.info("   Done. {} elements created.", testData.size());
        } else {
            LOG.info("No test data pre-created.");
            testData = null;
        }
        return testData;
    }

    /**
     * Creates a root "anchor" node (actually an InnerList hanging off an
     * outer list item) in each shard.
     *
     */
    private void createListAnchors(final List<SingleShardTest> singleShardTests) {
        final MapNode mapNode = ImmutableMapNodeBuilder
                .create()
                .withNodeIdentifier(new NodeIdentifier(InnerList.QNAME))
                .build();

        for (final SingleShardTest test : singleShardTests) {

            final DOMDataTreeProducer producer = test.getProducer();
            final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(false);

            final DOMDataTreeWriteCursor cursor = Preconditions.checkNotNull(tx.createCursor(test.getPrefix()));
            final YangInstanceIdentifier shardRootYid = test.getPrefix().getRootIdentifier();

            cursor.write(shardRootYid.node(InnerList.QNAME).getLastPathArgument(), mapNode);
            cursor.close();

            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Failed to create container for inner list, {}", e);
                throw new RuntimeException(e);
            }
        }
    }

    /** Retrieve the number of ok event notifications from all TestListeners.
     * @return the total number of ok event notifications from
     *      all TestListeners
     */
    protected int getListenerEventsOk() {
        int listenerEventsOk = 0;
        for ( ListenerRegistration<ShardTestListener> lreg : testListenerRegs) {
            listenerEventsOk += lreg.getInstance().getDataTreeEventsOk();
        }
        return listenerEventsOk;
    }

    /** Retrieve the number of failed event notifications from all
     *  TestListeners.
     * @return the total number of failed event notifications from
     *      all TestListeners
     */
    protected int getListenerEventsFail() {
        int listenerEventsFail = 0;
        for ( ListenerRegistration<ShardTestListener> lreg : testListenerRegs) {
            listenerEventsFail += lreg.getInstance().getDataTreeEventsFail();
        }
        return listenerEventsFail;
    }

    public static MapEntryNode createListEntry(final NodeIdentifierWithPredicates nodeId,
            final int shardIndex, final long elementIndex) {
        return ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(nodeId)
                .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_NAME, elementIndex))
                .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_VALUE,
                        "Item-" + String.valueOf(shardIndex) + "-" + String.valueOf((int)elementIndex)))
                .build();
    }

    /** Registers the validation listener with MD-SAL.
     * @throws DOMDataTreeLoopException when registration fails
     *
     */
    public void registerValidationListener() throws DOMDataTreeLoopException {
        LOG.info("Registering validation listener");
        if (validationListenerReg == null) {
            final ArrayList<DOMDataTreeIdentifier> treeIds = new ArrayList<>();
            shardData.forEach(sd -> treeIds.add(sd.getDOMDataTreeIdentifier()));
            validationListenerReg = dataTreeService.registerListener(new ValidationListener(),
                    treeIds, false, Collections.emptyList());
        } else {
            LOG.warn("Validation listener already registered");
        }
    }

    /* (non-Javadoc) Close function should be called to cleanup registrations
     * with MD-SAL.
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        LOG.info("Closing ShardTest");

        for (ShardFactory.ShardRegistration shardReg : shardRegistrations) {
            shardReg.close();
        }

        shardHelper.close();

        testListenerRegs.forEach( lReg -> lReg.close());
        if (validationListenerReg != null) {
            validationListenerReg.close();
            validationListenerReg = null;
        }
    }

    public abstract ShardTestStats runTest() throws DOMDataTreeShardingConflictException, ShardTestException,
            DOMDataTreeProducerException, DOMDataTreeShardCreationFailedException;
}
