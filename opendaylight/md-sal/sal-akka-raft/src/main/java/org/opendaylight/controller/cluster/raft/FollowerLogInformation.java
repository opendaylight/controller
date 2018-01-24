/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private final Stopwatch lastReplicatedStopwatch = Stopwatch.createUnstarted();

    private short payloadVersion = -1;

    // Assume the HELIUM_VERSION version initially for backwards compatibility until we obtain the follower's
    // actual version via AppendEntriesReply. Although we no longer support the Helium version, a pre-Boron
    // follower will not have the version field in AppendEntriesReply so it will be set to 0 which is
    // HELIUM_VERSION.
    private short raftVersion = RaftVersions.HELIUM_VERSION;

    private final PeerInfo peerInfo;

    private LeaderInstallSnapshotState installSnapshotState;

    private long slicedLogEntryIndex = NO_INDEX;

    /**
     * Constructs an instance.
     *
     * @param peerInfo the associated PeerInfo of the follower.
     * @param matchIndex the initial match index.
     * @param context the RaftActorContext.
     */
    @VisibleForTesting
    FollowerLogInformation(final PeerInfo peerInfo, final long matchIndex, final RaftActorContext context) {
        this.nextIndex = context.getCommitIndex();
        this.matchIndex = matchIndex;
        this.context = context;
        this.peerInfo = Preconditions.checkNotNull(peerInfo);
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
     * Decrements the value of the follower's next index.
     *
     * @return true if the next index was decremented, ie it was previously &gt;= 0, false otherwise.
     */
    public boolean decrNextIndex() {
        if (nextIndex <= 0) {
            return false;
        }

        nextIndex--;
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
     * @return time in milliseconds since the last activity from the follower.
     */
    public long timeSinceLastActivity() {
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    /**
     * This method checks if the next replicate message can be sent to the follower. This is an optimization to avoid
     * sending duplicate message too frequently if the last replicate message was sent and no reply has been received
     * yet within the current heart beat interval
     *
     * @return true if it is OK to replicate, false otherwise
     */
    public boolean okToReplicate() {
        if (peerInfo.getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        // Return false if we are trying to send duplicate data before the heartbeat interval
        if (getNextIndex() == lastReplicatedIndex && lastReplicatedStopwatch.elapsed(TimeUnit.MILLISECONDS)
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
        this.raftVersion = raftVersion;
    }

    /**
     * Returns the LeaderInstallSnapshotState for the in progress install snapshot.
     *
     * @return the LeaderInstallSnapshotState if a snapshot install is in progress, null otherwise.
     */
    @Nullable
    public LeaderInstallSnapshotState getInstallSnapshotState() {
        return installSnapshotState;
    }

    /**
     * Sets the LeaderInstallSnapshotState when an install snapshot is initiated.
     *
     * @param state the LeaderInstallSnapshotState
     */
    public void setLeaderInstallSnapshotState(@Nonnull final LeaderInstallSnapshotState state) {
        if (this.installSnapshotState == null) {
            this.installSnapshotState = Preconditions.checkNotNull(state);
        }
    }

    /**
     * Clears the LeaderInstallSnapshotState when an install snapshot is complete.
     */
    public void clearLeaderInstallSnapshotState() {
        Preconditions.checkState(installSnapshotState != null);
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

    @Override
    public String toString() {
        return "FollowerLogInformationImpl [id=" + getId() + ", nextIndex=" + nextIndex + ", matchIndex=" + matchIndex
                + ", lastReplicatedIndex=" + lastReplicatedIndex + ", votingState=" + peerInfo.getVotingState()
                + ", stopwatch=" + stopwatch.elapsed(TimeUnit.MILLISECONDS) + ", followerTimeoutMillis="
                + context.getConfigParams().getElectionTimeOutInterval().toMillis() + "]";
    }
}
