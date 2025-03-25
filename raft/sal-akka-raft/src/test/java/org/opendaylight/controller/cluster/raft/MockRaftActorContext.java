/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.MoreExecutors;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.ByteStateSnapshotCohort;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.TestTermInfoStore;
import org.opendaylight.raft.api.EntryMeta;

public class MockRaftActorContext extends RaftActorContextImpl {
    private ActorSystem system;
    private RaftPolicy raftPolicy;
    private Consumer<OutputStream> createSnapshotProcedure = out -> { };

    @NonNullByDefault
    private static LocalAccess newLocalAccess(final String id) {
        return new LocalAccess(id, new TestTermInfoStore(1, ""));
    }

    public MockRaftActorContext(final int payloadVersion) {
        super(null, null, newLocalAccess("test"), new HashMap<>(), new DefaultConfigParamsImpl(),
            (short) payloadVersion, TestDataProvider.INSTANCE, applyState -> { }, MoreExecutors.directExecutor());
        resetReplicatedLog(new MockReplicatedLogBuilder().build());
    }

    public MockRaftActorContext(final String id, final ActorSystem system, final ActorRef actor,
            final int payloadVersion) {
        super(actor, null, newLocalAccess(id), new HashMap<>(), new DefaultConfigParamsImpl(),
            (short) payloadVersion, TestDataProvider.INSTANCE, applyState -> actor.tell(applyState, actor),
            MoreExecutors.directExecutor());
        this.system = system;
        initReplicatedLog();
    }

    public MockRaftActorContext() {
        this(0);
    }

    public MockRaftActorContext(final String id, final ActorSystem system, final ActorRef actor) {
        this(id, system, actor, 0);
    }

    public void initReplicatedLog() {
        final var replicatedLog = new SimpleReplicatedLog();
        long term = currentTerm();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, term, new MockPayload("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, term, new MockPayload("2")));
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
    public SnapshotManager getSnapshotManager() {
        final var snapshotManager = super.getSnapshotManager();

        snapshotManager.setSnapshotCohort(new ByteStateSnapshotCohort() {
            @Override
            public ByteState takeSnapshot() {
                throw new UnsupportedOperationException();
            }

            @Override
            @Deprecated(forRemoval = true)
            public void createSnapshot(final ActorRef actorRef, final OutputStream installSnapshotStream) {
                createSnapshotProcedure.accept(installSnapshotStream);
            }

            @Override
            public void applySnapshot(final ByteState snapshotState) {
                // No-op
            }
        });

        return snapshotManager;
    }

    public void setCreateSnapshotProcedure(final Consumer<OutputStream> createSnapshotProcedure) {
        this.createSnapshotProcedure = requireNonNull(createSnapshotProcedure);
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return raftPolicy != null ? raftPolicy : super.getRaftPolicy();
    }

    public void setRaftPolicy(final RaftPolicy raftPolicy) {
        this.raftPolicy = raftPolicy;
    }

    public static class SimpleReplicatedLog extends AbstractReplicatedLog {
        public SimpleReplicatedLog() {
            super("");
        }

        @Override
        public int dataSize() {
            return -1;
        }

        @Override
        public void captureSnapshotIfReady(final EntryMeta replicatedLogEntry) {
            // No-op
        }

        @Override
        public boolean shouldCaptureSnapshot(final long logIndex) {
            return false;
        }

        @Override
        public boolean removeFromAndPersist(final long index) {
            return removeFrom(index) >= 0;
        }

        @Override
        public <T extends ReplicatedLogEntry> boolean appendAndPersist(final T replicatedLogEntry,
                final Consumer<T> callback, final boolean doAsync) {
            append(replicatedLogEntry);

            if (callback != null) {
                callback.accept(replicatedLogEntry);
            }
            return true;
        }
    }

    public static final class MockPayload extends Payload {
        @java.io.Serial
        private static final long serialVersionUID = 3121380393130864247L;

        private final String data;
        private final int size;

        public MockPayload() {
            this("");
        }

        public MockPayload(final String data) {
            this(data, data.length());
        }

        public MockPayload(final String data, final int size) {
            this.data = requireNonNull(data);
            this.size = size;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int serializedSize() {
            return size;
        }

        @Override
        public String toString() {
            return data;
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return this == obj || obj instanceof MockPayload other && size == other.size && data.equals(other.data);
        }

        @Override
        protected Object writeReplace() {
            return new MockPayloadProxy(data, size);
        }
    }

    private static final class MockPayloadProxy implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private final String value;
        private final int size;

        MockPayloadProxy(final String value, final int size) {
            this.value = value;
            this.size = size;
        }

        @java.io.Serial
        private Object readResolve() {
            return new MockPayload(value, size);
        }
    }

    public static class MockReplicatedLogBuilder {
        private final SimpleReplicatedLog mockLog = new SimpleReplicatedLog();

        public MockReplicatedLogBuilder createEntries(final int start, final int end, final int term) {
            for (int i = start; i < end; i++) {
                mockLog.append(new SimpleReplicatedLogEntry(i, term, new MockPayload(Integer.toString(i))));
            }
            return this;
        }

        public MockReplicatedLogBuilder addEntry(final int index, final int term, final MockPayload payload) {
            mockLog.append(new SimpleReplicatedLogEntry(index, term, payload));
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
