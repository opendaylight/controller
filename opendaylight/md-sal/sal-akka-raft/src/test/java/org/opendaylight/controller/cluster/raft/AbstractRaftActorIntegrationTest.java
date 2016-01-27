/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import akka.actor.ActorRef;
import akka.actor.InvalidActorNameException;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
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

    public static class SetPeerAddress {
        private final String peerId;
        private final String peerAddress;

        public SetPeerAddress(String peerId, String peerAddress) {
            this.peerId = peerId;
            this.peerAddress = peerAddress;
        }

        public String getPeerId() {
            return peerId;
        }

        public String getPeerAddress() {
            return peerAddress;
        }
    }

    public static class TestRaftActor extends MockRaftActor {

        private final TestActorRef<MessageCollectorActor> collectorActor;
        private final Map<Class<?>, Boolean> dropMessages = new ConcurrentHashMap<>();

        private TestRaftActor(Builder builder) {
            super(builder);
            this.collectorActor = builder.collectorActor;
        }

        void startDropMessages(Class<?> msgClass) {
            dropMessages.put(msgClass, Boolean.TRUE);
        }

        void stopDropMessages(Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(final long mockTotalMemory) {
            if(mockTotalMemory > 0) {
                getRaftActorContext().setTotalMemoryRetriever(new Supplier<Long>() {
                    @Override
                    public Long get() {
                        return mockTotalMemory;
                    }

                });
            } else {
                getRaftActorContext().setTotalMemoryRetriever(null);
            }
        }

        @Override
        public void handleCommand(Object message) {
            if(message instanceof MockPayload) {
                MockPayload payload = (MockPayload)message;
                super.persistData(collectorActor, payload.toString(), payload);
                return;
            }

            if(message instanceof SetPeerAddress) {
                setPeerAddress(((SetPeerAddress) message).getPeerId().toString(),
                        ((SetPeerAddress) message).getPeerAddress());
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
        public void createSnapshot(ActorRef actorRef) {
            try {
                actorRef.tell(new CaptureSnapshotReply(RaftActorTest.fromObject(getState()).toByteArray()), actorRef);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public ActorRef collectorActor() {
            return collectorActor;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder extends AbstractBuilder<Builder, TestRaftActor> {
            private TestActorRef<MessageCollectorActor> collectorActor;

            public Builder collectorActor(TestActorRef<MessageCollectorActor> collectorActor) {
                this.collectorActor = collectorActor;
                return this;
            }

            private Builder() {
                super(TestRaftActor.class);
            }
        }
    }

    protected static final int SNAPSHOT_CHUNK_SIZE = 100;

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

    protected int snapshotBatchCount = 4;

    protected List<MockPayload> expSnapshotState = new ArrayList<>();

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
        configParams.setSnapshotBatchCount(snapshotBatchCount);
        configParams.setSnapshotDataThresholdPercentage(70);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        configParams.setSnapshotChunkSize(SNAPSHOT_CHUNK_SIZE);
        return configParams;
    }

    protected DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1000);
        return configParams;
    }

    protected void waitUntilLeader(ActorRef actorRef) {
        RaftActorTestKit.waitUntilLeader(actorRef);
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(String id, Map<String, String> peerAddresses,
            ConfigParams configParams) {
        return newTestRaftActor(id, TestRaftActor.newBuilder().peerAddresses(peerAddresses != null ? peerAddresses :
            Collections.<String, String>emptyMap()).config(configParams));
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(String id, TestRaftActor.Builder builder) {
        builder.collectorActor(factory.<MessageCollectorActor>createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        factory.generateActorId(id + "-collector"))).id(id);

        InvalidActorNameException lastEx = null;
        for(int i = 0; i < 10; i++) {
            try {
                return factory.createTestActor(builder.props().withDispatcher(Dispatchers.DefaultDispatcherId()), id);
            } catch (InvalidActorNameException e) {
                lastEx = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        assertNotNull(lastEx);
        throw lastEx;
    }

    protected void killActor(TestActorRef<TestRaftActor> leaderActor) {
        JavaTestKit testkit = new JavaTestKit(getSystem());
        testkit.watch(leaderActor);

        leaderActor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        testkit.unwatch(leaderActor);
    }

    protected void verifyApplyJournalEntries(ActorRef actor, final long expIndex) {
        MessageCollectorActor.expectFirstMatching(actor, ApplyJournalEntries.class, new Predicate<ApplyJournalEntries>() {
            @Override
            public boolean apply(ApplyJournalEntries msg) {
                return msg.getToIndex() == expIndex;
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void verifySnapshot(String prefix, Snapshot snapshot, long lastAppliedTerm,
            long lastAppliedIndex, long lastTerm, long lastIndex)
                    throws Exception {
        assertEquals(prefix + " Snapshot getLastAppliedTerm", lastAppliedTerm, snapshot.getLastAppliedTerm());
        assertEquals(prefix + " Snapshot getLastAppliedIndex", lastAppliedIndex, snapshot.getLastAppliedIndex());
        assertEquals(prefix + " Snapshot getLastTerm", lastTerm, snapshot.getLastTerm());
        assertEquals(prefix + " Snapshot getLastIndex", lastIndex, snapshot.getLastIndex());

        List<Object> actualState = (List<Object>)MockRaftActor.toObject(snapshot.getState());
        assertEquals(String.format("%s Snapshot getState size. Expected %s: . Actual: %s", prefix, expSnapshotState,
                actualState), expSnapshotState.size(), actualState.size());
        for(int i = 0; i < expSnapshotState.size(); i++) {
            assertEquals(prefix + " Snapshot state " + i, expSnapshotState.get(i), actualState.get(i));
        }
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

    protected String testActorPath(String id){
        return factory.createTestActorPath(id);
    }

    protected void verifyLeadersTrimmedLog(long lastIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, lastIndex - 1);
    }

    protected void verifyLeadersTrimmedLog(long lastIndex, long replicatedToAllIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, replicatedToAllIndex);
    }

    protected void verifyFollowersTrimmedLog(int num, TestActorRef<TestRaftActor> actorRef, long lastIndex) {
        verifyTrimmedLog("Follower " + num, actorRef, lastIndex, lastIndex - 1);
    }

    protected void verifyTrimmedLog(String name, TestActorRef<TestRaftActor> actorRef, long lastIndex,
            long replicatedToAllIndex) {
        TestRaftActor actor = actorRef.underlyingActor();
        RaftActorContext context = actor.getRaftActorContext();
        long snapshotIndex = lastIndex - 1;
        assertEquals(name + " snapshot term", snapshotIndex < 0 ? -1 : currentTerm,
                context.getReplicatedLog().getSnapshotTerm());
        assertEquals(name + " snapshot index", snapshotIndex, context.getReplicatedLog().getSnapshotIndex());
        assertEquals(name + " journal log size", 1, context.getReplicatedLog().size());
        assertEquals(name + " journal last index", lastIndex, context.getReplicatedLog().lastIndex());
        assertEquals(name + " commit index", lastIndex, context.getCommitIndex());
        assertEquals(name + " last applied", lastIndex, context.getLastApplied());
        assertEquals(name + " replicatedToAllIndex", replicatedToAllIndex,
                actor.getCurrentBehavior().getReplicatedToAllIndex());
    }
}
