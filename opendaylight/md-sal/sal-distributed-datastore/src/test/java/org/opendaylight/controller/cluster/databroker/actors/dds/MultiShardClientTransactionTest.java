/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.apache.pekko.actor.ActorRef.noSender;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.serialization.Serialization;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.ModuleConfig;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.shardmanager.RegisterForShardAvailabilityChanges;
import org.opendaylight.controller.cluster.datastore.shardstrategy.DefaultShardStrategy;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test multi-shard coordination interactions enforced via {@link ClientTransaction}.
 */
class MultiShardClientTransactionTest {
    private static final class MockShardStrategy implements ShardStrategy {
        @Override
        public String findShard(final YangInstanceIdentifier path) {
            if (!path.isEmpty()) {
                final var qname = path.getPathArguments().getFirst().getNodeType();
                if (TestModel.TEST_QNAME.equals(qname)) {
                    return "shardA";
                }
                if (TestModel.TEST2_QNAME.equals(qname)) {
                    return "shardB";
                }
            }
            return DefaultShardStrategy.DEFAULT_SHARD;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MultiShardClientTransactionTest.class);

    // FIXME: AbstractActorTest
    private static ActorSystem system;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");
    }

    @AfterAll
    static void afterAll() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

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
    void twoTransactionsWithSameTwoParticipants() {
        final var testName = "testTwoTransactionsWithSameTwoParticipatingShards";
        try {
            LOG.info("{} starting", testName);

            // Common setup
            final var memberName = MemberName.forName(testName);
            final var shardManager = new TestKit(system);
            final var configuration = new ConfigurationImpl(unused -> Map.of(
                "unknown", ModuleConfig.builder("unknown").shardStrategy(new MockShardStrategy())));
            final var actorUtils = new ActorUtils(system, shardManager.getRef(), new MockClusterWrapper(memberName),
                configuration);
            final var clientActor = system.actorOf(
                DistributedDataStoreClientActor.props(memberName, actorUtils.getDataStoreName(), actorUtils));
            final var client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor,
                30, TimeUnit.SECONDS);

            // assert startup message
            final var shardAvailReg = shardManager.expectMsgClass(Duration.ofSeconds(1),
                RegisterForShardAvailabilityChanges.class);

            final var idA = ShardIdentifier.create("shardA", memberName, "config");
            final var idB = ShardIdentifier.create("shardB", memberName, "config");
            final var shardA = new TestKit(system);
            final var shardB = new TestKit(system);

//            shardAvailReg.getCallback().accept(idA.getShardName());
//            shardAvailReg.getCallback().accept(idB.getShardName());

            // create transactions and issue writes, triggering shard resolution
            final var tx1 = client.createTransaction();
            final var txId1 = tx1.getIdentifier();
            tx1.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);

            final var tx2 = client.createTransaction();
            final var txId2 = tx2.getIdentifier();
            tx2.write(TestModel.TEST2_PATH, TestModel.EMPTY_TEST2);

            final var firstFind = shardManager.expectMsgClass(Duration.ofSeconds(1), FindPrimary.class);
            assertEquals(idA.getShardName(), firstFind.getShardName());
            final var findA = shardManager.getLastSender();
            final var secondFind = shardManager.expectMsgClass(Duration.ofSeconds(1), FindPrimary.class);
            assertEquals(idB.getShardName(), secondFind.getShardName());
            final var findB = shardManager.getLastSender();

            // order of replies assigns cookies:
            // 1 = B
            // 2 = A
            findB.tell(new RemotePrimaryShardFound(
                Serialization.serializedActorPath(shardB.getRef()), DataStoreVersions.CURRENT_VERSION), noSender());
            findA.tell(new RemotePrimaryShardFound(
                Serialization.serializedActorPath(shardA.getRef()), DataStoreVersions.CURRENT_VERSION), noSender());

            final var connectB = shardB.expectMsgClass(Duration.ofSeconds(1), ConnectClientRequest.class);
            final var clientB = connectB.getTarget();
            connectB.getReplyTo().tell(new ConnectClientSuccess(clientB, 0, shardA.getRef(), List.of(), 10, null),
                noSender());

            final var connectA = shardA.expectMsgClass(Duration.ofSeconds(1), ConnectClientRequest.class);
            final var clientA = connectA.getTarget();
            connectA.getReplyTo().tell(new ConnectClientSuccess(clientA, 0, shardB.getRef(), List.of(), 10, null),
                noSender());

            // Both transactions talk to both shards
            tx1.write(TestModel.TEST2_PATH, TestModel.EMPTY_TEST2);
            tx2.write(TestModel.OUTER_LIST_PATH, TestModel.outerNode(1));

            // ready transactions
            final var ready1 = tx1.ready();
            final var can1 = ready1.canCommit();
            final var can1a = shardA.expectMsgClass(RequestEnvelope.class);
            var mod = assertInstanceOf(ModifyTransactionRequest.class, can1a.getMessage());
            assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), mod.getPersistenceProtocol());
            assertEquals(0, mod.getTarget().getTransactionId());
            assertEquals(2, mod.getTarget().getHistoryId().getCookie());

            final var can1b = shardB.expectMsgClass(RequestEnvelope.class);
            mod = assertInstanceOf(ModifyTransactionRequest.class, can1b.getMessage());
            assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), mod.getPersistenceProtocol());
            assertEquals(0, mod.getTarget().getTransactionId());
            assertEquals(1, mod.getTarget().getHistoryId().getCookie());


            final var ready2 = tx2.ready();
            final var can2 = ready2.canCommit();
            final var can2a = shardA.expectMsgClass(RequestEnvelope.class);
            mod = assertInstanceOf(ModifyTransactionRequest.class, can2a.getMessage());
            assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), mod.getPersistenceProtocol());
            assertEquals(1, mod.getTarget().getTransactionId());
            assertEquals(2, mod.getTarget().getHistoryId().getCookie());

            final var can2b = shardB.expectMsgClass(RequestEnvelope.class);
            mod = assertInstanceOf(ModifyTransactionRequest.class, can2b.getMessage());
            assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), mod.getPersistenceProtocol());
            assertEquals(1, mod.getTarget().getTransactionId());
            assertEquals(1, mod.getTarget().getHistoryId().getCookie());



            // FIXME: finish this
            shardA.expectNoMessage();
            shardB.expectNoMessage();
        } finally {
            LOG.info("{} ending", testName);
        }
    }
}
