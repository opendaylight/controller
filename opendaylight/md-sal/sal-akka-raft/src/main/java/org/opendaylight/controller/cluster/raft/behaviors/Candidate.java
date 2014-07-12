/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.internal.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The behavior of a RaftActor when it is in the CandidateState
 * <p>
 * Candidates (§5.2):
 * <ul>
 * <li> On conversion to candidate, start election:
 * <ul>
 * <li> Increment currentTerm
 * <li> Vote for self
 * <li> Reset election timer
 * <li> Send RequestVote RPCs to all other servers
 * </ul>
 * <li> If votes received from majority of servers: become leader
 * <li> If AppendEntries RPC received from new leader: convert to
 * follower
 * <li> If election timeout elapses: start new election
 * </ul>
 */
public class Candidate extends AbstractRaftActorBehavior {

    /**
     * The maximum election time variance
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;

    /**
     * The interval in which a new election would get triggered if no leader is found
     */
    private static final long ELECTION_TIME_INTERVAL = Leader.HEART_BEAT_INTERVAL.toMillis() * 2;

    /**
     *
     */
    private final Map<String, ActorSelection> peerToActor = new HashMap<>();

    private Cancellable electionCancel = null;

    private int voteCount;

    private final int votesRequired;

    public Candidate(RaftActorContext context, List<String> peerPaths) {
        super(context);

        for (String peerPath : peerPaths) {
            peerToActor.put(peerPath,
                context.actorSelection(peerPath));
        }

        if(peerPaths.size() > 0) {
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
            int noOfPeers = peerPaths.size();
            int self = 1;
            votesRequired = (noOfPeers + self) / 2 + 1;
        } else {
            votesRequired = 0;
        }

        scheduleElection(randomizedDuration());
    }

    @Override protected RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState) {

        // There is some peer who thinks it's a leader but is not
        // I will not accept this append entries
        sender.tell(new AppendEntriesReply(
            context.getTermInformation().getCurrentTerm().get(), false),
            context.getActor());

        return suggestedState;
    }

    @Override protected RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState) {

        // Some peer thinks I was a leader and sent me a reply

        return suggestedState;
    }

    @Override protected RaftState handleRequestVote(ActorRef sender,
        RequestVote requestVote, RaftState suggestedState) {

        // We got this RequestVote because the term in there is less than
        // or equal to our current term, so do not grant the vote
        sender.tell(new RequestVoteReply(
            context.getTermInformation().getCurrentTerm().get(), false),
            context.getActor());

        return suggestedState;
    }

    @Override protected RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState) {
        if(suggestedState == RaftState.Follower) {
            // If base class thinks I should be follower then I am
            return suggestedState;
        }

        if(requestVoteReply.isVoteGranted()){
            voteCount++;
        }

        if(voteCount >= votesRequired){
            return RaftState.Leader;
        }

        return state();
    }

    @Override protected RaftState state() {
        return RaftState.Candidate;
    }

    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        if(message instanceof ElectionTimeout){
            if(votesRequired == 0){
                // If there are no peers then we should be a Leader
                // We wait for the election timeout to occur before declare
                // ourselves the leader. This gives enough time for a leader
                // who we do not know about (as a peer)
                // to send a message to the candidate
                return RaftState.Leader;
            }
            scheduleElection(randomizedDuration());
            return state();
        }
        return super.handleMessage(sender, message);
    }

    private FiniteDuration randomizedDuration(){
        long variance = new Random().nextInt(ELECTION_TIME_MAX_VARIANCE);
        return new FiniteDuration(ELECTION_TIME_INTERVAL + variance, TimeUnit.MILLISECONDS);
    }

    private void scheduleElection(FiniteDuration interval) {

        // set voteCount back to 1 (that is voting for self)
        voteCount = 1;

        // Increment the election term and vote for self
        AtomicLong currentTerm = context.getTermInformation().getCurrentTerm();
        context.getTermInformation().update(currentTerm.incrementAndGet(), context.getId());

        // Request for a vote
        for(ActorSelection peerActor : peerToActor.values()){
            peerActor.tell(new RequestVote(
                context.getTermInformation().getCurrentTerm().get(),
                context.getId(), context.getReplicatedLog().last().getIndex(),
                context.getReplicatedLog().last().getTerm()),
                context.getActor());
        }

        if (electionCancel != null && !electionCancel.isCancelled()) {
            electionCancel.cancel();
        }

        // Schedule an election. When the scheduler triggers an ElectionTimeout
        // message is sent to itself
        electionCancel =
            context.getActorSystem().scheduler().scheduleOnce(interval,
                context.getActor(), new ElectionTimeout(),
                context.getActorSystem().dispatcher(), context.getActor());
    }

}
