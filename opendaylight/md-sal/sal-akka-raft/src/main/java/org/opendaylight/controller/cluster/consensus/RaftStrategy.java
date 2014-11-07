/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

import java.util.Collection;

public class RaftStrategy extends RaftConsensus implements ConsensusStrategy {

    private final Collection<String> peers;
    private final int votesRequired;

    public RaftStrategy(Collection<String> peers) {
        this.peers = peers;
        if(peers.size() > 0) {
            // Votes are required from a majority of the peers including self.
            // The votesRequired field therefore stores a calculated value
            // of the number of votes required for this candidate to win an
            // election based on it's known peers.
            // If a peer was added during normal operation and raft replicas
            // came to know about them then the new peer would also need to be
            // taken into consideration when calculating this value.
            // Here are some examples for what the votesRequired would be for n
            // peers
            // 0 peers = 1 votesRequired (0 + 1) / 2 + 1 = 1
            // 2 peers = 2 votesRequired (2 + 1) / 2 + 1 = 2
            // 4 peers = 3 votesRequired (4 + 1) / 2 + 1 = 3
            int noOfPeers = peers.size();
            int self = 1;
            votesRequired = (noOfPeers + self) / 2 + 1;
        } else {
            votesRequired = 0;
        }

    }

    @Override
    public DataReceivedAction onDataReceived(Leader receiver, Leader sender) {
        boolean senderHasLatestState = isCandidateStateLatest(receiver, sender, receiver.getId());

        if(senderHasLatestState) {
            return DataReceivedAction.BECOME_FOLLOWER;
        }

        return DataReceivedAction.NONE;
    }

    @Override
    public ParticipantSyncedAction onParticipantSynced(Participant participant) {
        return ParticipantSyncedAction.NONE;
    }

    @Override
    public HeartbeatNotReceivedAction onHeartbeatNotReceived(Follower follower) {
        return HeartbeatNotReceivedAction.BECOME_CANDIDATE;
    }

    @Override
    public ElectionTimeoutAction onElectionTimeout(Candidate candidate) {
        if(peers.size() == 1 && peers.contains(candidate.getId())){
            return ElectionTimeoutAction.BECOME_LEADER;
        }
        return ElectionTimeoutAction.REQUEST_VOTE;
    }

    @Override
    public VoteReceivedAction onVoteReceived(Candidate candidate, int voteCount) {
        if (voteCount >= votesRequired) {
            return VoteReceivedAction.BECOME_LEADER;
        }
        return VoteReceivedAction.NONE;
    }

    @Override
    public VoteRequestReceivedAction onVoteRequestReceived(Participant participant, Participant candidate, String votedFor) {
        boolean candidateHasLatestState = isCandidateStateLatest(participant, candidate, votedFor);

        return candidateHasLatestState ? VoteRequestReceivedAction.GRANT_VOTE : VoteRequestReceivedAction.NONE;
    }
}
