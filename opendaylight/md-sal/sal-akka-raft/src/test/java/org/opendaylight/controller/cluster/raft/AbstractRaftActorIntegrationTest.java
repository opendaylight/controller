/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static akka.pattern.Patterns.ask;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import akka.actor.ActorRef;
import akka.actor.InvalidActorNameException;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.dispatch.Mailboxes;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.opendaylight.controller.cluster.raft.MockRaftActor.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base for an integration test that tests end-to-end RaftActor and behavior functionality.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractRaftActorIntegrationTest extends AbstractActorTest {

    private static final class MockIdentifier extends AbstractStringIdentifier<MockIdentifier> {
        private static final long serialVersionUID = 1L;

        protected MockIdentifier(String string) {
            super(string);
        }
    }

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
        private final Map<Class<?>, Predicate<?>> dropMessages = new ConcurrentHashMap<>();

        private TestRaftActor(Builder builder) {
            super(builder);
            this.collectorActor = builder.collectorActor;
        }

        public void startDropMessages(Class<?> msgClass) {
            dropMessages.put(msgClass, msg -> true);
        }

        <T> void startDropMessages(Class<T> msgClass, Predicate<T> filter) {
            dropMessages.put(msgClass, filter);
        }

        public void stopDropMessages(Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(final long mockTotalMemory) {
            getRaftActorContext().setTotalMemoryRetriever(mockTotalMemory > 0 ? () -> mockTotalMemory : null);
        }

        @SuppressWarnings({ "rawtypes", "unchecked", "checkstyle:IllegalCatch" })
        @Override
        public void handleCommand(Object message) {
            if (message instanceof MockPayload) {
                MockPayload payload = (MockPayload) message;
                super.persistData(collectorActor, new MockIdentifier(payload.toString()), payload, false);
                return;
            }

            if (message instanceof ServerConfigurationPayload) {
                super.persistData(collectorActor, new MockIdentifier("serverConfig"), (Payload) message, false);
                return;
            }

            if (message instanceof SetPeerAddress) {
                setPeerAddress(((SetPeerAddress) message).getPeerId().toString(),
                        ((SetPeerAddress) message).getPeerAddress());
                return;
            }

            try {
                Predicate drop = dropMessages.get(message.getClass());
                if (drop == null || !drop.test(message)) {
                    super.handleCommand(message);
                }
            } finally {
                if (!(message instanceof SendHeartBeat)) {
                    try {
                        collectorActor.tell(message, ActorRef.noSender());
                    } catch (Exception e) {
                        LOG.error("MessageCollectorActor error", e);
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void createSnapshot(ActorRef actorRef, Optional<OutputStream> installSnapshotStream) {
            MockSnapshotState snapshotState = new MockSnapshotState(new ArrayList<>(getState()));
            if (installSnapshotStream.isPresent()) {
                SerializationUtils.serialize(snapshotState, installSnapshotStream.get());
            }

            actorRef.tell(new CaptureSnapshotReply(snapshotState, installSnapshotStream), actorRef);
        }

        public ActorRef collectorActor() {
            return collectorActor;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder extends AbstractBuilder<Builder, TestRaftActor> {
            private TestActorRef<MessageCollectorActor> collectorActor;

            public Builder collectorActor(TestActorRef<MessageCollectorActor> newCollectorActor) {
                this.collectorActor = newCollectorActor;
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
    protected int snapshotChunkSize = SNAPSHOT_CHUNK_SIZE;

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
        configParams.setElectionTimeoutFactor(4);
        configParams.setSnapshotBatchCount(snapshotBatchCount);
        configParams.setSnapshotDataThresholdPercentage(70);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        configParams.setSnapshotChunkSize(snapshotChunkSize);
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

    protected TestActorRef<TestRaftActor> newTestRaftActor(String id, Map<String, String> newPeerAddresses,
            ConfigParams configParams) {
        return newTestRaftActor(id, TestRaftActor.newBuilder().peerAddresses(newPeerAddresses != null
                ? newPeerAddresses : Collections.<String, String>emptyMap()).config(configParams));
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(String id, TestRaftActor.Builder builder) {
        builder.collectorActor(factory.<MessageCollectorActor>createTestActor(
                MessageCollectorActor.props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        factory.generateActorId(id + "-collector"))).id(id);

        InvalidActorNameException lastEx = null;
        for (int i = 0; i < 10; i++) {
            try {
                return factory.createTestActor(builder.props().withDispatcher(Dispatchers.DefaultDispatcherId())
                        .withMailbox(Mailboxes.DefaultMailboxId()), id);
            } catch (InvalidActorNameException e) {
                lastEx = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        assertNotNull(lastEx);
        throw lastEx;
    }

    protected void killActor(TestActorRef<TestRaftActor> actor) {
        JavaTestKit testkit = new JavaTestKit(getSystem());
        testkit.watch(actor);

        actor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(JavaTestKit.duration("5 seconds"), Terminated.class);

        testkit.unwatch(actor);
    }

    protected void verifyApplyJournalEntries(ActorRef actor, final long expIndex) {
        MessageCollectorActor.expectFirstMatching(actor, ApplyJournalEntries.class,
            msg -> msg.getToIndex() == expIndex);
    }

    protected void verifySnapshot(String prefix, Snapshot snapshot, long lastAppliedTerm,
            long lastAppliedIndex, long lastTerm, long lastIndex)
                    throws Exception {
        assertEquals(prefix + " Snapshot getLastAppliedTerm", lastAppliedTerm, snapshot.getLastAppliedTerm());
        assertEquals(prefix + " Snapshot getLastAppliedIndex", lastAppliedIndex, snapshot.getLastAppliedIndex());
        assertEquals(prefix + " Snapshot getLastTerm", lastTerm, snapshot.getLastTerm());
        assertEquals(prefix + " Snapshot getLastIndex", lastIndex, snapshot.getLastIndex());

        List<Object> actualState = ((MockSnapshotState)snapshot.getState()).getState();
        assertEquals(String.format("%s Snapshot getState size. Expected %s: . Actual: %s", prefix, expSnapshotState,
                actualState), expSnapshotState.size(), actualState.size());
        for (int i = 0; i < expSnapshotState.size(); i++) {
            assertEquals(prefix + " Snapshot state " + i, expSnapshotState.get(i), actualState.get(i));
        }
    }

    protected void verifyPersistedJournal(String persistenceId, List<? extends ReplicatedLogEntry> expJournal) {
        List<ReplicatedLogEntry> journal = InMemoryJournal.get(persistenceId, ReplicatedLogEntry.class);
        assertEquals("Journal ReplicatedLogEntry count", expJournal.size(), journal.size());
        for (int i = 0; i < expJournal.size(); i++) {
            ReplicatedLogEntry expected = expJournal.get(i);
            ReplicatedLogEntry actual = journal.get(i);
            verifyReplicatedLogEntry(expected, actual.getTerm(), actual.getIndex(), actual.getData());
        }
    }

    protected MockPayload sendPayloadData(ActorRef actor, String data) {
        return sendPayloadData(actor, data, 0);
    }

    protected MockPayload sendPayloadData(ActorRef actor, String data, int size) {
        MockPayload payload;
        if (size > 0) {
            payload = new MockPayload(data, size);
        } else {
            payload = new MockPayload(data);
        }

        actor.tell(payload, ActorRef.noSender());
        return payload;
    }

    protected void verifyApplyState(ApplyState applyState, ActorRef expClientActor,
            String expId, long expTerm, long expIndex, Payload payload) {
        assertEquals("ApplyState getClientActor", expClientActor, applyState.getClientActor());

        final Identifier id = expId == null ? null : new MockIdentifier(expId);
        assertEquals("ApplyState getIdentifier", id, applyState.getIdentifier());
        ReplicatedLogEntry replicatedLogEntry = applyState.getReplicatedLogEntry();
        verifyReplicatedLogEntry(replicatedLogEntry, expTerm, expIndex, payload);
    }

    protected void verifyReplicatedLogEntry(ReplicatedLogEntry replicatedLogEntry, long expTerm, long expIndex,
            Payload payload) {
        assertEquals("ReplicatedLogEntry getTerm", expTerm, replicatedLogEntry.getTerm());
        assertEquals("ReplicatedLogEntry getIndex", expIndex, replicatedLogEntry.getIndex());
        assertEquals("ReplicatedLogEntry getData", payload, replicatedLogEntry.getData());
    }

    protected String testActorPath(String id) {
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

    @SuppressWarnings("checkstyle:IllegalCatch")
    static void verifyRaftState(ActorRef raftActor, Consumer<OnDemandRaftState> verifier) {
        Timeout timeout = new Timeout(500, TimeUnit.MILLISECONDS);
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            try {
                OnDemandRaftState raftState = (OnDemandRaftState)Await.result(ask(raftActor,
                        GetOnDemandRaftState.INSTANCE, timeout), timeout.duration());
                verifier.accept(raftState);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                lastError = new AssertionError("OnDemandRaftState failed", e);
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }
}
