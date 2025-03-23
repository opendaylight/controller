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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;

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

    // FIXME: RaftRole here
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

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B, T>, T extends OnDemandRaftState> {
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        protected abstract @NonNull OnDemandRaftState state();

        public B lastLogIndex(final long value) {
            state().lastLogIndex = value;
            return self();
        }

        public B lastLogTerm(final long value) {
            state().lastLogTerm = value;
            return self();
        }

        public B currentTerm(final long value) {
            state().currentTerm = value;
            return self();
        }

        public B commitIndex(final long value) {
            state().commitIndex = value;
            return self();
        }

        public B lastApplied(final long value) {
            state().lastApplied = value;
            return self();
        }

        public B lastIndex(final long value) {
            state().lastIndex = value;
            return self();
        }

        public B lastTerm(final long value) {
            state().lastTerm = value;
            return self();
        }

        public B snapshotIndex(final long value) {
            state().snapshotIndex = value;
            return self();
        }

        public B snapshotTerm(final long value) {
            state().snapshotTerm = value;
            return self();
        }

        public B replicatedToAllIndex(final long value) {
            state().replicatedToAllIndex = value;
            return self();
        }

        public B inMemoryJournalDataSize(final long value) {
            state().inMemoryJournalDataSize = value;
            return self();
        }

        public B inMemoryJournalLogSize(final long value) {
            state().inMemoryJournalLogSize = value;
            return self();
        }

        public B leader(final String value) {
            state().leader = value;
            return self();
        }

        public B raftState(final String value) {
            state().raftState = value;
            return self();
        }

        public B votedFor(final String value) {
            state().votedFor = value;
            return self();
        }

        public B isVoting(final boolean isVoting) {
            state().isVoting = isVoting;
            return self();
        }

        public B followerInfoList(final List<FollowerInfo> followerInfoList) {
            state().followerInfoList = followerInfoList;
            return self();
        }

        public B peerAddresses(final Map<String, String> peerAddresses) {
            state().peerAddresses = peerAddresses;
            return self();
        }

        public B peerVotingStates(final Map<String, Boolean> peerVotingStates) {
            state().peerVotingStates = ImmutableMap.copyOf(peerVotingStates);
            return self();
        }

        public B isSnapshotCaptureInitiated(final boolean value) {
            state().isSnapshotCaptureInitiated = value;
            return self();
        }

        public B customRaftPolicyClassName(final String className) {
            state().customRaftPolicyClassName = className;
            return self();
        }

        @SuppressWarnings("unchecked")
        public T build() {
            return (T) state();
        }
    }

    public static class Builder extends AbstractBuilder<Builder, OnDemandRaftState> {
        private final OnDemandRaftState state = new OnDemandRaftState();

        @Override
        protected OnDemandRaftState state() {
            return state;
        }
    }
}
