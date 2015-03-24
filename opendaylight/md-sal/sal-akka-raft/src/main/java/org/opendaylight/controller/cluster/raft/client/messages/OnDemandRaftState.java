/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The response to a GetOnDemandRaftState message,
 *
 * @author Thomas Pantelis
 */
public class OnDemandRaftState {
    private long lastLogIndex = -1L;
    private long lastLogTerm = -1L;
    private long currentTerm = -1L;
    private long commitIndex = -1L;
    private long lastApplied = -1L;
    private long lastIndex = -1L;
    private long lastTerm = -1L;
    private long snapshotIndex = -1L;
    private long snapshotTerm = -1L;
    private long replicatedToAllIndex = -1L;
    private long inMemoryJournalDataSize;
    private long inMemoryJournalLogSize;
    private String leader;
    private String raftState;
    private String votedFor;
    private boolean isSnapshotCaptureInitiated;

    private List<FollowerInfo> followerInfoList = Collections.emptyList();
    private Map<String, String> peerAddresses = Collections.emptyMap();

    private OnDemandRaftState() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    public long getLastIndex() {
        return lastIndex;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getSnapshotIndex() {
        return snapshotIndex;
    }

    public long getSnapshotTerm() {
        return snapshotTerm;
    }

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public long getInMemoryJournalDataSize() {
        return inMemoryJournalDataSize;
    }

    public long getInMemoryJournalLogSize() {
        return inMemoryJournalLogSize;
    }

    public String getLeader() {
        return leader;
    }

    public String getRaftState() {
        return raftState;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public boolean isSnapshotCaptureInitiated() {
        return isSnapshotCaptureInitiated;
    }

    public List<FollowerInfo> getFollowerInfoList() {
        return followerInfoList;
    }

    public Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    public static class Builder {
        private final OnDemandRaftState stats = new OnDemandRaftState();

        public Builder lastLogIndex(long value) {
            stats.lastLogIndex = value;
            return this;
        }

        public Builder lastLogTerm(long value) {
            stats.lastLogTerm = value;
            return this;
        }

        public Builder currentTerm(long value) {
            stats.currentTerm = value;
            return this;
        }

        public Builder commitIndex(long value) {
            stats.commitIndex = value;
            return this;
        }

        public Builder lastApplied(long value) {
            stats.lastApplied = value;
            return this;
        }

        public Builder lastIndex(long value) {
            stats.lastIndex = value;
            return this;
        }

        public Builder lastTerm(long value) {
            stats.lastTerm = value;
            return this;
        }

        public Builder snapshotIndex(long value) {
            stats.snapshotIndex = value;
            return this;
        }

        public Builder snapshotTerm(long value) {
            stats.snapshotTerm = value;
            return this;
        }

        public Builder replicatedToAllIndex(long value) {
            stats.replicatedToAllIndex = value;
            return this;
        }

        public Builder inMemoryJournalDataSize(long value) {
            stats.inMemoryJournalDataSize = value;
            return this;
        }

        public Builder inMemoryJournalLogSize(long value) {
            stats.inMemoryJournalLogSize = value;
            return this;
        }

        public Builder leader(String value) {
            stats.leader = value;
            return this;
        }

        public Builder raftState(String value) {
            stats.raftState = value;
            return this;
        }

        public Builder votedFor(String value) {
            stats.votedFor = value;
            return this;
        }

        public Builder followerInfoList(List<FollowerInfo> followerInfoList) {
            stats.followerInfoList = followerInfoList;
            return this;
        }

        public Builder peerAddresses(Map<String, String> peerAddresses) {
            stats.peerAddresses = peerAddresses;
            return this;
        }

        public Builder isSnapshotCaptureInitiated(boolean value) {
            stats.isSnapshotCaptureInitiated = value;
            return this;
        }

        public OnDemandRaftState build() {
            return stats;
        }
    }
}
