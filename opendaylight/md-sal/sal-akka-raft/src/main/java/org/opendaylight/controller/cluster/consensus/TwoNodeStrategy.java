/*
 * Copyright (c) 2014 Hewlett-Packard Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.consensus;

import java.util.Map;

public class TwoNodeStrategy extends AbstractConsensus {

    private enum TwoNodeAction {
        BECOME_FOLLOWER,
        BECOME_LEADER,
        NONE
    };

    // Configuration
    private Participant configuredPrimary;
    private boolean failbackToPrimary;
    private boolean networkPartitionDetectionEnabled;
    private boolean activeActiveDeployment;                 // (default) FALSE :  Active-Passive deployment (Secondary will NOT operate during Network Partition.)
                                                            //           TRUE  :  Active-Active deployment

    // Runtime State
    private boolean networkPartitionDetected;

    public boolean isConfiguredPrimary(Participant participant) {
        return participant == configuredPrimary;
    }

    @Override
    public void updateStrategy(Map<String, Object> options) {

        // Note: Behavior change will be applied on next callback that uses parameter(s).
        for(String option : options.keySet()) {
            if(option.equals("configuredPrimary")) {
                configuredPrimary = (Participant) options.get(option);
            }
            if(option.equals("failbackToPrimary")) {
                failbackToPrimary = (Boolean) options.get(option);
            }
            if(option.equals("networkPartitionDetectionEnabled")) {
                networkPartitionDetectionEnabled = (Boolean) options.get(option);
            }
            if(option.equals("networkPartitionDetected")) {
                networkPartitionDetected = (Boolean) options.get(option);
            }
            if(option.equals("activeActiveDeployment")) {
                activeActiveDeployment = (Boolean) options.get(option);
            }
        }
    }

    @Override
    public ParticipantSyncedAction onParticipantSynced(Participant participant) {

        // On Configured Primary
        if (!isConfiguredPrimary(participant)) {
            return ParticipantSyncedAction.NONE;

        // On Configured Secondary
        } else {

            if (failbackToPrimary) {
                return ParticipantSyncedAction.RESET_ELECTION_TERM_AND_BECOME_FOLLOWER;
            } else {
                return ParticipantSyncedAction.NONE;
            }
        }
    }

    @Override
    public DataReceivedAction onDataReceived(Leader receiver, Leader sender) {

        if(isConfiguredPrimary(receiver)) {
            return DataReceivedAction.NONE;
        } else {
            //TODO: Tell some notification service (on receiver and sender) that data state may have changed!!!!!!
            return DataReceivedAction.CLEAR_STATE_AND_BECOME_FOLLOWER;
        }
    }

    @Override
    public DataSentAction onDataSent(Leader leader) {
        TwoNodeAction action = getTwoNodeAction(leader, TwoNodeAction.NONE, TwoNodeAction.BECOME_FOLLOWER);

        return (action == TwoNodeAction.BECOME_FOLLOWER ? DataSentAction.BECOME_FOLLOWER : DataSentAction.NONE);
    }

    @Override
    public HeartbeatNotReceivedAction onHeartbeatNotReceived(Follower follower) {
        TwoNodeAction action = getTwoNodeAction(follower, TwoNodeAction.BECOME_LEADER, TwoNodeAction.NONE);

        return (action == TwoNodeAction.BECOME_LEADER ? HeartbeatNotReceivedAction.BECOME_LEADER : HeartbeatNotReceivedAction.NONE);
    }

    @Override
    public ElectionTimeoutAction onElectionTimeout(Candidate candidate) {
        TwoNodeAction action = getTwoNodeAction(candidate, TwoNodeAction.BECOME_LEADER, TwoNodeAction.NONE);

        return (action == TwoNodeAction.BECOME_LEADER ? ElectionTimeoutAction.BECOME_LEADER : ElectionTimeoutAction.NONE);
    }

    @Override
    public VoteReceivedAction onVoteReceived(Candidate candidate, int voteCount) {
        // Ignore normal RAFT voting procedures.
        return VoteReceivedAction.NONE;
    }

    @Override
    public VoteRequestReceivedAction onVoteRequestReceived(Participant voter, Candidate candidate, String votedFor) {
        // Ignore normal RAFT voting procedures.
        return VoteRequestReceivedAction.NONE;
    }


    // Common Active-<Active|Passive> Action determination.
    // (Caller will remap generic TwoNodeAction to specific callback action type)
    private TwoNodeAction getTwoNodeAction(Participant participant, TwoNodeAction activeNodeAction, TwoNodeAction passiveNodeAction) {

        // Configured Primary
        if (isConfiguredPrimary(participant)) {
            return activeNodeAction;

        // Secondary
        } else {
            if (activeActiveDeployment) {
                return activeNodeAction;
            }

            if (networkPartitionDetectionEnabled && networkPartitionDetected) {
                return passiveNodeAction;
            } else {
                return activeNodeAction;
            }
        }
    }
}
