/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;

/**
 * Abstract base for an integration test that tests end-to-end RaftActor and behavior functionality.
 *
 * @author Thomas Pantelis
 */
abstract class AbstractRaftActorIntegrationTest extends AbstractActorTest {
    private static final class MockIdentifier extends AbstractStringIdentifier<MockIdentifier> {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        protected MockIdentifier(final String string) {
            super(string);
        }
    }

    static class SetPeerAddress {
        private final String peerId;
        private final String peerAddress;

        SetPeerAddress(final String peerId, final String peerAddress) {
            this.peerId = peerId;
            this.peerAddress = peerAddress;
        }

        String getPeerId() {
            return peerId;
        }

        String getPeerAddress() {
            return peerAddress;
        }
    }

    /**
     * Message intended for testing to allow triggering persistData via the mailbox.
     */
    static final class TestPersist {
        private final ActorRef actorRef;
        private final @NonNull Identifier identifier;
        private final MockCommand payload;

        TestPersist(final ActorRef actorRef, final Identifier identifier, final MockCommand payload) {
            this.actorRef = actorRef;
            this.identifier = requireNonNull(identifier);
            this.payload = payload;
        }

        ActorRef getActorRef() {
            return actorRef;
        }

        @NonNull Identifier getIdentifier() {
            return identifier;
        }

        MockCommand getPayload() {
            return payload;
        }
    }

    static class TestRaftActor extends MockRaftActor {
        private final ConcurrentHashMap<Class<?>, Predicate<?>> dropMessages = new ConcurrentHashMap<>();
        private final MessageCollector collector;

        TestRaftActor(final Path stateDir, final Builder builder) {
            super(stateDir, builder);
            collector = builder.collector;
        }

        void startDropMessages(final Class<?> msgClass) {
            dropMessages.put(msgClass, msg -> true);
        }

        <T> void startDropMessages(final Class<T> msgClass, final Predicate<T> filter) {
            dropMessages.put(msgClass, filter);
        }

        void stopDropMessages(final Class<?> msgClass) {
            dropMessages.remove(msgClass);
        }

        void setMockTotalMemory(final long mockTotalMemory) {
            getRaftActorContext().setTotalMemoryRetriever(mockTotalMemory > 0 ? () -> mockTotalMemory : null);
        }

        @Override
        @Deprecated
        @SuppressWarnings("checkstyle:IllegalCatch")
        protected void handleCommandImpl(final Object message) {
            switch (message) {
                case MockCommand payload ->
                    submitCommand(new MockIdentifier(payload.toString()), payload, false);
                case VotingConfig payload ->
                    submitCommand(new MockIdentifier("serverConfig"), payload);
                case SetPeerAddress setPeerAddress ->
                    setPeerAddress(setPeerAddress.getPeerId(), setPeerAddress.getPeerAddress());
                case TestPersist testPersist ->
                    submitCommand(testPersist.getIdentifier(), testPersist.getPayload(), false);
                default -> {
                    try {
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        final Predicate<Object> drop = (Predicate) dropMessages.get(message.getClass());
                        if (drop == null || !drop.test(message)) {
                            super.handleCommandImpl(message);
                        }
                    } finally {
                        if (!(message instanceof SendHeartBeat)) {
                            try {
                                collector.actor().tell(message, ActorRef.noSender());
                            } catch (Exception e) {
                                throw new AssertionError(e);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public MockSnapshotState takeSnapshot() {
            return new MockSnapshotState(List.copyOf(getState()));
        }

        MessageCollector collector() {
            return collector;
        }

        static Builder newBuilder() {
            return new Builder();
        }

        static final class Builder extends AbstractBuilder<Builder, TestRaftActor> {
            private MessageCollector collector;

            private Builder() {
                super(TestRaftActor.class);
            }

            public Builder collector(final MessageCollector newCollector) {
                collector = newCollector;
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
    protected MessageCollector leaderCollector;
    protected RaftActorContext leaderContext;
    protected RaftActorBehavior leader;

    protected String follower1Id = factory.generateActorId("follower");
    protected TestActorRef<TestRaftActor> follower1Actor;
    protected MessageCollector follower1Collector;
    protected RaftActorBehavior follower1;
    protected RaftActorContext follower1Context;

    protected String follower2Id = factory.generateActorId("follower");
    protected TestActorRef<TestRaftActor> follower2Actor;
    protected MessageCollector follower2Collector;
    protected RaftActorBehavior follower2;
    protected RaftActorContext follower2Context;

    protected Map<String, String> peerAddresses;

    protected long initialTerm = 5;
    protected long currentTerm;

    protected int snapshotBatchCount = 4;
    protected int maximumMessageSliceSize = MAXIMUM_MESSAGE_SLICE_SIZE;

    protected List<MockCommand> expSnapshotState = new ArrayList<>();

    @AfterEach
    void afterEach() {
        factory.close();
    }

    protected final DefaultConfigParamsImpl newLeaderConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(Duration.ofMillis(100));
        configParams.setElectionTimeoutFactor(4);
        configParams.setSnapshotBatchCount(snapshotBatchCount);
        configParams.setSnapshotDataThresholdPercentage(70);
        configParams.setIsolatedLeaderCheckInterval(Duration.ofDays(1));
        configParams.setMaximumMessageSliceSize(maximumMessageSliceSize);
        return configParams;
    }

    protected static final DefaultConfigParamsImpl newFollowerConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(Duration.ofMillis(500));
        configParams.setElectionTimeoutFactor(1000);
        return configParams;
    }

    protected static final void waitUntilLeader(final ActorRef actorRef) {
        RaftActorTestKit.waitUntilLeader(actorRef);
    }

    protected final TestActorRef<TestRaftActor> newTestRaftActor(final String id,
            final Map<String, String> newPeerAddresses, final ConfigParams configParams) {
        return newTestRaftActor(id, TestRaftActor.newBuilder()
            .peerAddresses(newPeerAddresses != null ? newPeerAddresses : Map.of())
            .config(configParams));
    }

    protected final TestActorRef<TestRaftActor> newTestRaftActor(final String id, final TestRaftActor.Builder builder) {
        builder.collectorActor(factory.createActor(
                MessageCollectorActor.props(), factory.generateActorId(id + "-collector"))).id(id);

        InvalidActorNameException lastEx = null;
        for (int i = 0; i < 10; i++) {
            try {
                return factory.createTestActor(builder.props(stateDir())
                    .withDispatcher(Dispatchers.DefaultDispatcherId())
                    .withMailbox(Mailboxes.DefaultMailboxId()), id);
            } catch (InvalidActorNameException e) {
                lastEx = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
        }

        assertNotNull(lastEx);
        throw lastEx;
    }

    protected static final void killActor(final TestActorRef<TestRaftActor> actor) {
        TestKit testkit = new TestKit(getSystem());
        testkit.watch(actor);

        actor.tell(PoisonPill.getInstance(), null);
        testkit.expectMsgClass(Duration.ofSeconds(5), Terminated.class);

        testkit.unwatch(actor);
    }

    protected final void verifySnapshot(final String prefix, final SnapshotFile snapshotFile,
            final long lastAppliedTerm, final long lastAppliedIndex) throws Exception {
        assertEquals(EntryInfo.of(lastAppliedIndex, lastAppliedTerm), snapshotFile.lastIncluded());

        verifyState(prefix, snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader()));
    }

    protected final void verifySnapshot(final String prefix, final Snapshot snapshot, final long lastAppliedTerm,
            final long lastAppliedIndex, final long lastTerm, final long lastIndex) {
        assertEquals(prefix + " Snapshot getLastAppliedTerm", lastAppliedTerm, snapshot.getLastAppliedTerm());
        assertEquals(prefix + " Snapshot getLastAppliedIndex", lastAppliedIndex, snapshot.getLastAppliedIndex());
        assertEquals(prefix + " Snapshot getLastTerm", lastTerm, snapshot.getLastTerm());
        assertEquals(prefix + " Snapshot getLastIndex", lastIndex, snapshot.getLastIndex());

        verifyState(prefix, (MockSnapshotState) snapshot.state());
    }

    protected final void verifyState(final String prefix, final MockSnapshotState mockState) {
        final var actualState = mockState.state();
        assertEquals("%s Snapshot getState size. Expected %s: . Actual: %s".formatted(prefix, expSnapshotState,
            actualState), expSnapshotState.size(), actualState.size());
        for (int i = 0; i < expSnapshotState.size(); i++) {
            assertEquals(prefix + " Snapshot state " + i, expSnapshotState.get(i), actualState.get(i));
        }
    }

    protected static final void verifyPersistedJournal(final TestActorRef<TestRaftActor> actor,
            final List<? extends LogEntry> expJournal) {
        final var journal = actor.underlyingActor().entryJournal();
        assertNotNull(journal);

        final var actorJournal = new ArrayList<LogEntry>();
        try (var reader = journal.openReader()) {
            while (true) {
                final var entry = reader.nextEntry();
                if (entry == null) {
                    break;
                }
                actorJournal.add(entry.toLogEntry(OBJECT_STREAMS));
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        assertEquals("Journal ReplicatedLogEntry count", expJournal.size(), actorJournal.size());
        for (int i = 0; i < expJournal.size(); i++) {
            final var expected = expJournal.get(i);
            final var actual = actorJournal.get(i);
            verifyReplicatedLogEntry(expected, actual.term(), actual.index(), actual.command());
        }
    }

    protected static final MockCommand sendPayloadData(final ActorRef actor, final String data) {
        return sendPayloadData(actor, data, 0);
    }

    protected static final MockCommand sendPayloadData(final ActorRef actor, final String data, final int size) {
        MockCommand payload;
        if (size > 0) {
            payload = new MockCommand(data, size);
        } else {
            payload = new MockCommand(data);
        }

        actor.tell(payload, ActorRef.noSender());
        return payload;
    }

    protected static final void verifyApplyState(final ApplyState applyState, final ActorRef expClientActor,
            final String expId, final long expTerm, final long expIndex, final Payload payload) {
        final var id = expId == null ? null : new MockIdentifier(expId);
        assertEquals("ApplyState getIdentifier", id, applyState.identifier());
        verifyReplicatedLogEntry(applyState.entry(), expTerm, expIndex, payload);
    }

    protected static final void verifyReplicatedLogEntry(final LogEntry replicatedLogEntry, final long expTerm,
            final long expIndex, final StateMachineCommand payload) {
        assertEquals(expTerm, replicatedLogEntry.term());
        assertEquals(expIndex, replicatedLogEntry.index());
        assertEquals(payload, replicatedLogEntry.command());
    }

    protected final String testActorPath(final String id) {
        return factory.createTestActorPath(id);
    }

    protected final void verifyLeadersTrimmedLog(final long lastIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, lastIndex - 1);
    }

    protected final void verifyLeadersTrimmedLog(final long lastIndex, final long replicatedToAllIndex) {
        verifyTrimmedLog("Leader", leaderActor, lastIndex, replicatedToAllIndex);
    }

    protected final void verifyFollowersTrimmedLog(final int num, final TestActorRef<TestRaftActor> actorRef,
            final long lastIndex) {
        verifyTrimmedLog("Follower " + num, actorRef, lastIndex, lastIndex - 1);
    }

    protected final void verifyTrimmedLog(final String name, final TestActorRef<TestRaftActor> actorRef,
            final long lastIndex, final long replicatedToAllIndex) {
        TestRaftActor actor = actorRef.underlyingActor();
        RaftActorContext context = actor.getRaftActorContext();
        final var log = context.getReplicatedLog();
        long snapshotIndex = lastIndex - 1;
        assertEquals(name + " snapshot term", snapshotIndex < 0 ? -1 : currentTerm, log.getSnapshotTerm());
        assertEquals(name + " snapshot index", snapshotIndex, log.getSnapshotIndex());
        assertEquals(name + " journal log size", 1, log.size());
        assertEquals(name + " journal last index", lastIndex, log.lastIndex());
        assertEquals(name + " commit index", lastIndex, log.getCommitIndex());
        assertEquals(name + " last applied", lastIndex, log.getLastApplied());
        assertEquals(name + " replicatedToAllIndex", replicatedToAllIndex,
                actor.getCurrentBehavior().getReplicatedToAllIndex());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    static final void verifyRaftState(final ActorRef raftActor, final Consumer<OnDemandRaftState> verifier) {
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
