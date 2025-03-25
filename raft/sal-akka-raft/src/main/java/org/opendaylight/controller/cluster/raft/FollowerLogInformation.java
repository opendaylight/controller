/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.behaviors.LeaderInstallSnapshotState;

/**
 * The state of the followers log as known by the Leader.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public final class FollowerLogInformation {
    public static final long NO_INDEX = -1;

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    private final RaftActorContext context;

    private long nextIndex;

    private long matchIndex;

    private long lastReplicatedIndex = -1L;

    private long sentCommitIndex = -1L;

    private final Stopwatch lastReplicatedStopwatch = Stopwatch.createUnstarted();

    private short payloadVersion = -1;

    // Assume the FLUORINE_VERSION version initially, as we no longer support pre-Fluorine versions.
    private short raftVersion = RaftVersions.FLUORINE_VERSION;

    private final PeerInfo peerInfo;

    private LeaderInstallSnapshotState installSnapshotState;

    private long slicedLogEntryIndex = NO_INDEX;

    private boolean needsLeaderAddress;

    /**
     * Constructs an instance.
     *
     * @param peerInfo the associated PeerInfo of the follower.
     * @param matchIndex the initial match index.
     * @param context the RaftActorContext.
     */
    @VisibleForTesting
    FollowerLogInformation(final PeerInfo peerInfo, final long matchIndex, final RaftActorContext context) {
        nextIndex = context.getReplicatedLog().getCommitIndex();
        this.matchIndex = matchIndex;
        this.context = context;
        this.peerInfo = requireNonNull(peerInfo);
    }

    /**
     * Constructs an instance with no matching index.
     *
     * @param peerInfo the associated PeerInfo of the follower.
     * @param context the RaftActorContext.
     */
    public FollowerLogInformation(final PeerInfo peerInfo, final RaftActorContext context) {
        this(peerInfo, NO_INDEX, context);
    }

    /**
     * Increments the value of the follower's next index.
     *
     * @return the new value of nextIndex.
     */
    @VisibleForTesting
    long incrNextIndex() {
        return nextIndex++;
    }

    /**
     * Decrements the value of the follower's next index, taking into account its reported last log index.
     *
     * @param followerLastIndex follower's last reported index.
     * @return true if the next index was decremented, i.e. it was previously &gt;= 0, false otherwise.
     */
    public boolean decrNextIndex(final long followerLastIndex) {
        if (nextIndex < 0) {
            return false;
        }

        if (followerLastIndex >= 0 && nextIndex > followerLastIndex) {
            // If the follower's last log index is lower than nextIndex, jump directly to it, so we converge
            // on a common index more quickly.
            nextIndex = followerLastIndex;
        } else {
            nextIndex--;
        }
        return true;
    }

    /**
     * Sets the index of the follower's next log entry.
     *
     * @param nextIndex the new index.
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    @SuppressWarnings("checkstyle:hiddenField")
    public boolean setNextIndex(final long nextIndex) {
        if (this.nextIndex != nextIndex) {
            this.nextIndex = nextIndex;
            return true;
        }

        return false;
    }

    /**
     * Increments the value of the follower's match index.
     *
     * @return the new value of matchIndex.
     */
    public long incrMatchIndex() {
        return matchIndex++;
    }

    /**
     * Sets the index of the follower's highest log entry.
     *
     * @param matchIndex the new index.
     * @return true if the new index differed from the current index and the current index was updated, false
     *              otherwise.
     */
    @SuppressWarnings("checkstyle:hiddenField")
    public boolean setMatchIndex(final long matchIndex) {
        // If the new match index is the index of the entry currently being sliced, then we know slicing is complete
        // and the follower received the entry and responded so clear the slicedLogEntryIndex
        if (isLogEntrySlicingInProgress() && slicedLogEntryIndex == matchIndex) {
            slicedLogEntryIndex = NO_INDEX;
        }

        if (this.matchIndex != matchIndex) {
            this.matchIndex = matchIndex;
            return true;
        }

        return false;
    }

    /**
     * Returns the identifier of the follower.
     *
     * @return the identifier of the follower.
     */
    public String getId() {
        return peerInfo.getId();
    }

    /**
     * Returns the index of the next log entry to send to the follower.
     *
     * @return index of the follower's next log entry.
     */
    public long getNextIndex() {
        return nextIndex;
    }

    /**
     * Returns the index of highest log entry known to be replicated on the follower.
     *
     * @return the index of highest log entry.
     */
    public long getMatchIndex() {
        return matchIndex;
    }

    /**
     * Checks if the follower is active by comparing the time of the last activity with the election time out. The
     * follower is active if some activity has occurred for the follower within the election time out interval.
     *
     * @return true if follower is active, false otherwise.
     */
    public boolean isFollowerActive() {
        if (peerInfo.getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        return stopwatch.isRunning()
                && elapsed <= context.getConfigParams().getElectionTimeOutInterval().toMillis();
    }

    /**
     * Marks the follower as active. This should be called when some activity has occurred for the follower.
     */
    public void markFollowerActive() {
        if (stopwatch.isRunning()) {
            stopwatch.reset();
        }
        stopwatch.start();
    }

    /**
     * Marks the follower as inactive. This should only be called from unit tests.
     */
    @VisibleForTesting
    public void markFollowerInActive() {
        if (stopwatch.isRunning()) {
            stopwatch.stop();
        }
    }

    /**
     * Returns the time since the last activity occurred for the follower.
     *
     * @return time in nanoseconds since the last activity from the follower.
     */
    public long nanosSinceLastActivity() {
        return stopwatch.elapsed(TimeUnit.NANOSECONDS);
    }

    /**
     * This method checks if the next replicate message can be sent to the follower. This is an optimization to avoid
     * sending duplicate message too frequently if the last replicate message was sent and no reply has been received
     * yet within the current heart beat interval
     *
     * @param commitIndex current commitIndex
     * @return true if it is OK to replicate, false otherwise
     */
    public boolean okToReplicate(final long commitIndex) {
        if (peerInfo.getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        // Return false if we are trying to send duplicate data before the heartbeat interval. This check includes
        // also our commitIndex, as followers need to be told of new commitIndex as soon as possible.
        if (getNextIndex() == lastReplicatedIndex && !hasStaleCommitIndex(commitIndex)
                && lastReplicatedStopwatch.elapsed(TimeUnit.MILLISECONDS)
                < context.getConfigParams().getHeartBeatInterval().toMillis()) {
            return false;
        }

        resetLastReplicated();
        return true;
    }

    private void resetLastReplicated() {
        lastReplicatedIndex = getNextIndex();
        if (lastReplicatedStopwatch.isRunning()) {
            lastReplicatedStopwatch.reset();
        }
        lastReplicatedStopwatch.start();
    }

    /**
     * Returns the log entry payload data version of the follower.
     *
     * @return the payload data version.
     */
    public short getPayloadVersion() {
        return payloadVersion;
    }

    /**
     * Sets the payload data version of the follower.
     *
     * @param payloadVersion the payload data version.
     */
    public void setPayloadVersion(final short payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    /**
     * Returns the the raft version of the follower.
     *
     * @return the raft version of the follower.
     */
    public short getRaftVersion() {
        return raftVersion;
    }

    /**
     * Sets the raft version of the follower.
     *
     * @param raftVersion the raft version.
     */
    public void setRaftVersion(final short raftVersion) {
        checkArgument(raftVersion >= RaftVersions.FLUORINE_VERSION, "Unexpected version %s", raftVersion);
        this.raftVersion = raftVersion;
    }

    /**
     * Returns the LeaderInstallSnapshotState for the in progress install snapshot.
     *
     * @return the LeaderInstallSnapshotState if a snapshot install is in progress, null otherwise.
     */
    public @Nullable LeaderInstallSnapshotState getInstallSnapshotState() {
        return installSnapshotState;
    }

    /**
     * Sets the LeaderInstallSnapshotState when an install snapshot is initiated.
     *
     * @param state the LeaderInstallSnapshotState
     */
    public void setLeaderInstallSnapshotState(final @NonNull LeaderInstallSnapshotState state) {
        if (installSnapshotState == null) {
            installSnapshotState = requireNonNull(state);
        }
    }

    /**
     * Clears the LeaderInstallSnapshotState when an install snapshot is complete.
     */
    public void clearLeaderInstallSnapshotState() {
        checkState(installSnapshotState != null);
        installSnapshotState.close();
        installSnapshotState = null;
    }

    /**
     * Sets the index of the log entry whose payload size exceeds the maximum size for a single message and thus
     * needs to be sliced into smaller chunks.
     *
     * @param index the log entry index or NO_INDEX to clear it
     */
    public void setSlicedLogEntryIndex(final long index) {
        slicedLogEntryIndex  = index;
    }

    /**
     * Return whether or not log entry slicing is currently in progress.
     *
     * @return true if slicing is currently in progress, false otherwise
     */
    public boolean isLogEntrySlicingInProgress() {
        return slicedLogEntryIndex != NO_INDEX;
    }

    public void setNeedsLeaderAddress(final boolean value) {
        needsLeaderAddress = value;
    }

    public @Nullable String needsLeaderAddress(final String leaderId) {
        return needsLeaderAddress ? context.getPeerAddress(leaderId) : null;
    }

    public boolean hasStaleCommitIndex(final long commitIndex) {
        return sentCommitIndex != commitIndex;
    }

    public void setSentCommitIndex(final long commitIndex) {
        sentCommitIndex = commitIndex;
    }

    @Override
    public String toString() {
        return "FollowerLogInformation [id=" + getId() + ", nextIndex=" + nextIndex + ", matchIndex=" + matchIndex
                + ", lastReplicatedIndex=" + lastReplicatedIndex + ", commitIndex=" + sentCommitIndex
                + ", votingState=" + peerInfo.getVotingState()
                + ", stopwatch=" + stopwatch.elapsed(TimeUnit.MILLISECONDS)
                + ", followerTimeoutMillis=" + context.getConfigParams().getElectionTimeOutInterval().toMillis() + "]";
    }
}
