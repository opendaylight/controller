/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

import java.util.Map;

/**
 * A ConsensusStrategy abstracts all the decisions that needs to be taken by an underlying implementation that
 * facilitates consensus by providing mechanisms for persistence, election, replication and state maintenance.
 *
 * A ConsensusStrategy for example can help determine when an election should happen and who should win that election.
 * It can also determine when it is ok to apply state.
 */
public interface ConsensusStrategy {

    /**
     * Update strategy with options.
     *
     * @param options <name,value> pairs describing the property to update
     */
    void updateStrategy(Map<String, Object> options);


    enum DataSentAction {
        BECOME_FOLLOWER,
        NONE
    }

    /**
     * Callback to be invoked by a Leader when it sends data (including heart beats)
     *
     * @param leader the Leader that sent the data
     * @return
     */
    DataSentAction onDataSent(Leader leader);


    enum DataReceivedAction {
        BECOME_FOLLOWER,
        CLEAR_STATE_AND_BECOME_FOLLOWER,
        NONE
    }

    /**
     * Callback to be invoked by a Leader when it has received a data update or heart beat
     *
     * @param receiver the leader who received the data
     * @param sender the leader who sent the data
     * @return
     */
    DataReceivedAction onDataReceived(Leader receiver, Leader sender);


    enum ParticipantSyncedAction {
        RESET_ELECTION_TERM_AND_BECOME_FOLLOWER,
        NONE
    }

    /**
     * Callback to be invoked by a Leader when it has synced a participant to its current view of the data
     *
     * @param participant the participant whose data is synced
     * @return
     */
    ParticipantSyncedAction onParticipantSynced(Participant participant);


    enum HeartbeatNotReceivedAction {
        BECOME_LEADER,
        BECOME_CANDIDATE,
        NONE
    }

    /**
     * Callback to be invoked by a Follower when heart beat is not received
     *
     * @param follower the follower that did not receive the heartbeat
     * @return
     */
    HeartbeatNotReceivedAction onHeartbeatNotReceived(Follower follower);

    enum ElectionTimeoutAction {
        BECOME_LEADER,
        REQUEST_VOTE,
        NONE
    }

    /**
     * Callback to be invoked by a Candidate when an election timeout occurs. An election timeout
     * occurs when a Candidate does not receive the required number of votes in a given term
     *
     * @param candidate the candidate for whom the election timeout occurred
     * @return
     */
    ElectionTimeoutAction onElectionTimeout(Candidate candidate);


    enum VoteReceivedAction {
        BECOME_LEADER,
        NONE
    }

    /**
     * Callback to be invoked by a Candidate when a vote is received
     *
     * @param candidate the candidate who received a vote
     * @param voteCount the count of votes that the candidate has received
     * @return
     */
    VoteReceivedAction onVoteReceived(Candidate candidate, int voteCount);


    enum VoteRequestReceivedAction {
    GRANT_VOTE,
    NONE
    }

    /**
     * Callback to be invoked by any Participant when it receives a request for a vote
     * @param voter the voter for whom the strategy needs to make a decision
     * @param requester the Candidate who requested the vote
     * @param currentChoice the id of the Participant who the voter has already voted for in the current term
     * @return
     */
    VoteRequestReceivedAction onVoteRequestReceived(Participant voter, Candidate requester, String currentChoice);
}
