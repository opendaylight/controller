/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

import java.util.Collection;

public class RaftStrategy implements ConsensusStrategy {

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
    public boolean shouldBecomeCandidate(Follower follower) {
        return true;
    }

    @Override
    public boolean shouldRequestVote(Candidate candidate) {
        return true;
    }

    @Override
    public boolean shouldBecomeLeader(Candidate candidate) {
        if(peers.size() == 1 && peers.contains(candidate.getId())){
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldBecomeLeader(Candidate candidate, int voteCount) {
        if (voteCount >= votesRequired) {
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldGrantVote(Participant participant, Candidate candidate, String votedFor) {
        boolean grantVote = false;

        //  Reply false if term < currentTerm (§5.1)
        if (candidate.getTerm() < participant.getTerm()) {
            grantVote = false;

            // If votedFor is null or candidateId, and candidate’s log is at
            // least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
        } else if (votedFor == null || votedFor
                .equals(candidate.getId())) {

            boolean candidateLatest = false;

            // From §5.4.1
            // Raft determines which of two logs is more up-to-date
            // by comparing the index and term of the last entries in the
            // logs. If the logs have last entries with different terms, then
            // the log with the later term is more up-to-date. If the logs
            // end with the same term, then whichever log is longer is
            // more up-to-date.
            if (candidate.getLastLogTerm() > participant.getLastLogTerm()) {
                candidateLatest = true;
            } else if ((candidate.getLastLogTerm() == participant.getLastLogTerm())
                    && candidate.getLastLogIndex() >= participant.getLastLogTerm()) {
                candidateLatest = true;
            }

            if (candidateLatest) {
                grantVote = true;
            }
        }

        return grantVote;
    }
}
