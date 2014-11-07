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
    public DataSentAction onDataSent(Leader leader) {

        // For Configured Primary
        if (isConfiguredPrimary(leader)) {

            // Once a Configured Primary is a leader (and getting this callback) it will remain a Leader regardless of
            // Network Partition state.
            return DataSentAction.NONE;

        // For Configured Secondary
        } else {
            if (networkPartitionDetectionEnabled && networkPartitionDetected) {

                if(activeActiveDeployment) {
                    return DataSentAction.BECOME_LEADER;        // Activate Secondary

                } else {
                    // Secondary trying to act as a Primary/Leader during a network partition
                    // (Not Allowed as it should be Passive)

                    // (Possibility: The network is NOT partitioned but we didn't detect it properly at the time of this callback.)
                    //
                    // Note: If Network Partition state used for this decision is incorrect and there is not a Network Partition the node will
                    //       get other chances to check the network state and adjust its behavior on addition electionTimeout callbacks
                    //       where it will become Leader again (and NOT lose any of its existing state as a Leader).

                    return DataSentAction.BECOME_FOLLOWER;
                }
            } else {

                // (Possibility: There is a Network Partition but we didn't detect it properly at the time of this callback.)
                //
                // Note: If Network Partition state used for this decision is incorrect and there is a Network Partition it will get other
                //       chances to check the network state and adjust its behavior during next onDataSent callback as a Leader.
                return DataSentAction.BECOME_LEADER;
            }
        }

    }

    @Override
    public DataReceivedAction onDataReceived(Leader receiver, Leader sender) {

        // Conflicting Leadership:
        //
        // - NOTE: When multiple leaders are detected the system will ALWAYS attempt to revert the total system state to the
        //         Configured Primary's view regardless of activeActiveDeployment setting or actual state data. The rational is
        //         that some node will be out of sync and instead of trying to determine who has the minimum state to change it
        //         is better if we just deterministically accept 1 node's state all the time and notify everyone of the state
        //         change.
        //
        // There are many factors that prevent updating all nodes to the exact state of the system in 2-Node.
        // Below are some of the factors that can contribute to this. Applications on both nodes will have to be notified
        // of the state change so they can resolve their own data, update network devices that will be out of
        // sync with the final state, etc.
        //
        // Contributing Factors:
        //
        //  1) No guarantee that Configured Primary will have the latest election term vs. the Secondary.
        //     (RAFT state rules could intervene and say that the Configure Primary's state is old
        //      so we have ot override this.)
        //  2) Network Partition detection state may NOT be correct at the time of callback decisions in this
        //     strategy. Still the callbacks allow the correct 2-Node behavior to EVENTUALLY exists. But there can
        //     always be a window where 2 Leaders could have been running in the system, accepting
        //     datastore (state) changes, updating network device config, etc.

        if(isConfiguredPrimary(receiver)) {
            return DataReceivedAction.NONE;
        } else {
            //FIXME: Tell some notification service (on receiver and sender) that data state may have changed!!!!!!

            return DataReceivedAction.CLEAR_STATE_AND_BECOME_FOLLOWER;
        }
    }

    @Override
    public ParticipantSyncedAction onParticipantSynced(Participant participant) {

        // On Configured Primary
        if (!isConfiguredPrimary(participant)) {
            return ParticipantSyncedAction.NONE;            // Just notified Follower is synced. (No leadership change)

        // On Configured Secondary
        } else {

            // Regardless of the report Network Partition state failback to Configured Primary if failbackToPrimary
            // is enabled now that it has latest data.
            //
            // The Secondary will take two actions:
            //
            //  1) Resetting its election term so that Configured Primary can become the leader on next election without
            //     election term conflicts.
            //     - Note: We do NOT clear state to cover the case where we've conceded Leadership to the Configured Primary
            //             but it dies before we are notified. Since we were the active Primary with the latest state
            //             and we did NOT clear our state it allows us to become Leader on next electionTimeout with less
            //             (and potentially zero) state loss.
            //  2) Becoming a Follower. The Configured Primary will either assume leadership before this node has an
            //     electionTimeout or, depending on timing, it may elect itself leader again during onVoteSent.             // FIXME: possible case of this sequence occurring over and over (assume random election terms will eventually break this.)
            //     Two leaders will be resolved as part of the onDataReceived callback.

            if (failbackToPrimary) {
                return ParticipantSyncedAction.RESET_ELECTION_TERM_AND_BECOME_FOLLOWER;
            } else {
                return ParticipantSyncedAction.NONE;        // Just notified Follower is synced. (No leadership change)
            }
        }
    }

    // Possible Modes of Network Partition Algorithm reporting state (networkPartitionDetected):
    //
    //  1) (Free-Running) - *******Current Plan*******
    //     - Always be polling and reporting state
    //  2) (Polling Started on This Callback)
    //     - Still chance of being incorrect and if done this way we CAN'T block Actors progress at this point
    //       (per Actor Model rules).
    @Override
    public HeartbeatNotReceivedAction onHeartbeatNotReceived(Follower follower) {

        // The logic below assumes the Network Partition detection algorithm reports the correct state. Of course
        // there is a chance it could be wrong and below are the consequences of this logic:
        //
        // For Configured Secondary (running as Follower):
        //
        //  1) (Network Actually Partitioned But Alg. Reports NO Partition)
        //     - Lack of heartbeat from Leader was really due to network partition. Logic below on this callback will have us
        //       become Active Primary and take over full control (or at least as much as it can really physically control).
        //       The Secondary acting as the new Primary will have an opportunity to reconsider its behavior each onDataSent
        //       callback as it reports its Leadership OR onDataReceived callback if it later sees the other node is still a Leader.
        //  2) (Network Not Actually Partitioned But Alg. Reports Partition)
        //     - Logic below will have the Secondary remain a Follower. Without an active Leader the Follower will have an
        //       electionTimeout and eventually become a Candidate. This node will have an opportunity to reconsider its
        //       behavior on each onVoteSent callback during its election. It will be at least 1 more election term/timeout
        //       before Secondary corrects this and becomes the active Primary/Leader.
        //
        // For Configured Primary (running as a Follower):
        //
        //  1) (Network Actually Partitioned But Alg. Reports NO Partition)
        //     - Node will be able to control fewer devices that it would think in this scenario.
        //  2) (Network Not Actually Partitioned But Alg. Reports Partition)
        //     - Node will attempt to control more devices that it can in this scenario.

        // On Configured Primary (Acting as a Secondary since it is a Follower)
        if (isConfiguredPrimary(follower)) {

            // Configured Primary will assume control (become Leader) regardless if there is a Network Partition or
            // previously acting Primary has failed. (The only difference in these two conditions is how much of the network
            // the Configured Primary will actually control.)
            //
            // 2-Leader case will be resolved during onDataReceived callback.
            return HeartbeatNotReceivedAction.BECOME_LEADER;

        // On Configured Secondary (Acting as a Secondary since it is a Follower)
        } else {
            if (networkPartitionDetectionEnabled && networkPartitionDetected) {

                if(activeActiveDeployment) {
                    return HeartbeatNotReceivedAction.BECOME_LEADER;        // Activate Secondary to operate on its network segment.
                } else {
                    return HeartbeatNotReceivedAction.NONE;                 // Remain passive Follower
                }

            } else {
                return HeartbeatNotReceivedAction.BECOME_LEADER;        // No Network Partition - Become Primary.
            }
        }
    }

    @Override
    public ElectionTimeoutAction onElectionTimeout(Candidate candidate) {

        // The following are cases where the Configured Primary or Secondary could be a Candidate on an Election Timeout:
        //
        //  1) Node is just starting up in a new cluster.
        //  2) Node previously failed and is restarting in to a running cluster that:
        //      a) has no existing active Primary (Leader)
        //      b) has an active Primary (Leader) but election timeout occurs before heartbeat                                         //FIXME I am assuming this is possible and logic in this strategy tries to account handle it.
        //  3) There is a Network Partition.
        //

        // Note: It is possible with this logic the Configured Primary will NOT be elected the first Leader on the cluster.
        //       But when this happens if failbackToPrimary is set the system will failback to the Configured Primary as intended
        //       as soon as possible.
        //
        //       This behavior allows us to make sure there is always a Leader managing what is can (and what it allowed to
        //       manage given the current activeActiveDeployment configuration value).


        // For Configured Primary
        if (isConfiguredPrimary(candidate)) {

            // Regardless of Network Partition status Configured Primary has to assume Leadership to avoid cascading failure
            // cases where no Leader would be elected by normal election process (example below):
            //  - If for some reason there was a existing active Leader in the system that was missed causing this electionTimeout
            //    we will have an opportunity to resolve who should be leader during onDataReceived callbacks)
            //
            // Example (Cascading Failures):
            //  1) If we started with an otherwise normal RAFT election process with both nodes present on new cluster the
            //     Configured Primary would become leader.
            //  2) Configured Primary fails (or all its network links fail) and Configured Secondary becomes active Primary.
            //  3) Secondary fails leaving no active Primary for cluster.
            //  4) Configured Primary is restarted (or all of its network links are fixed), starts a Follower (per Raft rules),
            //     and gets this electionTimeout callback.
            //
            //  Result: While Secondary is fail-stopped in this example the configured Primary would never win an election since there is
            //          no other voter present. The network would be unmanaged even though the Configured Primary could safely become
            //          Primary and control all/some of the network (depending on Network Partition state). We have to force
            //          Leadership here to have a chance of the cluster continuing operate ESPECIALLY if the Configured Primary itself
            //          didn't fail but its links where temporarily unavailable.

            return ElectionTimeoutAction.BECOME_LEADER;

        // For Configured Secondary
        } else {
            if (networkPartitionDetectionEnabled && networkPartitionDetected) {

                if(activeActiveDeployment) {
                    return ElectionTimeoutAction.BECOME_LEADER;        // Activate Secondary

                } else {

                    // (Possibility: The network is NOT partitioned but we didn't detect ot properly at the time of this callback.)
                    //
                    // Note: If Network Partition state used for this decision is incorrect and there is not a Network Partition the node will
                    //       get other chances to check the network state and adjust its behavior during onVoteSent callbacks as a Candidate.

                    return ElectionTimeoutAction.NONE;
                }

            } else {

                // (Possibility: There is a Network Partition but we didn't detect it properly at the time of this callback.)
                //
                // Note: If Network Partition state used for this decision is incorrect and there is a Network Partition it will get other
                //       chances to check the network state and adjust its behavior during onDataSent callbacks as a Leader.
                return ElectionTimeoutAction.BECOME_LEADER;
            }
        }
    }

    @Override
    public VoteSentAction onVoteSent(Candidate candidate) {

        // For Configured Primary
        if (isConfiguredPrimary(candidate)) {

            // There should be NO cases where an active Configured Primary should ever become a voting candidate.
            // One of the following cases should prevent this:
            //
            // RAFT Startup Rules:
            //  - Startup as a Follower (because active Leader exists in cluster and it gets its heartbeat
            //                           before own electionTimeout)
            //
            // onElectionTimeout:
            //  - Startup as a Leader   (because active Leader doesn't exists in cluster OR
            //                           heartbeat from that Leader's was missed before own electionTimeout)
            // onHeartbeatNotReceived:
            //  - Become Leader because previous active Leader:
            //      a) died
            //      b) its network links were unavailable
            //      c) a full Network Partition is occurring

            // In any case the Configured Primary is alive per this callback and should assume leadership.
            return VoteSentAction.BECOME_LEADER;

            // For Configured Secondary
        } else {
            if (networkPartitionDetectionEnabled && networkPartitionDetected) {

                if(activeActiveDeployment) {
                    return VoteSentAction.BECOME_LEADER;        // Activate Secondary

                } else {

                    // (Possibility: The network is NOT partitioned but we didn't detect it properly at the time of this callback.)
                    //
                    // Note: If Network Partition state used for this decision is incorrect and there is not a Network Partition the node will
                    //       get other chances to check the network state and adjust its behavior on addition onVoteSent callbacks as a Candidate.
                    //       It will be at least 1 more election term/timeout before the a Leader/Primary is defined.
                    return VoteSentAction.NONE;
                }

            } else {

                // (Possibility: There is a Network Partition but we didn't detect it properly at the time of this callback.)
                //
                // Note: If Network Partition state used for this decision is incorrect and there is a Network Partition it will get other
                //       chances to check the network state and adjust its behavior during onDataSent callbacks as a Primary/Leader.
                return VoteSentAction.BECOME_LEADER;
            }
        }
    }

    @Override
    public VoteReceivedAction onVoteReceived(Candidate candidate, int voteCount) {

        // (UNUSED)
        //
        // onElectionTimeout and onVoteSent will guarantee at least 1 Leader is elected depending on:
        //  - configuration
        //  - network parition state
        return VoteReceivedAction.NONE;
    }

    @Override
    public VoteRequestReceivedAction onVoteRequestReceived(Participant voter, Candidate candidate, String votedFor) {

        // (UNUSED)
        //
        // onElectionTimeout and onVoteSent will guarantee at least 1 Leader is elected depending on:
        //  - configuration
        //  - network parition state
        return VoteRequestReceivedAction.NONE;
    }

}
