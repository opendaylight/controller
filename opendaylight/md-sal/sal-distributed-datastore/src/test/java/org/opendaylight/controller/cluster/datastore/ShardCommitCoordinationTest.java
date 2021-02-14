/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertNotNull;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.ID_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.INNER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.NAME_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerMapPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerMapNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;

import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableSortedSet;
import java.time.Duration;
import java.util.SortedSet;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for various 3PC coordination scenarios.
 *
 * @author Thomas Pantelis
 */
public class ShardCommitCoordinationTest extends AbstractShardTest {
    private static final Logger LOG = LoggerFactory.getLogger(ShardCommitCoordinationTest.class);

    /**
     * Test 2 tx's accessing the same shards.
     * <pre>
     *   tx1 -> shard A, shard B
     *   tx2 -> shard A, shard B
     * </pre>
     * The tx's are readied such the pendingTransactions queue are as follows:
     * <pre>
     *   Queue for shard A -> tx1, tx2
     *   Queue for shard B -> tx2, tx1
     * </pre>
     * This is a potential deadlock scenario (ABBA) which should be avoided by allowing tx1 to proceed on shard B
     * even though it isn't at the head of the queues.
     */
    @Test
    public void testTwoTransactionsWithSameTwoParticipatingShards() {
        final String testName = "testTwoTransactionsWithSameTwoParticipatingShards";
        LOG.info("{} starting", testName);

        final TestKit kit1 = new TestKit(getSystem());
        final TestKit kit2 = new TestKit(getSystem());

        final ShardIdentifier shardAId = ShardIdentifier.create("shardA", MemberName.forName(testName), "config");
        final ShardIdentifier shardBId = ShardIdentifier.create("shardB", MemberName.forName(testName), "config");

        final TestActorRef<Shard> shardA = actorFactory.createTestActor(
                newShardBuilder().id(shardAId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardA);

        final TestActorRef<Shard> shardB = actorFactory.createTestActor(
                newShardBuilder().id(shardBId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardB);

        final TransactionIdentifier txId1 = nextTransactionId();
        final TransactionIdentifier txId2 = nextTransactionId();

        SortedSet<String> participatingShardNames = ImmutableSortedSet.of(shardAId.getShardName(),
                shardBId.getShardName());

        // Ready [tx1, tx2] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        shardA.tell(newReadyBatchedModifications(txId2, OUTER_LIST_PATH, outerNode(1),
                participatingShardNames), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, OUTER_LIST_PATH, outerNode(1),
                participatingShardNames), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId1, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Send tx2 CanCommit to A - tx1 is at the head of the queue so tx2 should not proceed as A is the first shard
        // in the participating shard list.

        shardA.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectNoMessage(Duration.ofMillis(100));

        // Send tx1 CanCommit to A - it's at the head of the queue so should proceed.

        shardA.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to B - tx2 is at the head of the queue but the preceding shards in tx1's participating
        // shard list [A] matches that of tx2 [A] so tx1 should be de-queued and allowed to proceed.

        shardB.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx2 CanCommit to B - tx1 should now be at the head of he queue.

        shardB.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectNoMessage(Duration.ofMillis(100));

        // Finish commit of tx1.

        shardA.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx2.

        kit2.expectMsgClass(CanCommitTransactionReply.class);
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        shardA.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        // Verify data in the data store.

        verifyOuterListEntry(shardA, 1);
        verifyOuterListEntry(shardB, 1);

        LOG.info("{} ending", testName);
    }

    /**
     * Test multiple tx's accessing a mix of same and differing shards.
     * <pre>
     *   tx1 -> shard X, shard B
     *   tx2 -> shard X, shard B
     *   tx3 -> shard A, shard B
     *   tx4 -> shard A, shard B
     *   tx5 -> shard A, shard B
     * </pre>
     * The tx's are readied such the pendingTransactions queue are as follows:
     * <pre>
     *   Queue for shard A -> tx3, tx4, tx5
     *   Queue for shard B -> tx1, tx2, tx5, tx4, tx3
     * </pre>
     * Note: shard X means any other shard which isn't relevant for the test.
     * This is a potential deadlock scenario (ABBA) which should be avoided by moving tx3 ahead of tx5 on shard B when
     * CanCommit is requested.
     */
    @Test
    public void testMultipleTransactionsWithMixedParticipatingShards() {
        final String testName = "testMultipleTransactionsWithMixedParticipatingShards";
        LOG.info("{} starting", testName);

        final TestKit kit1 = new TestKit(getSystem());
        final TestKit kit2 = new TestKit(getSystem());
        final TestKit kit3 = new TestKit(getSystem());
        final TestKit kit4 = new TestKit(getSystem());
        final TestKit kit5 = new TestKit(getSystem());

        final ShardIdentifier shardAId = ShardIdentifier.create("shardA", MemberName.forName(testName), "config");
        final ShardIdentifier shardBId = ShardIdentifier.create("shardB", MemberName.forName(testName), "config");

        final TestActorRef<Shard> shardA = actorFactory.createTestActor(
                newShardBuilder().id(shardAId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardA);

        final TestActorRef<Shard> shardB = actorFactory.createTestActor(
                newShardBuilder().id(shardBId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardB);

        final TransactionIdentifier txId1 = nextTransactionId();
        final TransactionIdentifier txId2 = nextTransactionId();
        final TransactionIdentifier txId3 = nextTransactionId();
        final TransactionIdentifier txId4 = nextTransactionId();
        final TransactionIdentifier txId5 = nextTransactionId();

        final SortedSet<String> participatingShardNames1 = ImmutableSortedSet.of(shardAId.getShardName(),
                shardBId.getShardName());
        final SortedSet<String> participatingShardNames2 = ImmutableSortedSet.of("shardX", shardBId.getShardName());

        // Ready [tx3, tx4, tx5] on shard A.

        shardA.tell(newReadyBatchedModifications(txId3, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames1), kit3.getRef());
        kit3.expectMsgClass(ReadyTransactionReply.class);

        shardA.tell(newReadyBatchedModifications(txId4, OUTER_LIST_PATH, outerMapNode(),
                participatingShardNames1), kit4.getRef());
        kit4.expectMsgClass(ReadyTransactionReply.class);

        shardA.tell(newReadyBatchedModifications(txId5, outerEntryPath(1),
                ImmutableNodes.mapEntry(OUTER_LIST_QNAME, ID_QNAME, 1), participatingShardNames1), kit5.getRef());
        kit5.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx1, tx2, tx5, tx4, tx3] on shard B.

        shardB.tell(newReadyBatchedModifications(txId1, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames2), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId2, OUTER_LIST_PATH, outerMapNode(),
                participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId5, innerEntryPath(1, "one"),
                ImmutableNodes.mapEntry(INNER_LIST_QNAME, NAME_QNAME, "one"), participatingShardNames1), kit5.getRef());
        kit5.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId4, innerMapPath(1), innerNode(),
                participatingShardNames1), kit4.getRef());
        kit4.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId3, outerEntryPath(1),
                ImmutableNodes.mapEntry(OUTER_LIST_QNAME, ID_QNAME, 1), participatingShardNames1), kit3.getRef());
        kit3.expectMsgClass(ReadyTransactionReply.class);

        // Send tx3 CanCommit to A - it's at the head of the queue so should proceed.

        shardA.tell(new CanCommitTransaction(txId3, CURRENT_VERSION).toSerializable(), kit3.getRef());
        kit3.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to B - it's at the head of the queue so should proceed.

        shardB.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx3 CanCommit to B - tx1 is at the head of the queue but the preceding shards in tx3's participating
        // shard list [A] matches that of tx5 so tx3 should be moved ahead of tx5 in the queue.

        shardB.tell(new CanCommitTransaction(txId3, CURRENT_VERSION).toSerializable(), kit3.getRef());
        kit3.expectNoMessage(Duration.ofMillis(100));

        // Send tx4 CanCommit to B - tx4's participating shard list [A] matches that of tx3 and tx5 - so tx4 should
        // be moved ahead of tx5 in the queue but not tx3 since should be in the CAN_COMMIT_PENDING state.

        shardB.tell(new CanCommitTransaction(txId4, CURRENT_VERSION).toSerializable(), kit4.getRef());
        kit4.expectNoMessage(Duration.ofMillis(100));

        // Send tx5 CanCommit to B - it's position in the queue should remain the same.

        shardB.tell(new CanCommitTransaction(txId5, CURRENT_VERSION).toSerializable(), kit5.getRef());
        kit5.expectNoMessage(Duration.ofMillis(100));

        // Finish commit of tx1.

        shardB.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx2.

        shardB.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx3.

        // From shard B
        kit3.expectMsgClass(CanCommitTransactionReply.class);

        shardA.tell(new CommitTransaction(txId3, CURRENT_VERSION).toSerializable(), kit3.getRef());
        kit3.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId3, CURRENT_VERSION).toSerializable(), kit3.getRef());
        kit3.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx4.

        // From shard B
        kit4.expectMsgClass(CanCommitTransactionReply.class);

        shardA.tell(new CanCommitTransaction(txId4, CURRENT_VERSION).toSerializable(), kit4.getRef());
        kit4.expectMsgClass(CanCommitTransactionReply.class);
        shardA.tell(new CommitTransaction(txId4, CURRENT_VERSION).toSerializable(), kit4.getRef());
        kit4.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId4, CURRENT_VERSION).toSerializable(), kit4.getRef());
        kit4.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx5.

        // From shard B
        kit5.expectMsgClass(CanCommitTransactionReply.class);

        shardA.tell(new CanCommitTransaction(txId5, CURRENT_VERSION).toSerializable(), kit5.getRef());
        kit5.expectMsgClass(CanCommitTransactionReply.class);
        shardA.tell(new CommitTransaction(txId5, CURRENT_VERSION).toSerializable(), kit5.getRef());
        kit5.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId5, CURRENT_VERSION).toSerializable(), kit5.getRef());
        kit5.expectMsgClass(CommitTransactionReply.class);

        verifyOuterListEntry(shardA, 1);
        verifyInnerListEntry(shardB, 1, "one");

        LOG.info("{} ending", testName);
    }

    /**
     * Test 2 tx's accessing 2 shards, the second in common.
     * <pre>
     *   tx1 -> shard A, shard C
     *   tx2 -> shard B, shard C
     * </pre>
     * The tx's are readied such the pendingTransactions queue are as follows:
     * <pre>
     *   Queue for shard A -> tx1
     *   Queue for shard B -> tx2
     *   Queue for shard C -> tx2, tx1
     * </pre>
     * When the tx's re committed verify the ready order is preserved.
     */
    @Test
    public void testTwoTransactionsWithOneCommonParticipatingShard1() {
        final String testName = "testTwoTransactionsWithOneCommonParticipatingShard1";
        LOG.info("{} starting", testName);

        final TestKit kit1 = new TestKit(getSystem());
        final TestKit kit2 = new TestKit(getSystem());

        final ShardIdentifier shardAId = ShardIdentifier.create("shardA", MemberName.forName(testName), "config");
        final ShardIdentifier shardBId = ShardIdentifier.create("shardB", MemberName.forName(testName), "config");
        final ShardIdentifier shardCId = ShardIdentifier.create("shardC", MemberName.forName(testName), "config");

        final TestActorRef<Shard> shardA = actorFactory.createTestActor(
                newShardBuilder().id(shardAId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardA);

        final TestActorRef<Shard> shardB = actorFactory.createTestActor(
                newShardBuilder().id(shardBId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardB);

        final TestActorRef<Shard> shardC = actorFactory.createTestActor(
                newShardBuilder().id(shardCId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardC);

        final TransactionIdentifier txId1 = nextTransactionId();
        final TransactionIdentifier txId2 = nextTransactionId();

        SortedSet<String> participatingShardNames1 =
                ImmutableSortedSet.of(shardAId.getShardName(), shardCId.getShardName());
        SortedSet<String> participatingShardNames2 =
                ImmutableSortedSet.of(shardBId.getShardName(), shardCId.getShardName());

        // Ready [tx1] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard C.

        shardC.tell(newReadyBatchedModifications(txId2, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardC.tell(newReadyBatchedModifications(txId1, OUTER_LIST_PATH, outerNode(1),
                participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Send tx1 CanCommit to A - should succeed.

        shardA.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx2 CanCommit to B - should succeed.

        shardB.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to C - tx2 is at the head of the queue but the preceding shards in tx1's participating
        // shard list [A] do not match that of tx2 [B] so tx1 should not be allowed to proceed.

        shardC.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectNoMessage(Duration.ofMillis(100));

        // Send tx2 CanCommit to C - it's at the head of the queue so should proceed.

        shardC.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        // Finish commit of tx2.

        shardB.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        shardC.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx1.

        kit1.expectMsgClass(CanCommitTransactionReply.class);
        shardA.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        shardC.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        // Verify data in the data store.

        verifyOuterListEntry(shardC, 1);

        LOG.info("{} ending", testName);
    }

    /**
     * Test 2 tx's accessing 2 shards, the first for one and the second for the other in common.
     * <pre>
     *   tx1 -> shard A, shard B
     *   tx2 -> shard B, shard C
     * </pre>
     * The tx's are readied such the pendingTransactions queue are as follows:
     * <pre>
     *   Queue for shard A -> tx1
     *   Queue for shard B -> tx2, tx1
     *   Queue for shard C -> tx2
     * </pre>
     * When the tx's re committed verify the ready order is preserved.
     */
    @Test
    public void testTwoTransactionsWithOneCommonParticipatingShard2() {
        final String testName = "testTwoTransactionsWithOneCommonParticipatingShard2";
        LOG.info("{} starting", testName);

        final TestKit kit1 = new TestKit(getSystem());
        final TestKit kit2 = new TestKit(getSystem());

        final ShardIdentifier shardAId = ShardIdentifier.create("shardA", MemberName.forName(testName), "config");
        final ShardIdentifier shardBId = ShardIdentifier.create("shardB", MemberName.forName(testName), "config");
        final ShardIdentifier shardCId = ShardIdentifier.create("shardC", MemberName.forName(testName), "config");

        final TestActorRef<Shard> shardA = actorFactory.createTestActor(
                newShardBuilder().id(shardAId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardA);

        final TestActorRef<Shard> shardB = actorFactory.createTestActor(
                newShardBuilder().id(shardBId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardB);

        final TestActorRef<Shard> shardC = actorFactory.createTestActor(
                newShardBuilder().id(shardCId).props().withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shardC);

        final TransactionIdentifier txId1 = nextTransactionId();
        final TransactionIdentifier txId2 = nextTransactionId();

        SortedSet<String> participatingShardNames1 =
                ImmutableSortedSet.of(shardAId.getShardName(), shardBId.getShardName());
        SortedSet<String> participatingShardNames2 =
                ImmutableSortedSet.of(shardBId.getShardName(), shardCId.getShardName());

        // Ready [tx1] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId1, OUTER_LIST_PATH, outerNode(1),
                participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2] on shard C.

        shardC.tell(newReadyBatchedModifications(txId2, TEST_PATH,
                ImmutableNodes.containerNode(TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Send tx1 CanCommit to A - should succeed.

        shardA.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to B - tx2 is at the head of the queue but the preceding shards in tx1's participating
        // shard list [A] do not match that of tx2 [] so tx1 should not be allowed to proceed.

        shardB.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectNoMessage(Duration.ofMillis(100));

        // Send tx2 CanCommit to B - it's at the head of the queue so should proceed.

        shardB.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        // Finish commit of tx2.

        shardC.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CanCommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        shardC.tell(new CommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectMsgClass(CommitTransactionReply.class);

        // Finish commit of tx1.

        kit1.expectMsgClass(CanCommitTransactionReply.class);
        shardA.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        shardB.tell(new CommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CommitTransactionReply.class);

        // Verify data in the data store.

        verifyOuterListEntry(shardB, 1);

        LOG.info("{} ending", testName);
    }

    static void verifyInnerListEntry(TestActorRef<Shard> shard, int outerID, String innerID) {
        final YangInstanceIdentifier path = innerEntryPath(outerID, innerID);
        final NormalizedNode innerListEntry = readStore(shard, path);
        assertNotNull(path + " not found", innerListEntry);
    }
}
