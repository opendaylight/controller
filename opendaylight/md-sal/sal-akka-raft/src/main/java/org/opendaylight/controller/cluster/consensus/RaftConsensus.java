/*
 * Copyright (c) 2014 Hewlett-Packard Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

public class RaftConsensus {

    /**
     * Check that candidate has the latest state vs. participant's state.
     * (per the official Raft Consensus Algorithm's rules)
     *
     * @param participant base state
     * @param candidate alternative state
     * @param votedFor participant's current leader vote
     * @return
     */
    public boolean isCandidateStateLatest(Participant participant, Participant candidate, String votedFor) {
        boolean candidateLatest = false;

        //  Reply false if term < currentTerm (§5.1)
        if (candidate.getTerm() < participant.getTerm()) {
            candidateLatest = false;

            // If votedFor is null or candidateId, and candidate’s log is at
            // least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
        } else if (votedFor == null || votedFor.equals(candidate.getId())) {

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
        }

        return candidateLatest;
    }
}
