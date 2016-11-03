/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static org.junit.Assert.assertNotNull;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory.DistributedShardRegistration;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedShardedDOMDataTreeTest  extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTreeRemotingTest.class);

    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");

    private static final DOMDataTreeIdentifier TEST_ID = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);

    private ActorSystem leaderSystem;

    private final Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(2);

    private DistributedDataStore leaderDistributedDataStore;
    private IntegrationTestKit leaderTestKit;

    private DistributedShardedDOMDataTree leaderShardFactory;

    @Before
    public void setUp() {

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

    }

    @After
    public void tearDown() {
        if (leaderDistributedDataStore != null) {
            leaderDistributedDataStore.close();
        }

        JavaTestKit.shutdownActorSystem(leaderSystem);
    }

    private void initEmptyDatastore(final String type) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);

        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStoreWithoutConfig(type, SchemaContextHelper.full());

        leaderShardFactory = new DistributedShardedDOMDataTree(leaderSystem,
                leaderDistributedDataStore,
                leaderDistributedDataStore);
    }

    @Test
    @Ignore("The datastore client doesn't forward the requests to backend yet")
    public void testSingleNodeWrites() throws Exception {
        initEmptyDatastore("config");

        final DistributedShardRegistration shardRegistration = leaderShardFactory.createDistributedShard(TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME));
        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        LOG.warn("Got after waiting for nonleader");
        final ActorRef leaderShardManager = leaderDistributedDataStore.getActorContext().getShardManager();

        new JavaTestKit(leaderSystem) {{
            leaderShardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            leaderDistributedDataStore.getActorContext().getShardManager().tell(new FindPrimary(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
        }};

        final DOMDataTreeProducer producer = leaderShardFactory.createProducer(Collections.singleton(TEST_ID));

        final DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(true);
        final DOMDataTreeWriteCursor cursor = tx.createCursor(TEST_ID);
        Assert.assertNotNull(cursor);
        final YangInstanceIdentifier nameId = YangInstanceIdentifier.builder(TestModel.TEST_PATH).node(TestModel.NAME_QNAME).build();
        cursor.write(nameId.getLastPathArgument(),
                ImmutableLeafNodeBuilder.<String>create().withNodeIdentifier(new NodeIdentifier(TestModel.NAME_QNAME)).withValue("Test Value").build());

        cursor.close();
        LOG.warn("Got to pre submit");

        tx.submit().checkedGet();
    }

    @Test
    public void testMultipleWritesIntoSingleMapEntry() throws Exception {
        initEmptyDatastore("config");

        final DistributedShardRegistration shardRegistration = leaderShardFactory.createDistributedShard(TEST_ID, Lists.newArrayList(AbstractTest.MEMBER_NAME));
        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(), ClusterUtils.getCleanShardName(TEST_ID.getRootIdentifier()));

        LOG.warn("Got after waiting for nonleader");
        final ActorRef leaderShardManager = leaderDistributedDataStore.getActorContext().getShardManager();

        new JavaTestKit(leaderSystem) {{
            leaderShardManager.tell(new FindLocalShard(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            leaderDistributedDataStore.getActorContext().getShardManager()
                    .tell(new FindPrimary(ClusterUtils.getCleanShardName(TestModel.TEST_PATH), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
        }};

        final YangInstanceIdentifier oid1 = TestModel.OUTER_LIST_PATH.node(new NodeIdentifierWithPredicates(
                TestModel.OUTER_LIST_QNAME, QName.create(TestModel.OUTER_LIST_QNAME, "id"), 0));
        final DOMDataTreeIdentifier outerListPath = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, oid1);

        final DistributedShardRegistration outerListShardReg = leaderShardFactory.createDistributedShard(outerListPath,
                Lists.newArrayList(AbstractTest.MEMBER_NAME));

        new JavaTestKit(leaderSystem) {{
            leaderShardManager.tell(new FindLocalShard(
                    ClusterUtils.getCleanShardName(outerListPath.getRootIdentifier()), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalShardFound.class);

            leaderDistributedDataStore.getActorContext().getShardManager()
                    .tell(new FindPrimary(ClusterUtils.getCleanShardName(outerListPath.getRootIdentifier()), true), getRef());
            expectMsgClass(duration("5 seconds"), LocalPrimaryShardFound.class);
        }};

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
        tx.submit().checkedGet();

        final ArrayList<CheckedFuture<Void, TransactionCommitFailedException>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final Collection<MapEntryNode> innerListMapEntries = createInnerListMapEntries(1000, "run-" + i);
            for (final MapEntryNode innerListMapEntry : innerListMapEntries) {
                final DOMDataTreeCursorAwareTransaction tx1 = shardProducer.createTransaction(false);
                final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(
                        new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                oid1.node(new NodeIdentifier(TestModel.INNER_LIST_QNAME))));
                cursor1.write(innerListMapEntry.getIdentifier(), innerListMapEntry);
                cursor1.close();
                futures.add(tx1.submit());
            }
        }

        futures.get(futures.size() - 1).checkedGet();

    }

    private static Collection<MapEntryNode> createInnerListMapEntries(final int amount, final String valuePrefix) {
        final Collection<MapEntryNode> ret = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            ret.add(ImmutableNodes.mapEntryBuilder()
                    .withNodeIdentifier(new NodeIdentifierWithPredicates(TestModel.INNER_LIST_QNAME,
                            QName.create(TestModel.OUTER_LIST_QNAME, "name"), Integer.toString(i)))
                    .withChild(ImmutableNodes
                            .leafNode(QName.create(TestModel.INNER_LIST_QNAME, "name"), Integer.toString(i)))
                    .withChild(ImmutableNodes
                            .leafNode(QName.create(TestModel.INNER_LIST_QNAME, "value"), valuePrefix + "-" + i))
                    .build());
        }

        return ret;
    }
}