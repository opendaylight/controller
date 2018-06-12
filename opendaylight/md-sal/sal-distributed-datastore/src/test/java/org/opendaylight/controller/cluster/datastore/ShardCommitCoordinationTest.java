/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;

import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import akka.testkit.javadsl.TestKit;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

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
    public void testTwoTransactionsWithSameTwoParticipatingShards() throws Exception {
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

        List<String> participatingShardNames = ImmutableList.of(shardAId.getShardName(), shardBId.getShardName());

        // Ready [tx1, tx2] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        shardA.tell(newReadyBatchedModifications(txId2, TestModel.OUTER_LIST_PATH, TestModel.outerNode(1),
                participatingShardNames), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, TestModel.OUTER_LIST_PATH, TestModel.outerNode(1),
                participatingShardNames), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Send tx2 CanCommit to A - tx1 is at the head of the queue so tx2 should not proceed as A is the first shard
        // in the participating shard list.

        shardA.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectNoMessage(FiniteDuration.create(100, TimeUnit.MILLISECONDS));

        // Send tx1 CanCommit to A - it's at the head of the queue so should proceed.

        shardA.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to B - tx2 is at the head of the queue but the preceding shards in tx1's participating
        // shard list [A] matches that of tx2 [A] so tx1 should be de-queued and allowed to proceed.

        shardB.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx2 CanCommit to B - tx1 should now be at the head of he queue.

        shardB.tell(new CanCommitTransaction(txId2, CURRENT_VERSION).toSerializable(), kit2.getRef());
        kit2.expectNoMessage(FiniteDuration.create(100, TimeUnit.MILLISECONDS));

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
    public void testTwoTransactionsWithOneCommonParticipatingShard1() throws Exception {
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

        List<String> participatingShardNames1 = ImmutableList.of(shardAId.getShardName(), shardCId.getShardName());
        List<String> participatingShardNames2 = ImmutableList.of(shardBId.getShardName(), shardCId.getShardName());

        // Ready [tx1] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard C.

        shardC.tell(newReadyBatchedModifications(txId2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardC.tell(newReadyBatchedModifications(txId1, TestModel.OUTER_LIST_PATH, TestModel.outerNode(1),
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
        kit1.expectNoMessage(FiniteDuration.create(100, TimeUnit.MILLISECONDS));

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
    public void testTwoTransactionsWithOneCommonParticipatingShard2() throws Exception {
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

        List<String> participatingShardNames1 = ImmutableList.of(shardAId.getShardName(), shardBId.getShardName());
        List<String> participatingShardNames2 = ImmutableList.of(shardBId.getShardName(), shardCId.getShardName());

        // Ready [tx1] on shard A.

        shardA.tell(newReadyBatchedModifications(txId1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2, tx1] on shard B.

        shardB.tell(newReadyBatchedModifications(txId2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        shardB.tell(newReadyBatchedModifications(txId1, TestModel.OUTER_LIST_PATH, TestModel.outerNode(1),
                participatingShardNames1), kit1.getRef());
        kit1.expectMsgClass(ReadyTransactionReply.class);

        // Ready [tx2] on shard C.

        shardC.tell(newReadyBatchedModifications(txId2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), participatingShardNames2), kit2.getRef());
        kit2.expectMsgClass(ReadyTransactionReply.class);

        // Send tx1 CanCommit to A - should succeed.

        shardA.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectMsgClass(CanCommitTransactionReply.class);

        // Send tx1 CanCommit to B - tx2 is at the head of the queue but the preceding shards in tx1's participating
        // shard list [A] do not match that of tx2 [] so tx1 should not be allowed to proceed.

        shardB.tell(new CanCommitTransaction(txId1, CURRENT_VERSION).toSerializable(), kit1.getRef());
        kit1.expectNoMessage(FiniteDuration.create(100, TimeUnit.MILLISECONDS));

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
}
