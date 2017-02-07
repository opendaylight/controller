/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * The response to a GetOnDemandRaftState message.
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
    private String customRaftPolicyClassName;
    private boolean isVoting;

    private List<FollowerInfo> followerInfoList = Collections.emptyList();
    private Map<String, String> peerAddresses = Collections.emptyMap();
    private Map<String, Boolean> peerVotingStates = Collections.emptyMap();

    protected OnDemandRaftState() {
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

    public boolean isVoting() {
        return isVoting;
    }

    public List<FollowerInfo> getFollowerInfoList() {
        return followerInfoList;
    }

    public Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    public Map<String, Boolean> getPeerVotingStates() {
        return peerVotingStates;
    }

    public String getCustomRaftPolicyClassName() {
        return customRaftPolicyClassName;
    }

    public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {
        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        @Nonnull
        protected abstract OnDemandRaftState state();

        public T lastLogIndex(long value) {
            state().lastLogIndex = value;
            return self();
        }

        public T lastLogTerm(long value) {
            state().lastLogTerm = value;
            return self();
        }

        public T currentTerm(long value) {
            state().currentTerm = value;
            return self();
        }

        public T commitIndex(long value) {
            state().commitIndex = value;
            return self();
        }

        public T lastApplied(long value) {
            state().lastApplied = value;
            return self();
        }

        public T lastIndex(long value) {
            state().lastIndex = value;
            return self();
        }

        public T lastTerm(long value) {
            state().lastTerm = value;
            return self();
        }

        public T snapshotIndex(long value) {
            state().snapshotIndex = value;
            return self();
        }

        public T snapshotTerm(long value) {
            state().snapshotTerm = value;
            return self();
        }

        public T replicatedToAllIndex(long value) {
            state().replicatedToAllIndex = value;
            return self();
        }

        public T inMemoryJournalDataSize(long value) {
            state().inMemoryJournalDataSize = value;
            return self();
        }

        public T inMemoryJournalLogSize(long value) {
            state().inMemoryJournalLogSize = value;
            return self();
        }

        public T leader(String value) {
            state().leader = value;
            return self();
        }

        public T raftState(String value) {
            state().raftState = value;
            return self();
        }

        public T votedFor(String value) {
            state().votedFor = value;
            return self();
        }

        public T isVoting(boolean isVoting) {
            state().isVoting = isVoting;
            return self();
        }

        public T followerInfoList(List<FollowerInfo> followerInfoList) {
            state().followerInfoList = followerInfoList;
            return self();
        }

        public T peerAddresses(Map<String, String> peerAddresses) {
            state().peerAddresses = peerAddresses;
            return self();
        }

        public T peerVotingStates(Map<String, Boolean> peerVotingStates) {
            state().peerVotingStates = ImmutableMap.copyOf(peerVotingStates);
            return self();
        }

        public T isSnapshotCaptureInitiated(boolean value) {
            state().isSnapshotCaptureInitiated = value;
            return self();
        }

        public T customRaftPolicyClassName(String className) {
            state().customRaftPolicyClassName = className;
            return self();
        }

        public OnDemandRaftState build() {
            return state();
        }
    }

    public static class Builder extends AbstractBuilder<Builder> {
        private final OnDemandRaftState state = new OnDemandRaftState();

        @Override
        protected OnDemandRaftState state() {
            return state;
        }
    }
}
