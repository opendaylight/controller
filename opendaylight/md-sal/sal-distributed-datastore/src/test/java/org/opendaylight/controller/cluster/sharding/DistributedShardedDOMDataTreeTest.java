/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.cluster.datastore.IntegrationTestKit.findLocalShard;
import static org.opendaylight.controller.cluster.datastore.IntegrationTestKit.waitUntilShardIsDown;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedShardedDOMDataTreeTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTreeRemotingTest.class);

    private static final Address MEMBER_1_ADDRESS =
            AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");

    private static final DOMDataTreeIdentifier TEST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private static final DOMDataTreeIdentifier INNER_LIST_ID =
            new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                    YangInstanceIdentifier.create(getOuterListIdFor(0).getPathArguments())
                            .node(TestModel.INNER_LIST_QNAME));
    private static final Set<MemberName> SINGLE_MEMBER = Collections.singleton(AbstractTest.MEMBER_NAME);

    private static final String MODULE_SHARDS_CONFIG = "module-shards-default-member-1.conf";

    private ActorSystem leaderSystem;

    private final Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder()
                    .shardHeartbeatIntervalInMillis(100)
                    .shardElectionTimeoutFactor(2)
                    .logicalStoreType(LogicalDatastoreType.CONFIGURATION);

    private DistributedDataStore leaderDistributedDataStore;
    private DistributedDataStore operDistributedDatastore;
    private IntegrationTestKit leaderTestKit;

    private DistributedShardedDOMDataTree leaderShardFactory;

    @Captor
    private ArgumentCaptor<Collection<DataTreeCandidate>> captorForChanges;
    @Captor
    private ArgumentCaptor<Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>>> captorForSubtrees;

    private ActorSystemProvider leaderSystemProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        leaderSystemProvider = Mockito.mock(ActorSystemProvider.class);
        doReturn(leaderSystem).when(leaderSystemProvider).getActorSystem();
    }

    @After
    public void tearDown() {
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        if (operDistributedDatastore != null) {
            operDistributedDatastore.close();
        }

        TestKit.shutdownActorSystem(leaderSystem);

        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    private void initEmptyDatastores() throws Exception {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStore(
                "config", MODULE_SHARDS_CONFIG, "empty-modules.conf", true,
                SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());

        operDistributedDatastore = leaderTestKit.setupDistributedDataStore(
                "operational", MODULE_SHARDS_CONFIG, "empty-modules.conf",true,
                SchemaContextHelper.distributedShardedDOMDataTreeSchemaContext());

        leaderShardFactory = new DistributedShardedDOMDataTree(leaderSystemProvider,
                operDistributedDatastore,
                leaderDistributedDataStore);

        leaderShardFactory.init();
    }


    @Test
    public void testWritesIntoDefaultShard() throws Exception {
        initEmptyDatastores();

        final DOMDataTreeIdentifier configRoot =
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty());

        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(configRoot));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(true);
        final DOMDataTreeWriteCursor cursor =
                tx.createCursor(new DOMDataTreeIdentifier(
                        LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty()));
        Assert.assertNotNull(cursor);

        final ContainerNode test =
                ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME)).build();

        cursor.write(test.getIdentifier(), test);
        cursor.close();

        tx.commit().get();
    }

    @Test
    public void testSingleNodeWritesAndRead() throws Exception {
        initEmptyDatastores();

        final DistributedShardRegistration shardRegistration = waitOnAsyncTask(
                leaderShardFactory.createDistributedShard(TEST_ID, null,
                        Lists.newArrayList(AbstractTest.MEMBER_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(TEST_ID));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(true);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(TEST_ID);
        Assert.assertNotNull(cursor);
        final YangInstanceIdentifier nameId =
                YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(TestModel.NAME_QNAME).build();
        final LeafNode<String> valueToCheck = ImmutableLeafNodeBuilder.<String>create().withNodeIdentifier(
                new NodeIdentifier(TestModel.NAME_QNAME)).withValue("Test Value").build();
        LOG.debug("Writing data {} at {}, cursor {}", nameId.getLastPathArgument(), valueToCheck, cursor);
        cursor.write(nameId.getLastPathArgument(),
                valueToCheck);

        cursor.close();
        LOG.debug("Got to pre submit");

        tx.commit().get();

        final DOMDataTreeListener mockedDataTreeListener = mock(DOMDataTreeListener.class);
        doNothing().when(mockedDataTreeListener).onDataTreeChanged(anyCollection(), anyMap());

        leaderShardFactory.registerListener(mockedDataTreeListener, Collections.singletonList(TEST_ID),
                true, Collections.emptyList());

        verify(mockedDataTreeListener, timeout(1000).times(1)).onDataTreeChanged(captorForChanges.capture(),
                captorForSubtrees.capture());
        final List<Collection<DataTreeCandidate>> capturedValue = captorForChanges.getAllValues();

        final Optional<NormalizedNode<?, ?>> dataAfter =
                capturedValue.get(0).iterator().next().getRootNode().getDataAfter();

        final NormalizedNode<?,?> expected = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME)).withChild(valueToCheck).build();
        assertEquals(expected, dataAfter.get());

        verifyNoMoreInteractions(mockedDataTreeListener);

        final String shardName = ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier());
        LOG.debug("Creating distributed datastore client for shard {}", shardName);

        final ActorUtils actorUtils = leaderDistributedDataStore.getActorUtils();
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor.props(actorUtils.getCurrentMemberName(), "Shard-" + shardName, actorUtils,
                    shardName);

        final ActorRef clientActor = leaderSystem.actorOf(distributedDataStoreClientProps);
        final DataStoreClient distributedDataStoreClient = SimpleDataStoreClientActor
                    .getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);

        final ClientLocalHistory localHistory = distributedDataStoreClient.createLocalHistory();
        final ClientTransaction tx2 = localHistory.createTransaction();
        final FluentFuture<Optional<NormalizedNode<?, ?>>> read = tx2.read(YangInstanceIdentifier.empty());

        final Optional<NormalizedNode<?, ?>> optional = read.get();
        tx2.abort();
        localHistory.close();

        shardRegistration.close().toCompletableFuture().get();

    }

    @Test
    public void testMultipleWritesIntoSingleMapEntry() throws Exception {
        initEmptyDatastores();

        final DistributedShardRegistration shardRegistration = waitOnAsyncTask(
                leaderShardFactory.createDistributedShard(TEST_ID, null,
                        Lists.newArrayList(AbstractTest.MEMBER_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        LOG.warn("Got after waiting for nonleader");
        final ActorRef leaderShardManager = leaderDistributedDataStore.getActorUtils().getShardManager();

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        final YangInstanceIdentifier oid1 = getOuterListIdFor(0);
        final DOMDataTreeIdentifier outerListPath = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, oid1);

        final DistributedShardRegistration outerListShardReg = waitOnAsyncTask(
                leaderShardFactory.createDistributedShard(outerListPath, null,
                        Lists.newArrayList(AbstractTest.MEMBER_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(outerListPath.getRootIdentifier()));

        final DOMDataTreeProducer shardProducer = leaderShardFactory.createProducer(
                Collections.singletonList(outerListPath));

        final DOMDataTreeCursorAwareTransaction tx = shardProducer.createTransaction(false);
        final DOMDataTreeWriteCursor cursor =
                tx.createCursor(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, oid1));
        assertNotNull(cursor);

        MapNode innerList = ImmutableMapNodeBuilder
                .create()
                .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_LIST_QNAME))
                .build();

        cursor.write(new NodeIdentifier(TestModel.INNER_LIST_QNAME), innerList);
        cursor.close();
        tx.commit().get();

        final ArrayList<ListenableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final Collection<MapEntryNode> innerListMapEntries = createInnerListMapEntries(1000, "run-" + i);
            for (final MapEntryNode innerListMapEntry : innerListMapEntries) {
                final DOMDataTreeCursorAwareTransaction tx1 = shardProducer.createTransaction(false);
                final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(
                        new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                oid1.node(new NodeIdentifier(TestModel.INNER_LIST_QNAME))));
                cursor1.write(innerListMapEntry.getIdentifier(), innerListMapEntry);
                cursor1.close();
                futures.add(tx1.commit());
            }
        }

        futures.get(futures.size() - 1).get();

        final DOMDataTreeListener mockedDataTreeListener = mock(DOMDataTreeListener.class);
        doNothing().when(mockedDataTreeListener).onDataTreeChanged(anyCollection(), anyMap());

        leaderShardFactory.registerListener(mockedDataTreeListener, Collections.singletonList(INNER_LIST_ID),
                true, Collections.emptyList());

        verify(mockedDataTreeListener, timeout(1000).times(1)).onDataTreeChanged(captorForChanges.capture(),
                captorForSubtrees.capture());
        verifyNoMoreInteractions(mockedDataTreeListener);
        final List<Collection<DataTreeCandidate>> capturedValue = captorForChanges.getAllValues();

        final NormalizedNode<?,?> expected =
                ImmutableMapNodeBuilder
                        .create()
                        .withNodeIdentifier(new NodeIdentifier(TestModel.INNER_LIST_QNAME))
                                // only the values from the last run should be present
                        .withValue(createInnerListMapEntries(1000, "run-999"))
                        .build();

        assertEquals("List values dont match the expected values from the last run",
                expected, capturedValue.get(0).iterator().next().getRootNode().getDataAfter().get());

    }

    // top level shard at TEST element, with subshards on each outer-list map entry
    @Test
    @Ignore
    public void testMultipleShardLevels() throws Exception {
        initEmptyDatastores();

        final DistributedShardRegistration testShardReg = waitOnAsyncTask(
                leaderShardFactory.createDistributedShard(TEST_ID, null, SINGLE_MEMBER),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        final ArrayList<DistributedShardRegistration> registrations = new ArrayList<>();
        final int listSize = 5;
        for (int i = 0; i < listSize; i++) {
            final YangInstanceIdentifier entryYID = getOuterListIdFor(i);
            final CompletionStage<DistributedShardRegistration> future = leaderShardFactory.createDistributedShard(
                    new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, entryYID), null, SINGLE_MEMBER);

            registrations.add(waitOnAsyncTask(future, DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION));
        }

        final DOMDataTreeIdentifier rootId =
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty());
        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singletonList(
                rootId));

        DOMDataTreeCursorAwareTransaction transaction = producer.createTransaction(false);

        DOMDataTreeWriteCursor cursor = transaction.createCursor(rootId);
        assertNotNull(cursor);

        final MapNode outerList =
                ImmutableMapNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(TestModel.OUTER_LIST_QNAME)).build();

        final ContainerNode testNode =
                ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
                        .withChild(outerList)
                        .build();

        cursor.write(testNode.getIdentifier(), testNode);

        cursor.close();
        transaction.commit().get();

        final DOMDataTreeListener mockedDataTreeListener = mock(DOMDataTreeListener.class);
        doNothing().when(mockedDataTreeListener).onDataTreeChanged(anyCollection(), anyMap());

        final MapNode wholeList = ImmutableMapNodeBuilder.create(outerList)
                .withValue(createOuterEntries(listSize, "testing-values")).build();

        transaction = producer.createTransaction(false);
        cursor = transaction.createCursor(TEST_ID);
        assertNotNull(cursor);

        cursor.write(wholeList.getIdentifier(), wholeList);
        cursor.close();

        transaction.commit().get();

        leaderShardFactory.registerListener(mockedDataTreeListener, Collections.singletonList(TEST_ID),
                true, Collections.emptyList());

        verify(mockedDataTreeListener, timeout(35000).atLeast(2)).onDataTreeChanged(captorForChanges.capture(),
                captorForSubtrees.capture());
        verifyNoMoreInteractions(mockedDataTreeListener);
        final List<Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>>> allSubtrees = captorForSubtrees.getAllValues();

        final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> lastSubtree = allSubtrees.get(allSubtrees.size() - 1);

        final NormalizedNode<?, ?> actual = lastSubtree.get(TEST_ID);
        assertNotNull(actual);

        final NormalizedNode<?, ?> expected =
                ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
                        .withChild(ImmutableMapNodeBuilder.create(outerList)
                                .withValue(createOuterEntries(listSize, "testing-values")).build())
                        .build();


        for (final DistributedShardRegistration registration : registrations) {
            waitOnAsyncTask(registration.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);
        }

        waitOnAsyncTask(testShardReg.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleRegistrationsAtOnePrefix() throws Exception {
        initEmptyDatastores();

        for (int i = 0; i < 10; i++) {
            LOG.debug("Round {}", i);
            final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                    TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME)),
                    DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

            assertNotNull(findLocalShard(leaderDistributedDataStore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));

            waitOnAsyncTask(reg1.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

            waitUntilShardIsDown(leaderDistributedDataStore.getActorUtils(),
                    ClusterUtils.getCleanShardName(TestModel.TEST_PATH));
        }
    }

    @Test
    public void testCDSDataTreeProducer() throws Exception {
        initEmptyDatastores();

        final DistributedShardRegistration reg1 = waitOnAsyncTask(leaderShardFactory.createDistributedShard(
                TEST_ID, null, Lists.newArrayList(AbstractTest.MEMBER_NAME)),
                DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH));

        assertNotNull(findLocalShard(leaderDistributedDataStore.getActorUtils(),
                ClusterUtils.getCleanShardName(TestModel.TEST_PATH)));


        final DOMDataTreeIdentifier configRoot =
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty());
        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(configRoot));

        assertTrue(producer instanceof CDSDataTreeProducer);

        final CDSDataTreeProducer cdsProducer = (CDSDataTreeProducer) producer;
        CDSShardAccess shardAccess = cdsProducer.getShardAccess(TEST_ID);
        assertEquals(shardAccess.getShardIdentifier(), TEST_ID);

        shardAccess = cdsProducer.getShardAccess(INNER_LIST_ID);
        assertEquals(TEST_ID, shardAccess.getShardIdentifier());

        shardAccess = cdsProducer.getShardAccess(configRoot);
        assertEquals(configRoot, shardAccess.getShardIdentifier());

        waitOnAsyncTask(reg1.close(), DistributedShardedDOMDataTree.SHARD_FUTURE_TIMEOUT_DURATION);
    }

    private static Collection<MapEntryNode> createOuterEntries(final int amount, final String valuePrefix) {
        final Collection<MapEntryNode> ret = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ret.add(ImmutableNodes.mapEntryBuilder()
                    .withNodeIdentifier(NodeIdentifierWithPredicates.of(TestModel.OUTER_LIST_QNAME,
                            QName.create(TestModel.OUTER_LIST_QNAME, "id"), i))
                    .withChild(ImmutableNodes
                            .leafNode(QName.create(TestModel.OUTER_LIST_QNAME, "id"), i))
                    .withChild(createWholeInnerList(amount, "outer id: " + i + " " + valuePrefix))
                    .build());
        }

        return ret;
    }

    private static MapNode createWholeInnerList(final int amount, final String valuePrefix) {
        return ImmutableMapNodeBuilder.create().withNodeIdentifier(new NodeIdentifier(TestModel.INNER_LIST_QNAME))
                .withValue(createInnerListMapEntries(amount, valuePrefix)).build();
    }

    private static Collection<MapEntryNode> createInnerListMapEntries(final int amount, final String valuePrefix) {
        final Collection<MapEntryNode> ret = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ret.add(ImmutableNodes.mapEntryBuilder()
                    .withNodeIdentifier(NodeIdentifierWithPredicates.of(TestModel.INNER_LIST_QNAME,
                            QName.create(TestModel.INNER_LIST_QNAME, "name"), Integer.toString(i)))
                    .withChild(ImmutableNodes
                            .leafNode(QName.create(TestModel.INNER_LIST_QNAME, "value"), valuePrefix + "-" + i))
                    .build());
        }

        return ret;
    }

    private static YangInstanceIdentifier getOuterListIdFor(final int id) {
        return TestModel.OUTER_LIST_PATH.node(NodeIdentifierWithPredicates.of(
                TestModel.OUTER_LIST_QNAME, QName.create(TestModel.OUTER_LIST_QNAME, "id"), id));
    }
}
