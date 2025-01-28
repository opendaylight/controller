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

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.InvalidActorNameException;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Terminated;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.dispatch.Mailboxes;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.junit.After;
import org.opendaylight.controller.cluster.raft.MockRaftActor.MockSnapshotState;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
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
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        protected MockIdentifier(final String string) {
            super(string);
        }
    }

    public static class SetPeerAddress {
        private final String peerId;
        private final String peerAddress;

        public SetPeerAddress(final String peerId, final String peerAddress) {
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

    /**
     * Message intended for testing to allow triggering persistData via the mailbox.
     */
    public static final class TestPersist {

        private final ActorRef actorRef;
        private final Identifier identifier;
        private final Payload payload;

        TestPersist(final ActorRef actorRef, final Identifier identifier, final Payload payload) {
            this.actorRef = actorRef;
            this.identifier = identifier;
            this.payload = payload;
        }

        public ActorRef getActorRef() {
            return actorRef;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public Payload getPayload() {
            return payload;
        }
    }

    public static class TestRaftActor extends MockRaftActor {

        private final ActorRef collectorActor;
        private final Map<Class<?>, Predicate<?>> dropMessages = new ConcurrentHashMap<>();

        TestRaftActor(final Builder builder) {
            super(builder);
            collectorActor = builder.collectorActor;
        }

        public void startDropMessages(final Class<?> msgClass) {
            dropMessages.put(msgClass, msg -> true);
        }

        <T> void startDropMessages(final Class<T> msgClass, final Predicate<T> filter) {
            dropMessages.put(msgClass, filter);
        }

        public void stopDropMessages(final Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(final long mockTotalMemory) {
            getRaftActorContext().setTotalMemoryRetriever(mockTotalMemory > 0 ? () -> mockTotalMemory : null);
        }

        @SuppressWarnings({ "rawtypes", "unchecked", "checkstyle:IllegalCatch" })
        @Override
        public void handleCommand(final Object message) {
            if (message instanceof MockPayload payload) {
                super.persistData(collectorActor, new MockIdentifier(payload.toString()), payload, false);
                return;
            }

            if (message instanceof ClusterConfig payload) {
                super.persistData(collectorActor, new MockIdentifier("serverConfig"), payload, false);
                return;
            }

            if (message instanceof SetPeerAddress setPeerAddress) {
                setPeerAddress(setPeerAddress.getPeerId(), setPeerAddress.getPeerAddress());
                return;
            }

            if (message instanceof TestPersist testPersist) {
                persistData(testPersist.getActorRef(), testPersist.getIdentifier(), testPersist.getPayload(), false);
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
                        throw new AssertionError(e);
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
            MockSnapshotState snapshotState = new MockSnapshotState(List.copyOf(getState()));
            if (installSnapshotStream.isPresent()) {
                SerializationUtils.serialize(snapshotState, installSnapshotStream.orElseThrow());
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
            private ActorRef collectorActor;

            Builder() {
                super(TestRaftActor.class);
            }

            public Builder collectorActor(final ActorRef newCollectorActor) {
                collectorActor = newCollectorActor;
                return this;
            }
        }
    }

    // FIXME: this is an arbitrary limit. Document interactions and/or improve them to improve maintainability
    protected static final int MAXIMUM_MESSAGE_SLICE_SIZE = 700;

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
    protected RaftActorBehavior follower2;
    protected RaftActorContext follower2Context;

    protected Map<String, String> peerAddresses;

    protected long initialTerm = 5;
    protected long currentTerm;

    protected int snapshotBatchCount = 4;
    protected int maximumMessageSliceSize = MAXIMUM_MESSAGE_SLICE_SIZE;

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
        configParams.setMaximumMessageSliceSize(maximumMessageSliceSize);
        return configParams;
    }

    protected DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(500, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(1000);
        return configParams;
    }

    protected void waitUntilLeader(final ActorRef actorRef) {
        RaftActorTestKit.waitUntilLeader(actorRef);
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(final String id, final Map<String, String> newPeerAddresses,
            final ConfigParams configParams) {
        return newTestRaftActor(id, TestRaftActor.newBuilder().baseDir(baseDir()).peerAddresses(newPeerAddresses != null
                ? newPeerAddresses : Map.of()).config(configParams));
    }

    protected TestActorRef<TestRaftActor> newTestRaftActor(final String id, final TestRaftActor.Builder builder) {
        builder.collectorActor(factory.createActor(
                MessageCollectorActor.props(), factory.generateActorId(id + "-collector"))).id(id);

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

    protected void killActor(final TestActorRef<TestRaftActor> actor) {
        TestKit testkit = new TestKit(getSystem());
        testkit.watch(actor);

        actor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(Duration.ofSeconds(5), Terminated.class);

        testkit.unwatch(actor);
    }

    protected void verifyApplyJournalEntries(final ActorRef actor, final long expIndex) {
        MessageCollectorActor.expectFirstMatching(actor, ApplyJournalEntries.class,
            msg -> msg.getToIndex() == expIndex);
    }

    protected void verifySnapshot(final String prefix, final Snapshot snapshot, final long lastAppliedTerm,
            final long lastAppliedIndex, final long lastTerm, final long lastIndex) {
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

    protected void verifyPersistedJournal(final String persistenceId,
            final List<? extends ReplicatedLogEntry> expJournal) {
        final var journal = InMemoryJournal.get(persistenceId, ReplicatedLogEntry.class);
        assertEquals("Journal ReplicatedLogEntry count", expJournal.size(), journal.size());
        for (int i = 0; i < expJournal.size(); i++) {
            final var expected = expJournal.get(i);
            final var actual = journal.get(i);
            verifyReplicatedLogEntry(expected, actual.term(), actual.index(), actual.getData());
        }
    }

    protected MockPayload sendPayloadData(final ActorRef actor, final String data) {
        return sendPayloadData(actor, data, 0);
    }

    protected MockPayload sendPayloadData(final ActorRef actor, final String data, final int size) {
        MockPayload payload;
        if (size > 0) {
            payload = new MockPayload(data, size);
        } else {
            payload = new MockPayload(data);
        }

        actor.tell(payload, ActorRef.noSender());
        return payload;
    }

    protected void verifyApplyState(final ApplyState applyState, final ActorRef expClientActor,
            final String expId, final long expTerm, final long expIndex, final Payload payload) {
        assertEquals("ApplyState getClientActor", expClientActor, applyState.getClientActor());

        final Identifier id = expId == null ? null : new MockIdentifier(expId);
        assertEquals("ApplyState getIdentifier", id, applyState.getIdentifier());
        ReplicatedLogEntry replicatedLogEntry = applyState.getReplicatedLogEntry();
        verifyReplicatedLogEntry(replicatedLogEntry, expTerm, expIndex, payload);
    }

    protected void verifyReplicatedLogEntry(final ReplicatedLogEntry replicatedLogEntry, final long expTerm,
            final long expIndex, final Payload payload) {
        assertEquals("ReplicatedLogEntry getTerm", expTerm, replicatedLogEntry.term());
        assertEquals("ReplicatedLogEntry getIndex", expIndex, replicatedLogEntry.index());
        assertEquals("ReplicatedLogEntry getData", payload, replicatedLogEntry.getData());
    }

    protected String testActorPath(final String id) {
        return factory.createTestActorPath(id);
    }

    protected void verifyLeadersTrimmedLog(final long lastIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, lastIndex - 1);
    }

    protected void verifyLeadersTrimmedLog(final long lastIndex, final long replicatedToAllIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, replicatedToAllIndex);
    }

    protected void verifyFollowersTrimmedLog(final int num, final TestActorRef<TestRaftActor> actorRef,
            final long lastIndex) {
        verifyTrimmedLog("Follower " + num, actorRef, lastIndex, lastIndex - 1);
    }

    protected void verifyTrimmedLog(final String name, final TestActorRef<TestRaftActor> actorRef, final long lastIndex,
            final long replicatedToAllIndex) {
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
    static void verifyRaftState(final ActorRef raftActor, final Consumer<OnDemandRaftState> verifier) {
        Timeout timeout = new Timeout(500, TimeUnit.MILLISECONDS);
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            try {
                OnDemandRaftState raftState = (OnDemandRaftState)Await.result(Patterns.ask(raftActor,
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
