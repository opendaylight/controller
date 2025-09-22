/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;
import org.opendaylight.raft.api.RaftRole;

/**
 * The response to a GetOnDemandRaftState message.
 *
 * @author Thomas Pantelis
 */
public class OnDemandRaftState {
    private final long lastLogIndex;
    private final long lastLogTerm;
    private final long currentTerm;
    private final long commitIndex;
    private final long lastApplied;
    private final long lastIndex;
    private final long lastTerm;
    private final long snapshotIndex;
    private final long snapshotTerm;
    private final long replicatedToAllIndex;
    private final String leader;
    private final RaftRole raftState;
    private final String votedFor;
    private final boolean isSnapshotCaptureInitiated;
    private final String customRaftPolicyClassName;
    private final boolean isVoting;

    private final List<FollowerInfo> followerInfoList;
    private final Map<String, String> peerAddresses;
    private final Map<String, Boolean> peerVotingStates;

    private final long inMemoryJournalDataSize;
    private final long inMemoryJournalLogSize;

    protected OnDemandRaftState(final AbstractBuilder<?, ?> builder) {
        lastLogIndex = builder.lastLogIndex;
        lastLogTerm = builder.lastLogTerm;
        currentTerm = builder.currentTerm;
        commitIndex = builder.commitIndex;
        lastApplied = builder.lastApplied;
        lastIndex = builder.lastIndex;
        lastTerm = builder.lastTerm;
        snapshotIndex = builder.snapshotIndex;
        snapshotTerm = builder.snapshotTerm;
        replicatedToAllIndex = builder.replicatedToAllIndex;
        leader = builder.leader;
        raftState = builder.raftState;
        votedFor = builder.votedFor;
        isSnapshotCaptureInitiated = builder.isSnapshotCaptureInitiated;
        customRaftPolicyClassName = builder.customRaftPolicyClassName;
        isVoting = builder.isVoting;
        followerInfoList = builder.followerInfoList;
        peerAddresses = builder.peerAddresses;
        peerVotingStates = builder.peerVotingStates;
        inMemoryJournalDataSize = builder.inMemoryJournalDataSize;
        inMemoryJournalLogSize = builder.inMemoryJournalLogSize;
    }

    public final long getLastLogIndex() {
        return lastLogIndex;
    }

    public final long getLastLogTerm() {
        return lastLogTerm;
    }

    public final long getCurrentTerm() {
        return currentTerm;
    }

    public final long getCommitIndex() {
        return commitIndex;
    }

    public final long getLastApplied() {
        return lastApplied;
    }

    public final long getLastIndex() {
        return lastIndex;
    }

    public final long getLastTerm() {
        return lastTerm;
    }

    public final long getSnapshotIndex() {
        return snapshotIndex;
    }

    public final long getSnapshotTerm() {
        return snapshotTerm;
    }

    public final long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public final long getInMemoryJournalDataSize() {
        return inMemoryJournalDataSize;
    }

    public final long getInMemoryJournalLogSize() {
        return inMemoryJournalLogSize;
    }

    public final String getLeader() {
        return leader;
    }

    public final RaftRole getRaftState() {
        return raftState;
    }

    public final String getVotedFor() {
        return votedFor;
    }

    public final boolean isSnapshotCaptureInitiated() {
        return isSnapshotCaptureInitiated;
    }

    public final boolean isVoting() {
        return isVoting;
    }

    public final List<FollowerInfo> getFollowerInfoList() {
        return followerInfoList;
    }

    public final Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    public final Map<String, Boolean> getPeerVotingStates() {
        return peerVotingStates;
    }

    public final String getCustomRaftPolicyClassName() {
        return customRaftPolicyClassName;
    }

    public abstract static class AbstractBuilder<B extends AbstractBuilder<B, T>, T extends OnDemandRaftState> {
        private List<FollowerInfo> followerInfoList = List.of();
        private Map<String, String> peerAddresses = Map.of();
        private Map<String, Boolean> peerVotingStates = Map.of();
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
        private RaftRole raftState;
        private String votedFor;
        private boolean isSnapshotCaptureInitiated;
        private String customRaftPolicyClassName;
        private boolean isVoting;

        public final @NonNull B lastLogIndex(final long value) {
            lastLogIndex = value;
            return self();
        }

        public final @NonNull B lastLogTerm(final long value) {
            lastLogTerm = value;
            return self();
        }

        public final @NonNull B currentTerm(final long value) {
            currentTerm = value;
            return self();
        }

        public final @NonNull B commitIndex(final long value) {
            commitIndex = value;
            return self();
        }

        public final @NonNull B lastApplied(final long value) {
            lastApplied = value;
            return self();
        }

        public final @NonNull B lastIndex(final long value) {
            lastIndex = value;
            return self();
        }

        public final @NonNull B lastTerm(final long value) {
            lastTerm = value;
            return self();
        }

        public final @NonNull B snapshotIndex(final long value) {
            snapshotIndex = value;
            return self();
        }

        public final @NonNull B snapshotTerm(final long value) {
            snapshotTerm = value;
            return self();
        }

        public final @NonNull B replicatedToAllIndex(final long value) {
            replicatedToAllIndex = value;
            return self();
        }

        public final @NonNull B inMemoryJournalDataSize(final long value) {
            inMemoryJournalDataSize = value;
            return self();
        }

        public final @NonNull B inMemoryJournalLogSize(final long value) {
            inMemoryJournalLogSize = value;
            return self();
        }

        public final @NonNull B leader(final String value) {
            leader = value;
            return self();
        }

        public final @NonNull B raftState(final RaftRole value) {
            raftState = value;
            return self();
        }

        public final @NonNull B votedFor(final String value) {
            votedFor = value;
            return self();
        }

        public final @NonNull B isVoting(final boolean value) {
            isVoting = value;
            return self();
        }

        public final @NonNull B followerInfoList(final List<FollowerInfo> value) {
            followerInfoList = value != null ? List.copyOf(value) : null;
            return self();
        }

        public final @NonNull B peerAddresses(final Map<String, String> value) {
            peerAddresses = copyMap(value);
            return self();
        }

        public final @NonNull B peerVotingStates(final Map<String, Boolean> value) {
            peerVotingStates = copyMap(value);
            return self();
        }

        public final @NonNull B isSnapshotCaptureInitiated(final boolean value) {
            isSnapshotCaptureInitiated = value;
            return self();
        }

        public final @NonNull B raftPolicySymbolicName(final String className) {
            customRaftPolicyClassName = className;
            return self();
        }

        public abstract @NonNull T build();

        protected static final <K extends Comparable<?>, V> @NonNull Map<K, V> copyMap(final Map<K, V> map) {
            // FIXME: sort by key
            return ImmutableMap.copyOf(map);
        }

        @SuppressWarnings("unchecked")
        protected final @NonNull B self() {
            return (B) this;
        }
    }

    public static final class Builder extends AbstractBuilder<Builder, OnDemandRaftState> {
        @Override
        public OnDemandRaftState build() {
            return new OnDemandRaftState(this);
        }
    }
}
