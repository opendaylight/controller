/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.TestTermInfoStore;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.spi.RaftPolicy;

public class MockRaftActorContext extends RaftActorContextImpl {
    private ActorSystem system;
    private RaftPolicy raftPolicy;

    @NonNullByDefault
    private static LocalAccess newLocalAccess(final String id, final Path stateDir) {
        return new LocalAccess(id, stateDir, new TestTermInfoStore(1, ""));
    }

    public MockRaftActorContext(final Path stateDir, final int payloadVersion) {
        super(null, null, newLocalAccess("test", stateDir), new HashMap<>(), new DefaultConfigParamsImpl(),
            (short) payloadVersion, AbstractActorTest.OBJECT_STREAMS, new TestPersistenceProvider(),
            (identifier, entry) -> { });
        resetReplicatedLog(new Builder().build());
    }

    @NonNullByDefault
    public MockRaftActorContext(final String id, final Path stateDir, final ActorSystem system, final ActorRef actor,
            final int payloadVersion) {
        super(actor, null, newLocalAccess(id, stateDir), new HashMap<>(), new DefaultConfigParamsImpl(),
            (short) payloadVersion, AbstractActorTest.OBJECT_STREAMS, new TestPersistenceProvider(),
            (identifier, entry) -> actor.tell(new ApplyState(identifier, entry), actor));
        this.system = system;
        initReplicatedLog();
    }

    @NonNullByDefault
    public MockRaftActorContext(final Path stateDir) {
        this(stateDir, 0);
    }

    @NonNullByDefault
    public MockRaftActorContext(final String id, final Path stateDir, final ActorSystem system, final ActorRef actor) {
        this(id, stateDir, system, actor, 0);
    }

    public void initReplicatedLog() {
        final var replicatedLog = new SimpleReplicatedLog();
        long term = currentTerm();
        replicatedLog.append(new DefaultLogEntry(0, term, new MockCommand("1")));
        replicatedLog.append(new DefaultLogEntry(1, term, new MockCommand("2")));
        resetReplicatedLog(replicatedLog);
        replicatedLog.setCommitIndex(replicatedLog.lastIndex());
        replicatedLog.setLastApplied(replicatedLog.lastIndex());
    }

    @Override
    final ActorSelection actorSelection(final String path) {
        return system.actorSelection(path);
    }

    @Override
    public final ActorSystem getActorSystem() {
        return system;
    }

    public void setPeerAddresses(final Map<String, String> peerAddresses) {
        for (String id: getPeerIds()) {
            removePeer(id);
        }

        for (Map.Entry<String, String> e: peerAddresses.entrySet()) {
            addToPeers(e.getKey(), e.getValue(), VotingState.VOTING);
        }
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return raftPolicy != null ? raftPolicy : super.getRaftPolicy();
    }

    public void setRaftPolicy(final RaftPolicy raftPolicy) {
        this.raftPolicy = raftPolicy;
    }

    public static class SimpleReplicatedLog extends AbstractReplicatedLog<ReplicatedLogEntry> {
        public SimpleReplicatedLog() {
            super("");
        }

        @Override
        public int dataSize() {
            return -1;
        }

        @Override
        public void captureSnapshotIfReady(final EntryMeta entry) {
            // No-op
        }

        @Override
        public boolean shouldCaptureSnapshot(final long logIndex) {
            return false;
        }

        @Override
        public boolean trimToReceive(final long fromIndex) {
            return removeFrom(fromIndex) >= 0;
        }

        @Override
        public boolean appendReceived(final LogEntry entry, final Consumer<LogEntry> callback) {
            assertTrue(append(entry));
            if (callback != null) {
                callback.accept(entry);
            }
            return false;
        }

        @Override
        public boolean appendSubmitted(final long index, final long term, final Payload command,
                final Consumer<ReplicatedLogEntry> callback) {
            final var entry = new SimpleReplicatedLogEntry(index, term, command);
            // FIXME: do not ignore return value here: we should be returning that instead of 'true'
            appendImpl(entry);
            if (callback != null) {
                callback.accept(entry);
            }
            return true;
        }

        @Override
        public void markLastApplied() {
            // No-op
        }

        @Override
        protected ReplicatedLogEntry adoptEntry(final LogEntry entry) {
            return SimpleReplicatedLogEntry.of(entry);
        }
    }

    public static final class Builder {
        private final SimpleReplicatedLog mockLog = new SimpleReplicatedLog();

        public Builder createEntries(final int start, final int end, final int term) {
            for (int i = start; i < end; i++) {
                mockLog.append(new DefaultLogEntry(i, term, new MockCommand(Integer.toString(i))));
            }
            return this;
        }

        public Builder addEntry(final int index, final int term, final MockCommand payload) {
            mockLog.append(new DefaultLogEntry(index, term, payload));
            return this;
        }

        public ReplicatedLog build() {
            return mockLog;
        }
    }

    @Override
    public void setCurrentBehavior(final RaftActorBehavior behavior) {
        super.setCurrentBehavior(behavior);
    }
}
