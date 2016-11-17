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
import akka.japi.Procedure;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRaftActorContext extends RaftActorContextImpl {
    private static final Logger LOG = LoggerFactory.getLogger(MockRaftActorContext.class);

    private ActorSystem system;
    private RaftPolicy raftPolicy;

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
            public void update(long newTerm, String newVotedFor) {
                this.currentTerm = newTerm;
                this.votedFor = newVotedFor;

                // TODO : Write to some persistent state
            }

            @Override public void updateAndPersist(long newTerm, String newVotedFor) {
                update(newTerm, newVotedFor);
            }
        };
    }

    public MockRaftActorContext() {
        super(null, null, "test", newElectionTerm(), -1, -1, new HashMap<>(),
                new DefaultConfigParamsImpl(), new NonPersistentDataProvider(), LOG);
        setReplicatedLog(new MockReplicatedLogBuilder().build());
    }

    public MockRaftActorContext(String id, ActorSystem system, ActorRef actor) {
        super(actor, null, id, newElectionTerm(), -1, -1, new HashMap<>(),
                new DefaultConfigParamsImpl(), new NonPersistentDataProvider(), LOG);

        this.system = system;

        initReplicatedLog();
    }


    public void initReplicatedLog() {
        SimpleReplicatedLog replicatedLog = new SimpleReplicatedLog();
        long term = getTermInformation().getCurrentTerm();
        replicatedLog.append(new MockReplicatedLogEntry(term, 0, new MockPayload("1")));
        replicatedLog.append(new MockReplicatedLogEntry(term, 1, new MockPayload("2")));
        setReplicatedLog(replicatedLog);
        setCommitIndex(replicatedLog.lastIndex());
        setLastApplied(replicatedLog.lastIndex());
    }

    @Override public ActorRef actorOf(Props props) {
        return system.actorOf(props);
    }

    @Override public ActorSelection actorSelection(String path) {
        return system.actorSelection(path);
    }

    @Override public ActorSystem getActorSystem() {
        return this.system;
    }

    @Override public ActorSelection getPeerActorSelection(String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if (peerAddress != null) {
            return actorSelection(peerAddress);
        }
        return null;
    }

    public void setPeerAddresses(Map<String, String> peerAddresses) {
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
        snapshotManager.setCreateSnapshotRunnable(() -> { });
        return snapshotManager;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return raftPolicy != null ? raftPolicy : super.getRaftPolicy();
    }

    public void setRaftPolicy(RaftPolicy raftPolicy) {
        this.raftPolicy = raftPolicy;
    }

    public static class SimpleReplicatedLog extends AbstractReplicatedLogImpl {
        @Override
        public int dataSize() {
            return -1;
        }

        @Override
        public void captureSnapshotIfReady(ReplicatedLogEntry replicatedLogEntry) {
        }

        @Override
        public boolean removeFromAndPersist(long index) {
            return removeFrom(index) >= 0;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public boolean appendAndPersist(ReplicatedLogEntry replicatedLogEntry, Procedure<ReplicatedLogEntry> callback,
                boolean doAsync) {
            append(replicatedLogEntry);

            if (callback != null) {
                try {
                    callback.apply(replicatedLogEntry);
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }

            return true;
        }
    }

    public static class MockPayload extends Payload implements Serializable {
        private static final long serialVersionUID = 3121380393130864247L;
        private String value = "";
        private int size;

        public MockPayload() {
        }

        public MockPayload(String data) {
            this.value = data;
            size = value.length();
        }

        public MockPayload(String data, int size) {
            this(data);
            this.size = size;
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
        public boolean equals(Object obj) {
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

    // TODO - this class can be removed and use ReplicatedLogImplEntry directly.
    public static class MockReplicatedLogEntry extends ReplicatedLogImplEntry {
        private static final long serialVersionUID = 1L;

        public MockReplicatedLogEntry(long term, long index, Payload data) {
            super(index, term, data);
        }
    }

    public static class MockReplicatedLogBuilder {
        private final ReplicatedLog mockLog = new SimpleReplicatedLog();

        public  MockReplicatedLogBuilder createEntries(int start, int end, int term) {
            for (int i = start; i < end; i++) {
                this.mockLog.append(new ReplicatedLogImplEntry(i, term,
                        new MockRaftActorContext.MockPayload(Integer.toString(i))));
            }
            return this;
        }

        public  MockReplicatedLogBuilder addEntry(int index, int term, MockPayload payload) {
            this.mockLog.append(new ReplicatedLogImplEntry(index, term, payload));
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
