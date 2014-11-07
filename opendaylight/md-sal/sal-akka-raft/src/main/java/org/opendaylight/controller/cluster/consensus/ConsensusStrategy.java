/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

/**
 * A ConsensusStrategy abstracts all the decisions that needs to be taken by an underlying implementation that
 * facilitates consensus by providing mechanisms for persistence, election, replication and state maintenance.
 *
 * A ConsensusStrategy for example can help determine when an election should happen and who should win that election.
 * It can also determine when it is ok to apply state.
 */
public interface ConsensusStrategy {

    /**
     * When a Follower does not receive a heartbeat for some configurable time it will check with the
     * strategy if it should become a candidate
     *
     * @param follower
     * @return
     */
    boolean shouldBecomeCandidate(Follower follower);

    /**
     * When an election timeout happens on a Candidate and it has verified with the strategy that it cannot become
     * a Leader the Candidate should check with the strategy if it should request votes
     *
     * @param candidate
     * @return
     */
    boolean shouldRequestVote(Candidate candidate);

    /**
     * When an election time out happens on a Candidate it can check with the strategy if it is allowed to become
     * a Leader.
     *
     * @param candidate
     * @return
     */
    boolean shouldBecomeLeader(Candidate candidate);

    /**
     * When a vote is received by a Candidate it can check with the strategy if it is allowed to become
     * a Leader.
     *
     * @param candidate
     * @return
     */
    boolean shouldBecomeLeader(Candidate candidate, int voteCount);

    /**
     * When any Participant receives a request for a vote it can check with the strategy if it should cast the vote for
     * that candidate
     *
     * @param participant
     * @param candidate
     * @param votedFor
     * @return
     */
    boolean shouldGrantVote(Participant participant, Candidate candidate, String  votedFor);
}
