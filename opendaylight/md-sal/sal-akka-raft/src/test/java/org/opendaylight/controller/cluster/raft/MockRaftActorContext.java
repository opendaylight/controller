/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.IdentifiablePayload;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRaftActorContext extends RaftActorContextImpl {
    private static final Logger LOG = LoggerFactory.getLogger(MockRaftActorContext.class);

    private ActorSystem system;
    private RaftPolicy raftPolicy;
    private Consumer<Optional<OutputStream>> createSnapshotProcedure = out -> { };

    private static ElectionTerm newElectionTerm() {
        return new ElectionTerm() {
            private long currentTerm = 1;
            private String votedFor = "";

            @Override
            public long getCurrentTerm() {
                return currentTerm;
            }

            @Override
            public String getVotedFor() {
                return votedFor;
            }

            @Override
            public void update(final long newTerm, final String newVotedFor) {
                this.currentTerm = newTerm;
                this.votedFor = newVotedFor;

                // TODO : Write to some persistent state
            }

            @Override public void updateAndPersist(final long newTerm, final String newVotedFor) {
                update(newTerm, newVotedFor);
            }
        };
    }

    private static DataPersistenceProvider createProvider() {
        return new NonPersistentDataProvider(Runnable::run);
    }

    public MockRaftActorContext() {
        super(null, null, "test", newElectionTerm(), -1, -1, new HashMap<>(),
                new DefaultConfigParamsImpl(), createProvider(), applyState -> { }, LOG,
                MoreExecutors.directExecutor());
        setReplicatedLog(new MockReplicatedLogBuilder().build());
    }

    public MockRaftActorContext(final String id, final ActorSystem system, final ActorRef actor) {
        super(actor, null, id, newElectionTerm(), -1, -1, new HashMap<>(),
            new DefaultConfigParamsImpl(), createProvider(), applyState -> actor.tell(applyState, actor), LOG,
            MoreExecutors.directExecutor());

        this.system = system;

        initReplicatedLog();
    }


    public void initReplicatedLog() {
        SimpleReplicatedLog replicatedLog = new SimpleReplicatedLog();
        long term = getTermInformation().getCurrentTerm();
        replicatedLog.append(new SimpleReplicatedLogEntry(0, term, new MockPayload("1")));
        replicatedLog.append(new SimpleReplicatedLogEntry(1, term, new MockPayload("2")));
        setReplicatedLog(replicatedLog);
        setCommitIndex(replicatedLog.lastIndex());
        setLastApplied(replicatedLog.lastIndex());
    }

    @Override public ActorRef actorOf(final Props props) {
        return system.actorOf(props);
    }

    @Override public ActorSelection actorSelection(final String path) {
        return system.actorSelection(path);
    }

    @Override public ActorSystem getActorSystem() {
        return this.system;
    }

    @Override public ActorSelection getPeerActorSelection(final String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if (peerAddress != null) {
            return actorSelection(peerAddress);
        }
        return null;
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
        SnapshotManager snapshotManager = super.getSnapshotManager();
        snapshotManager.setCreateSnapshotConsumer(createSnapshotProcedure);

        snapshotManager.setSnapshotCohort(new RaftActorSnapshotCohort() {
            @Override
            public State deserializeSnapshot(final ByteSource snapshotBytes) throws IOException {
                return ByteState.of(snapshotBytes.read());
            }

            @Override
            public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
            }

            @Override
            public void applySnapshot(final State snapshotState) {
            }
        });

        return snapshotManager;
    }

    public void setCreateSnapshotProcedure(final Consumer<Optional<OutputStream>> createSnapshotProcedure) {
        this.createSnapshotProcedure = createSnapshotProcedure;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return raftPolicy != null ? raftPolicy : super.getRaftPolicy();
    }

    public void setRaftPolicy(final RaftPolicy raftPolicy) {
        this.raftPolicy = raftPolicy;
    }

    public static class SimpleReplicatedLog extends AbstractReplicatedLogImpl {
        @Override
        public int dataSize() {
            return -1;
        }

        @Override
        public void captureSnapshotIfReady(final ReplicatedLogEntry replicatedLogEntry) {
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
        @SuppressWarnings("checkstyle:IllegalCatch")
        public boolean appendAndPersist(final ReplicatedLogEntry replicatedLogEntry,
                final Consumer<ReplicatedLogEntry> callback, final boolean doAsync) {
            append(replicatedLogEntry);

            if (callback != null) {
                callback.accept(replicatedLogEntry);
            }

            return true;
        }
    }

    public static class MockPayload extends IdentifiablePayload implements Serializable {
        private static final long serialVersionUID = 3121380393130864247L;
        private final String value;
        private final int size;
        private final Identifier id;

        public MockPayload() {
            this.value = "";
            this.size = 0;
            this.id = null;
        }

        public MockPayload(final String data) {
            this.value = data;
            this.size = value.length();
            this.id = new AbstractRaftActorIntegrationTest.MockIdentifier(data);
        }

        public MockPayload(final String data, final Identifier id) {
            this.value = data;
            this.size = value.length();
            this.id = id;
        }

        public MockPayload(final String data, final int size) {
            this.value = data;
            this.size = size;
            this.id = new AbstractRaftActorIntegrationTest.MockIdentifier(data);
        }

        @Override
        public Object getIdentifier() {
            return id;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (value == null ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MockPayload other = (MockPayload) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }
    }

    public static class MockReplicatedLogBuilder {
        private final ReplicatedLog mockLog = new SimpleReplicatedLog();

        public  MockReplicatedLogBuilder createEntries(final int start, final int end, final int term) {
            for (int i = start; i < end; i++) {
                this.mockLog.append(new SimpleReplicatedLogEntry(i, term,
                        new MockRaftActorContext.MockPayload(Integer.toString(i))));
            }
            return this;
        }

        public  MockReplicatedLogBuilder addEntry(final int index, final int term, final MockPayload payload) {
            this.mockLog.append(new SimpleReplicatedLogEntry(index, term, payload));
            return this;
        }

        public ReplicatedLog build() {
            return this.mockLog;
        }
    }

    @Override
    public void setCurrentBehavior(final RaftActorBehavior behavior) {
        super.setCurrentBehavior(behavior);
    }
}
