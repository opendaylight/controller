/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.RaftActorTest.MockRaftActor;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyLogEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base for an integration test that tests end-to-end RaftActor and behavior functionality.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractRaftActorIntegrationTest extends AbstractActorTest {

    public static class TestRaftActor extends MockRaftActor {

        private final TestActorRef<MessageCollectorActor> collectorActor;
        private final Map<Class<?>, Boolean> dropMessages = new ConcurrentHashMap<>();
        private volatile byte[] snapshot;
        private volatile long mockTotalMemory;

        private TestRaftActor(String id, Map<String, String> peerAddresses, ConfigParams config,
                TestActorRef<MessageCollectorActor> collectorActor) {
            super(id, peerAddresses, Optional.of(config), null);
            dataPersistenceProvider = new PersistentDataProvider();
            this.collectorActor = collectorActor;
        }

        public static Props props(String id, Map<String, String> peerAddresses, ConfigParams config,
                TestActorRef<MessageCollectorActor> collectorActor) {
            return Props.create(TestRaftActor.class, id, peerAddresses, config, collectorActor).
                    withDispatcher(Dispatchers.DefaultDispatcherId());
        }

        void startDropMessages(Class<?> msgClass) {
            dropMessages.put(msgClass, Boolean.TRUE);
        }

        void stopDropMessages(Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(long mockTotalMemory) {
            this.mockTotalMemory = mockTotalMemory;
        }

        @Override
        protected long getTotalMemory() {
            return mockTotalMemory > 0 ? mockTotalMemory : super.getTotalMemory();
        }

        @Override
        public void handleCommand(Object message) {
            if(message instanceof MockPayload) {
                MockPayload payload = (MockPayload)message;
                super.persistData(collectorActor, payload.toString(), payload);
                return;
            }

            try {
                if(!dropMessages.containsKey(message.getClass())) {
                    super.handleCommand(message);
                }
            } finally {
                if(!(message instanceof SendHeartBeat)) {
                    try {
                        collectorActor.tell(message, ActorRef.noSender());
                    } catch (Exception e) {
                        LOG.error("MessageCollectorActor error", e);
                    }
                }
            }
        }

        @Override
        protected void createSnapshot() {
            if(snapshot != null) {
                getSelf().tell(new CaptureSnapshotReply(snapshot), ActorRef.noSender());
            }
        }

        @Override
        protected void applyRecoverySnapshot(byte[] bytes) {
        }

        void setSnapshot(byte[] snapshot) {
            this.snapshot = snapshot;
        }

        public ActorRef collectorActor() {
            return collectorActor;
        }
    }

    protected final Logger testLog = LoggerFactory.getLogger(getClass());

    protected final TestActorFactory factory = new TestActorFactory(getSystem());

    protected String leaderId = factory.generateActorId("leader");
    protected DefaultConfigParamsImpl leaderConfigParams;
    protected TestActorRef<TestRaftActor> leaderActor;
    protected ActorRef leaderCollectorActor;
    protected RaftActorContext leaderContext;
    protected RaftActorBehavior leader;

    protected String follower1Id = factory.generateActorId("follower");
    protected TestActorRef<TestRaftActor> follower1Actor;
    protected ActorRef follower1CollectorActor;
    protected RaftActorBehavior follower1;
    protected RaftActorContext follower1Context;

    protected String follower2Id = factory.generateActorId("follower");
    protected TestActorRef<TestRaftActor> follower2Actor;
    protected ActorRef follower2CollectorActor;
    protected  RaftActorBehavior follower2;
    protected RaftActorContext follower2Context;

    protected ImmutableMap<String, String> peerAddresses;

    protected long initialTerm = 5;
    protected long currentTerm;

    @After
    public void tearDown() {
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
        factory.close();
    }

    protected DefaultConfigParamsImpl newLeaderConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1);
        configParams.setSnapshotBatchCount(4);
        configParams.setSnapshotDataThresholdPercentage(70);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        return configParams;
    }

    protected DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1000);
        return configParams;
    }

    protected void waitUntilLeader(ActorRef actorRef) {
        RaftActorTest.RaftActorTestKit.waitUntilLeader(actorRef);
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(String id, Map<String, String> peerAddresses,
            ConfigParams configParams) {
        TestActorRef<MessageCollectorActor> collectorActor = factory.createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        factory.generateActorId(id + "-collector"));
        return factory.createTestActor(TestRaftActor.props(id,
                peerAddresses != null ? peerAddresses : Collections.<String, String>emptyMap(),
                        configParams, collectorActor), id);
    }

    protected void killActor(TestActorRef<TestRaftActor> leaderActor) {
        JavaTestKit testkit = new JavaTestKit(getSystem());
        testkit.watch(leaderActor);

        leaderActor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        testkit.unwatch(leaderActor);
    }

    protected void verifyApplyLogEntry(ActorRef actor, final int expIndex) {
        MessageCollectorActor.expectFirstMatching(actor, ApplyLogEntries.class, new Predicate<ApplyLogEntries>() {
            @Override
            public boolean apply(ApplyLogEntries msg) {
                return msg.getToIndex() == expIndex;
            }
        });
    }

    protected void verifySnapshot(String prefix, Snapshot snapshot, long lastAppliedTerm,
            int lastAppliedIndex, long lastTerm, long lastIndex, byte[] data) {
        assertEquals(prefix + " Snapshot getLastAppliedTerm", lastAppliedTerm, snapshot.getLastAppliedTerm());
        assertEquals(prefix + " Snapshot getLastAppliedIndex", lastAppliedIndex, snapshot.getLastAppliedIndex());
        assertEquals(prefix + " Snapshot getLastTerm", lastTerm, snapshot.getLastTerm());
        assertEquals(prefix + " Snapshot getLastIndex", lastIndex, snapshot.getLastIndex());
        assertArrayEquals(prefix + " Snapshot getState", data, snapshot.getState());
    }

    protected void verifyPersistedJournal(String persistenceId, List<? extends ReplicatedLogEntry> expJournal) {
        List<ReplicatedLogEntry> journal = InMemoryJournal.get(persistenceId, ReplicatedLogEntry.class);
        assertEquals("Journal ReplicatedLogEntry count", expJournal.size(), journal.size());
        for(int i = 0; i < expJournal.size(); i++) {
            ReplicatedLogEntry expected = expJournal.get(i);
            ReplicatedLogEntry actual = journal.get(i);
            verifyReplicatedLogEntry(expected, actual.getTerm(), actual.getIndex(), actual.getData());
        }
    }

    protected MockPayload sendPayloadData(ActorRef leaderActor, String data) {
        return sendPayloadData(leaderActor, data, 0);
    }

    protected MockPayload sendPayloadData(ActorRef leaderActor, String data, int size) {
        MockPayload payload;
        if(size > 0) {
            payload = new MockPayload(data, size);
        } else {
            payload = new MockPayload(data);
        }

        leaderActor.tell(payload, ActorRef.noSender());
        return payload;
    }

    protected void verifyApplyState(ApplyState applyState, ActorRef expClientActor,
            String expId, long expTerm, long expIndex, MockPayload payload) {
        assertEquals("ApplyState getClientActor", expClientActor, applyState.getClientActor());
        assertEquals("ApplyState getIdentifier", expId, applyState.getIdentifier());
        ReplicatedLogEntry replicatedLogEntry = applyState.getReplicatedLogEntry();
        verifyReplicatedLogEntry(replicatedLogEntry, expTerm, expIndex, payload);
    }

    protected void verifyReplicatedLogEntry(ReplicatedLogEntry replicatedLogEntry, long expTerm, long expIndex,
            Payload payload) {
        assertEquals("ReplicatedLogEntry getTerm", expTerm, replicatedLogEntry.getTerm());
        assertEquals("ReplicatedLogEntry getIndex", expIndex, replicatedLogEntry.getIndex());
        assertEquals("ReplicatedLogEntry getData", payload, replicatedLogEntry.getData());
    }
}
