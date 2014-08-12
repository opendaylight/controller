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
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
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
     *
     */
    private Cancellable electionCancel = null;

    /**
     *
     */
    protected String leaderId = null;


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
     * @return
     */
    protected abstract RaftState handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries);


    /**
     * appendEntries first processes the AppendEntries message and then
     * delegates handling to a specific behavior
     *
     * @param sender
     * @param appendEntries
     * @return
     */
    protected RaftState appendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        // 1. Reply false if term < currentTerm (§5.1)
        if (appendEntries.getTerm() < currentTerm()) {
            context.getLogger().debug(
                "Cannot append entries because sender term " + appendEntries
                    .getTerm() + " is less than " + currentTerm());
            sender.tell(
                new AppendEntriesReply(context.getId(), currentTerm(), false,
                    lastIndex(), lastTerm()), actor()
            );
            return state();
        }


        return handleAppendEntries(sender, appendEntries);
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
     * @return
     */
    protected abstract RaftState handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply);

    /**
     * requestVote handles the RequestVote message. This logic is common
     * for all behaviors
     *
     * @param sender
     * @param requestVote
     * @return
     */
    protected RaftState requestVote(ActorRef sender,
        RequestVote requestVote) {


        context.getLogger().debug(requestVote.toString());

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
                && requestVote.getLastLogIndex() >= lastIndex()) {
                candidateLatest = true;
            }

            if (candidateLatest) {
                grantVote = true;
                context.getTermInformation().updateAndPersist(requestVote.getTerm(),
                    requestVote.getCandidateId());
            }
        }

        sender.tell(new RequestVoteReply(currentTerm(), grantVote), actor());

        return state();
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
     * @return
     */
    protected abstract RaftState handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply);

    /**
     * Creates a random election duration
     *
     * @return
     */
    protected FiniteDuration electionDuration() {
        long variance = new Random().nextInt(context.getConfigParams().getElectionTimeVariance());
        return context.getConfigParams().getElectionTimeOutInterval().$plus(
            new FiniteDuration(variance, TimeUnit.MILLISECONDS));
    }

    /**
     * stop the scheduled election
     */
    protected void stopElection() {
        if (electionCancel != null && !electionCancel.isCancelled()) {
            electionCancel.cancel();
        }
    }

    /**
     * schedule a new election
     *
     * @param interval
     */
    protected void scheduleElection(FiniteDuration interval) {
        stopElection();

        // Schedule an election. When the scheduler triggers an ElectionTimeout
        // message is sent to itself
        electionCancel =
            context.getActorSystem().scheduler().scheduleOnce(interval,
                context.getActor(), new ElectionTimeout(),
                context.getActorSystem().dispatcher(), context.getActor());
    }

    /**
     * Get the current term
     * @return
     */
    protected long currentTerm() {
        return context.getTermInformation().getCurrentTerm();
    }

    /**
     * Get the candidate for whom we voted in the current term
     * @return
     */
    protected String votedFor() {
        return context.getTermInformation().getVotedFor();
    }

    /**
     * Get the actor associated with this behavior
     * @return
     */
    protected ActorRef actor() {
        return context.getActor();
    }

    /**
     * Get the term from the last entry in the log
     *
     * @return
     */
    protected long lastTerm() {
        return context.getReplicatedLog().lastTerm();
    }

    /**
     * Get the index from the last entry in the log
     *
     * @return
     */
    protected long lastIndex() {
        return context.getReplicatedLog().lastIndex();
    }

    /**
     * Find the client request tracker for a specific logIndex
     *
     * @param logIndex
     * @return
     */
    protected ClientRequestTracker findClientRequestTracker(long logIndex) {
        return null;
    }

    /**
     * Find the log index from the previous to last entry in the log
     *
     * @return
     */
    protected long prevLogIndex(long index){
        ReplicatedLogEntry prevEntry =
            context.getReplicatedLog().get(index - 1);
        if (prevEntry != null) {
            return prevEntry.getIndex();
        }
        return -1;
    }

    /**
     * Find the log term from the previous to last entry in the log
     * @return
     */
    protected long prevLogTerm(long index){
        ReplicatedLogEntry prevEntry =
            context.getReplicatedLog().get(index - 1);
        if (prevEntry != null) {
            return prevEntry.getTerm();
        }
        return -1;
    }

    /**
     * Apply the provided index to the state machine
     *
     * @param index a log index that is known to be committed
     */
    protected void applyLogToStateMachine(final long index) {
        // Now maybe we apply to the state machine
        for (long i = context.getLastApplied() + 1;
             i < index + 1; i++) {
            ActorRef clientActor = null;
            String identifier = null;
            ClientRequestTracker tracker = findClientRequestTracker(i);

            if (tracker != null) {
                clientActor = tracker.getClientActor();
                identifier = tracker.getIdentifier();
            }
            ReplicatedLogEntry replicatedLogEntry =
                context.getReplicatedLog().get(i);

            if (replicatedLogEntry != null) {
                actor().tell(new ApplyState(clientActor, identifier,
                    replicatedLogEntry), actor());
            } else {
                context.getLogger().error(
                    "Missing index " + i + " from log. Cannot apply state.");
            }
        }
        // Send a local message to the local RaftActor (it's derived class to be
        // specific to apply the log to it's index)
        context.getLogger().debug("Setting last applied to {}", index);
        context.setLastApplied(index);
    }

    protected Object fromSerializableMessage(Object serializable){
        return SerializationUtils.fromSerializable(serializable);
    }

    @Override
    public RaftState handleMessage(ActorRef sender, Object message) {
        if (message instanceof AppendEntries) {
            return appendEntries(sender, (AppendEntries) message);
        } else if (message instanceof AppendEntriesReply) {
            return handleAppendEntriesReply(sender, (AppendEntriesReply) message);
        } else if (message instanceof RequestVote) {
            return requestVote(sender, (RequestVote) message);
        } else if (message instanceof RequestVoteReply) {
            return handleRequestVoteReply(sender, (RequestVoteReply) message);
        }
        return state();
    }

    @Override public String getLeaderId() {
        return leaderId;
    }
}
