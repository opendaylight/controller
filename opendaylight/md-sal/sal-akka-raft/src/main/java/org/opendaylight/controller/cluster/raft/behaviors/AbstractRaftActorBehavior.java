/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.internal.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import scala.concurrent.duration.FiniteDuration;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class that represents the behavior of a RaftActor
 * <p/>
 * All Servers:
 * <ul>
 * <li> If commitIndex > lastApplied: increment lastApplied, apply
 * log[lastApplied] to state machine (§5.3)
 * <li> If RPC request or response contains term T > currentTerm:
 * set currentTerm = T, convert to follower (§5.1)
 */
public abstract class AbstractRaftActorBehavior implements RaftActorBehavior {

    /**
     * Information about the RaftActor whose behavior this class represents
     */
    protected final RaftActorContext context;

    /**
     * The maximum election time variance
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;

    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     * <p/>
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     */
    protected static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);

    /**
     * The interval in which a new election would get triggered if no leader is found
     */
    private static final long ELECTION_TIME_INTERVAL =
        HEART_BEAT_INTERVAL.toMillis() * 2;

    /**
     *
     */

    private Cancellable electionCancel = null;


    protected AbstractRaftActorBehavior(RaftActorContext context) {
        this.context = context;
    }

    /**
     * Derived classes should not directly handle AppendEntries messages it
     * should let the base class handle it first. Once the base class handles
     * the AppendEntries message and does the common actions that are applicable
     * in all RaftState's it will delegate the handling of the AppendEntries
     * message to the derived class to do more state specific handling by calling
     * this method
     *
     * @param sender         The actor that sent this message
     * @param appendEntries  The AppendEntries message
     * @param suggestedState The state that the RaftActor should be in based
     *                       on the base class's processing of the AppendEntries
     *                       message
     * @return
     */
    protected abstract RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState suggestedState);


    protected RaftState appendEntries(ActorRef sender,
        AppendEntries appendEntries, RaftState raftState){

        // 1. Reply false if term < currentTerm (§5.1)
        if(appendEntries.getTerm() < currentTerm()){
            sender.tell(new AppendEntriesReply(currentTerm(), false), actor());
            return state();
        }

        // 2. Reply false if log doesn’t contain an entry at prevLogIndex
        // whose term matches prevLogTerm (§5.3)
        ReplicatedLogEntry previousEntry = context.getReplicatedLog()
            .get(appendEntries.getPrevLogIndex());

        if(previousEntry == null || previousEntry.getTerm() != appendEntries.getPrevLogTerm()){
            sender.tell(new AppendEntriesReply(currentTerm(), false), actor());
            return state();
        }

        if(appendEntries.getEntries() != null) {
            // 3. If an existing entry conflicts with a new one (same index
            // but different terms), delete the existing entry and all that
            // follow it (§5.3)
            int addEntriesFrom = 0;
            for (int i = 0;
                 i < appendEntries.getEntries().size(); i++, addEntriesFrom++) {
                ReplicatedLogEntry newEntry = context.getReplicatedLog()
                    .get(i + 1);

                if (newEntry != null && newEntry.getTerm() == appendEntries.getEntries().get(i).getTerm()){
                    break;
                }
                if (newEntry != null && newEntry.getTerm() != appendEntries
                    .getEntries().get(i).getTerm()) {
                    context.getReplicatedLog().removeFrom(i + 1);
                    break;
                }
            }

            // 4. Append any new entries not already in the log
            for (int i = addEntriesFrom;
                 i < appendEntries.getEntries().size(); i++) {
                context.getReplicatedLog()
                    .append(appendEntries.getEntries().get(i));
            }
        }


        // 5. If leaderCommit > commitIndex, set commitIndex =
        // min(leaderCommit, index of last new entry)
        context.setCommitIndex(Math.min(appendEntries.getLeaderCommit(),
            context.getReplicatedLog().last().getIndex()));

        // If commitIndex > lastApplied: increment lastApplied, apply
        // log[lastApplied] to state machine (§5.3)
        if (appendEntries.getLeaderCommit() > context.getLastApplied()) {
            applyLogToStateMachine(appendEntries.getLeaderCommit());
        }

        sender.tell(new AppendEntriesReply(currentTerm(), true), actor());

        return handleAppendEntries(sender, appendEntries, raftState);
    }

    /**
     * Derived classes should not directly handle AppendEntriesReply messages it
     * should let the base class handle it first. Once the base class handles
     * the AppendEntriesReply message and does the common actions that are
     * applicable in all RaftState's it will delegate the handling of the
     * AppendEntriesReply message to the derived class to do more state specific
     * handling by calling this method
     *
     * @param sender             The actor that sent this message
     * @param appendEntriesReply The AppendEntriesReply message
     * @param suggestedState     The state that the RaftActor should be in based
     *                           on the base class's processing of the
     *                           AppendEntriesReply message
     * @return
     */

    protected abstract RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply, RaftState suggestedState);

    protected RaftState requestVote(ActorRef sender,
        RequestVote requestVote, RaftState suggestedState) {

        boolean grantVote = false;

        //  Reply false if term < currentTerm (§5.1)
        if (requestVote.getTerm() < currentTerm()) {
            grantVote = false;

            // If votedFor is null or candidateId, and candidate’s log is at
            // least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
        } else if (votedFor() == null || votedFor()
            .equals(requestVote.getCandidateId())) {

            boolean candidateLatest = false;

            // From §5.4.1
            // Raft determines which of two logs is more up-to-date
            // by comparing the index and term of the last entries in the
            // logs. If the logs have last entries with different terms, then
            // the log with the later term is more up-to-date. If the logs
            // end with the same term, then whichever log is longer is
            // more up-to-date.
            if (requestVote.getLastLogTerm() > lastTerm()) {
                candidateLatest = true;
            } else if ((requestVote.getLastLogTerm() == lastTerm())
                && requestVote.getLastLogIndex() >= lastTerm()) {
                candidateLatest = true;
            }

            if (candidateLatest) {
                grantVote = true;
                context.getTermInformation().update(requestVote.getTerm(),
                    requestVote.getCandidateId());
            }
        }

        sender.tell(new RequestVoteReply(currentTerm(), grantVote), actor());

        return suggestedState;
    }

    /**
     * Derived classes should not directly handle RequestVoteReply messages it
     * should let the base class handle it first. Once the base class handles
     * the RequestVoteReply message and does the common actions that are
     * applicable in all RaftState's it will delegate the handling of the
     * RequestVoteReply message to the derived class to do more state specific
     * handling by calling this method
     *
     * @param sender           The actor that sent this message
     * @param requestVoteReply The RequestVoteReply message
     * @param suggestedState   The state that the RaftActor should be in based
     *                         on the base class's processing of the RequestVote
     *                         message
     * @return
     */

    protected abstract RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply, RaftState suggestedState);

    /**
     * @return The derived class should return the state that corresponds to
     * it's behavior
     */
    protected abstract RaftState state();

    protected FiniteDuration electionDuration() {
        long variance = new Random().nextInt(ELECTION_TIME_MAX_VARIANCE);
        return new FiniteDuration(ELECTION_TIME_INTERVAL + variance,
            TimeUnit.MILLISECONDS);
    }

    protected void scheduleElection(FiniteDuration interval) {

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

    protected long currentTerm() {
        return context.getTermInformation().getCurrentTerm();
    }

    protected String votedFor() {
        return context.getTermInformation().getVotedFor();
    }

    protected ActorRef actor() {
        return context.getActor();
    }

    protected long lastTerm() {
        return context.getReplicatedLog().last().getTerm();
    }

    protected long lastIndex() {
        return context.getReplicatedLog().last().getIndex();
    }


    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        RaftState raftState = state();
        if (message instanceof RaftRPC) {
            raftState = applyTerm((RaftRPC) message);
        }
        if (message instanceof AppendEntries) {
            raftState = appendEntries(sender, (AppendEntries) message,
                raftState);
        } else if (message instanceof AppendEntriesReply) {
            raftState =
                handleAppendEntriesReply(sender, (AppendEntriesReply) message,
                    raftState);
        } else if (message instanceof RequestVote) {
            raftState =
                requestVote(sender, (RequestVote) message, raftState);
        } else if (message instanceof RequestVoteReply) {
            raftState =
                handleRequestVoteReply(sender, (RequestVoteReply) message,
                    raftState);
        }
        return raftState;
    }

    private RaftState applyTerm(RaftRPC rpc) {
        // If RPC request or response contains term T > currentTerm:
        // set currentTerm = T, convert to follower (§5.1)
        // This applies to all RPC messages and responses
        if (rpc.getTerm() > context.getTermInformation().getCurrentTerm()) {
            context.getTermInformation().update(rpc.getTerm(), null);
            return RaftState.Follower;
        }
        return state();
    }

    private void applyLogToStateMachine(long index) {
        // Send a local message to the local RaftActor (it's derived class to be
        // specific to apply the log to it's index)
        context.setLastApplied(index);
    }


}
