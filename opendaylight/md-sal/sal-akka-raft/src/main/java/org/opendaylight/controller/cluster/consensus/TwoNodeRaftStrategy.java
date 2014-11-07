/*
 * Copyright (c) 2014 Hewlett-Packard Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

public class TwoNodeRaftStrategy extends RaftConsensus implements ConsensusStrategy {

    // Configuration
    private Participant configuredPrimary;
    private boolean failbackToPrimary;
    private boolean networkPartitionDetectionEnabled;
    private boolean becomePrimaryOnNetworkPartition;

    // Runtime State
    private boolean networkPartitionDetected;

    public void onConfiguredPrimaryChange(Participant configuredPrimary) {
        this.configuredPrimary = configuredPrimary;
    }

    public void onFailbackToPrimaryChange(boolean failbackToPrimary) {
        this.failbackToPrimary = failbackToPrimary;
    }

    public void onNetworkPartitionDetectionEnabledChange(boolean networkPartitionDetectionEnabled) {
        this.networkPartitionDetectionEnabled = networkPartitionDetectionEnabled;
    }

    public void onBecomePrimaryOnNetworkParitionChange(boolean becomePrimaryOnNetworkPartition) {
        this.becomePrimaryOnNetworkPartition = becomePrimaryOnNetworkPartition;
    }

    // Updated by an active network partition detection algorithm.
    public void onNetworkPartition(boolean networkPartition) {
        this.networkPartitionDetected = networkPartition;
    }

    public boolean isConfiguredPrimary(Participant participant) {
        return participant == configuredPrimary;
    }


    @Override
    public DataReceivedAction onDataReceived(Leader receiver, Leader sender) {

        // Failback to configured primary's control and its current state.
        if(isConfiguredPrimary(sender) && failbackToPrimary) {
            return DataReceivedAction.CLEAR_STATE_AND_BECOME_FOLLOWER;
        }

        // Keep new acting primary.
        if(isConfiguredPrimary(receiver) && !failbackToPrimary) {
            return DataReceivedAction.CLEAR_STATE_AND_BECOME_FOLLOWER;
        }

        // Receiver is configured primary and will maintain its state.
        return DataReceivedAction.NONE;
    }

    @Override
    public ParticipantSyncedAction onParticipantSynced(Participant participant) {

        // Failback to participant (configured primary) now that it has latest data for current
        // Leader by starting election again.
        if(isConfiguredPrimary(participant) && failbackToPrimary) {
            return ParticipantSyncedAction.BECOME_CANDIDATE;
        }

        return ParticipantSyncedAction.NONE;
    }

    @Override
    public HeartbeatNotReceivedAction onHeartbeatNotReceived(Follower follower) {

        // Note: The shorter the network partition check period relative to the heartbeat period the lower
        //       the possibility of diverging states between nodes that can result in:
        //       1) Longer reconvergence time and/or
        //       2) Potentially lost data during sync. (1 leader's data must be selected in whole currently).
        //
        // (Recommend heart beat-period > 2x network partition poll period)
        if(networkPartitionDetectionEnabled && networkPartitionDetected) {
            if(becomePrimaryOnNetworkPartition) {
                return HeartbeatNotReceivedAction.BECOME_LEADER;
            } else {
                return HeartbeatNotReceivedAction.NONE;     // Will become a Candidate automatically after election timeout.
            }                                               // (Per RAFT rules)
        }

        return HeartbeatNotReceivedAction.BECOME_LEADER;    // Activate Secondary (Standby)
    }

    @Override
    public ElectionTimeoutAction onElectionTimeout(Candidate candidate) {
        return ElectionTimeoutAction.REQUEST_VOTE;
    }

    @Override
    public VoteReceivedAction onVoteReceived(Candidate candidate, int voteCount) {
        // Extra check is to cover window of time when leader vote is received but configuration designating
        // primary has been changed. (Election will restart in that case.)
        if(isConfiguredPrimary(candidate)) {
            return VoteReceivedAction.BECOME_LEADER;
        }

        return VoteReceivedAction.NONE;
    }

    @Override
    public VoteRequestReceivedAction onVoteRequestReceived(Participant voter, Participant candidate, String votedFor) {
        boolean candidateUpToDate = isCandidateStateLatest(voter, candidate, votedFor);

        if(isConfiguredPrimary(candidate)) {

            if(candidateUpToDate) {
                return VoteRequestReceivedAction.GRANT_VOTE;
            }

            // To cover case when voter and candidate were:
            // 1) Network partitioned.
            // 2) Both active leaders in their network partitioned.
            // 3) Both failed (state was persisted to disk by RAFT protocol).
            // 4) On node restart and first election normal RAFT rules would dictate latest state should be elected leader.
            //    But candidate is the configured primary and its state should overwrite voter (per 2-Node behavior).
            if(!candidateUpToDate && failbackToPrimary) {
                return VoteRequestReceivedAction.GRANT_VOTE_AND_CLEAR_STATE;
            }
        }

        return VoteRequestReceivedAction.NONE;
    }
}
