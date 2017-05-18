/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.behaviors.LeaderInstallSnapshotState;

/**
 * Implementation of the FollowerLogInformation interface.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class FollowerLogInformationImpl implements FollowerLogInformation {
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
    public FollowerLogInformationImpl(PeerInfo peerInfo, long matchIndex, RaftActorContext context) {
        this.nextIndex = context.getCommitIndex();
        this.matchIndex = matchIndex;
        this.context = context;
        this.peerInfo = Preconditions.checkNotNull(peerInfo);
    }

    @Override
    public long incrNextIndex() {
        return nextIndex++;
    }

    @Override
    public boolean decrNextIndex() {
        if (nextIndex >= 0) {
            nextIndex--;
            return true;
        }

        return false;
    }

    @Override
    public boolean setNextIndex(long nextIndex) {
        if (this.nextIndex != nextIndex) {
            this.nextIndex = nextIndex;
            return true;
        }

        return false;
    }

    @Override
    public long incrMatchIndex() {
        return matchIndex++;
    }

    @Override
    public boolean setMatchIndex(long matchIndex) {
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

    @Override
    public String getId() {
        return peerInfo.getId();
    }

    @Override
    public long getNextIndex() {
        return nextIndex;
    }

    @Override
    public long getMatchIndex() {
        return matchIndex;
    }

    @Override
    public boolean isFollowerActive() {
        if (peerInfo.getVotingState() == VotingState.VOTING_NOT_INITIALIZED) {
            return false;
        }

        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        return stopwatch.isRunning()
                && elapsed <= context.getConfigParams().getElectionTimeOutInterval().toMillis();
    }

    @Override
    public void markFollowerActive() {
        if (stopwatch.isRunning()) {
            stopwatch.reset();
        }
        stopwatch.start();
    }

    @Override
    public void markFollowerInActive() {
        if (stopwatch.isRunning()) {
            stopwatch.stop();
        }
    }

    @Override
    public long timeSinceLastActivity() {
        return stopwatch.elapsed(TimeUnit.MILLISECONDS);
    }

    @Override
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

    @Override
    public short getPayloadVersion() {
        return payloadVersion;
    }

    @Override
    public void setPayloadVersion(short payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    @Override
    public short getRaftVersion() {
        return raftVersion;
    }

    @Override
    public void setRaftVersion(short raftVersion) {
        this.raftVersion = raftVersion;
    }

    @Override
    @Nullable
    public LeaderInstallSnapshotState getInstallSnapshotState() {
        return installSnapshotState;
    }

    @Override
    public void setLeaderInstallSnapshotState(@Nonnull LeaderInstallSnapshotState state) {
        if (this.installSnapshotState == null) {
            this.installSnapshotState = Preconditions.checkNotNull(state);
        }
    }

    @Override
    public void clearLeaderInstallSnapshotState() {
        Preconditions.checkState(installSnapshotState != null);
        installSnapshotState.close();
        installSnapshotState = null;
    }

    @Override
    public void setSlicedLogEntryIndex(long index) {
        slicedLogEntryIndex  = index;
    }

    @Override
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
