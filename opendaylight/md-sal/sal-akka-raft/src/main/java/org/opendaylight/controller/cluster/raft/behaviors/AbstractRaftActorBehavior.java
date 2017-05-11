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
import com.google.common.base.Preconditions;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.raft.ClientRequestTracker;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.slf4j.Logger;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract class that provides common code for a RaftActor behavior.
 */
public abstract class AbstractRaftActorBehavior implements RaftActorBehavior {
    /**
     * Information about the RaftActor whose behavior this class represents.
     */
    protected final RaftActorContext context;

    /**
     * Used for message logging.
     */
    protected final Logger log;

    /**
     * Prepended to log messages to provide appropriate context.
     */
    private final String logName;

    /**
     * The RaftState corresponding to his behavior.
     */
    private final RaftState state;

    /**
     * Used to cancel a scheduled election.
     */
    private Cancellable electionCancel = null;

    /**
     * The index of the last log entry that has been replicated to all raft peers.
     */
    private long replicatedToAllIndex = -1;

    AbstractRaftActorBehavior(final RaftActorContext context, final RaftState state) {
        this.context = Preconditions.checkNotNull(context);
        this.state = Preconditions.checkNotNull(state);
        this.log = context.getLogger();

        logName = String.format("%s (%s)", context.getId(), state);
    }

    public static RaftActorBehavior createBehavior(final RaftActorContext context, final RaftState state) {
        switch (state) {
            case Candidate:
                return new Candidate(context);
            case Follower:
                return new Follower(context);
            case IsolatedLeader:
                return new IsolatedLeader(context);
            case Leader:
                return new Leader(context);
            case PreLeader:
                return new PreLeader(context);
            default:
                throw new IllegalArgumentException("Unhandled state " + state);
        }
    }

    @Override
    public final RaftState state() {
        return state;
    }

    protected final String logName() {
        return logName;
    }

    @Override
    public void setReplicatedToAllIndex(long replicatedToAllIndex) {
        this.replicatedToAllIndex = replicatedToAllIndex;
    }

    @Override
    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
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
     * @return a new behavior if it was changed or the current behavior
     */
    protected abstract RaftActorBehavior handleAppendEntries(ActorRef sender,
        AppendEntries appendEntries);

    /**
     * Handles the common logic for the AppendEntries message and delegates handling to the derived class.
     *
     * @param sender the ActorRef that sent the message
     * @param appendEntries the message
     * @return a new behavior if it was changed or the current behavior
     */
    protected RaftActorBehavior appendEntries(ActorRef sender, AppendEntries appendEntries) {

        // 1. Reply false if term < currentTerm (§5.1)
        if (appendEntries.getTerm() < currentTerm()) {
            log.info("{}: Cannot append entries because sender's term {} is less than {}", logName(),
                    appendEntries.getTerm(), currentTerm());

            sender.tell(new AppendEntriesReply(context.getId(), currentTerm(), false, lastIndex(), lastTerm(),
                    context.getPayloadVersion()), actor());
            return this;
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
     * @return a new behavior if it was changed or the current behavior
     */
    protected abstract RaftActorBehavior handleAppendEntriesReply(ActorRef sender,
        AppendEntriesReply appendEntriesReply);

    /**
     * Handles the logic for the RequestVote message that is common for all behaviors.
     *
     * @param sender the ActorRef that sent the message
     * @param requestVote the message
     * @return a new behavior if it was changed or the current behavior
     */
    protected RaftActorBehavior requestVote(ActorRef sender, RequestVote requestVote) {

        log.debug("{}: In requestVote:  {} - currentTerm: {}, votedFor: {}, lastIndex: {}, lastTerm: {}", logName(),
                requestVote, currentTerm(), votedFor(), lastIndex(), lastTerm());

        boolean grantVote = canGrantVote(requestVote);

        if (grantVote) {
            context.getTermInformation().updateAndPersist(requestVote.getTerm(), requestVote.getCandidateId());
        }

        RequestVoteReply reply = new RequestVoteReply(currentTerm(), grantVote);

        log.debug("{}: requestVote returning: {}", logName(), reply);

        sender.tell(reply, actor());

        return this;
    }

    protected boolean canGrantVote(RequestVote requestVote) {
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
            } else if (requestVote.getLastLogTerm() == lastTerm()
                    && requestVote.getLastLogIndex() >= lastIndex()) {
                candidateLatest = true;
            }

            if (candidateLatest) {
                grantVote = true;
            }
        }
        return grantVote;
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
     * @return a new behavior if it was changed or the current behavior
     */
    protected abstract RaftActorBehavior handleRequestVoteReply(ActorRef sender,
        RequestVoteReply requestVoteReply);

    /**
     * Returns a duration for election with an additional variance for randomness.
     *
     * @return a random election duration
     */
    protected FiniteDuration electionDuration() {
        long variance = new Random().nextInt(context.getConfigParams().getElectionTimeVariance());
        return context.getConfigParams().getElectionTimeOutInterval().$plus(
                new FiniteDuration(variance, TimeUnit.MILLISECONDS));
    }

    /**
     * Stops the currently scheduled election.
     */
    protected void stopElection() {
        if (electionCancel != null && !electionCancel.isCancelled()) {
            electionCancel.cancel();
        }
    }

    protected boolean canStartElection() {
        return context.getRaftPolicy().automaticElectionsEnabled() && context.isVotingMember();
    }

    /**
     * Schedule a new election.
     *
     * @param interval the duration after which we should trigger a new election
     */
    protected void scheduleElection(FiniteDuration interval) {
        stopElection();

        // Schedule an election. When the scheduler triggers an ElectionTimeout message is sent to itself
        electionCancel = context.getActorSystem().scheduler().scheduleOnce(interval, context.getActor(),
                ElectionTimeout.INSTANCE, context.getActorSystem().dispatcher(), context.getActor());
    }

    /**
     * Returns the current election term.
     *
     * @return the current term
     */
    protected long currentTerm() {
        return context.getTermInformation().getCurrentTerm();
    }

    /**
     * Returns the id of the candidate that this server voted for in current term.
     *
     * @return the candidate for whom we voted in the current term
     */
    protected String votedFor() {
        return context.getTermInformation().getVotedFor();
    }

    /**
     * Returns the actor associated with this behavior.
     *
     * @return the actor
     */
    protected ActorRef actor() {
        return context.getActor();
    }

    /**
     * Returns the term of the last entry in the log.
     *
     * @return the term
     */
    protected long lastTerm() {
        return context.getReplicatedLog().lastTerm();
    }

    /**
     * Returns the index of the last entry in the log.
     *
     * @return the index
     */
    protected long lastIndex() {
        return context.getReplicatedLog().lastIndex();
    }

    /**
     * Removes and returns the ClientRequestTracker for the specified log index.
     * @param logIndex the log index
     * @return the ClientRequestTracker or null if none available
     */
    protected ClientRequestTracker removeClientRequestTracker(long logIndex) {
        return null;
    }

    /**
     * Returns the actual index of the entry in replicated log for the given index or -1 if not found.
     *
     * @return the log entry index or -1 if not found
     */
    protected long getLogEntryIndex(long index) {
        if (index == context.getReplicatedLog().getSnapshotIndex()) {
            return context.getReplicatedLog().getSnapshotIndex();
        }

        ReplicatedLogEntry entry = context.getReplicatedLog().get(index);
        if (entry != null) {
            return entry.getIndex();
        }

        return -1;
    }

    /**
     * Returns the actual term of the entry in replicated log for the given index or -1 if not found.
     *
     * @return the log entry term or -1 if not found
     */
    protected long getLogEntryTerm(long index) {
        if (index == context.getReplicatedLog().getSnapshotIndex()) {
            return context.getReplicatedLog().getSnapshotTerm();
        }

        ReplicatedLogEntry entry = context.getReplicatedLog().get(index);
        if (entry != null) {
            return entry.getTerm();
        }

        return -1;
    }

    /**
     * Applies the log entries up to the specified index that is known to be committed to the state machine.
     *
     * @param index the log index
     */
    protected void applyLogToStateMachine(final long index) {
        // Now maybe we apply to the state machine
        for (long i = context.getLastApplied() + 1; i < index + 1; i++) {

            ReplicatedLogEntry replicatedLogEntry = context.getReplicatedLog().get(i);
            if (replicatedLogEntry != null) {
                // Send a local message to the local RaftActor (it's derived class to be
                // specific to apply the log to it's index)

                final ApplyState applyState;
                final ClientRequestTracker tracker = removeClientRequestTracker(i);
                if (tracker != null) {
                    applyState = new ApplyState(tracker.getClientActor(), tracker.getIdentifier(), replicatedLogEntry);
                } else {
                    applyState = new ApplyState(null, null, replicatedLogEntry);
                }

                log.debug("{}: Setting last applied to {}", logName(), i);

                context.setLastApplied(i);
                context.getApplyStateConsumer().accept(applyState);
            } else {
                //if one index is not present in the log, no point in looping
                // around as the rest wont be present either
                log.warn("{}: Missing index {} from log. Cannot apply state. Ignoring {} to {}",
                        logName(), i, i, index);
                break;
            }
        }

        // send a message to persist a ApplyLogEntries marker message into akka's persistent journal
        // will be used during recovery
        //in case if the above code throws an error and this message is not sent, it would be fine
        // as the  append entries received later would initiate add this message to the journal
        actor().tell(new ApplyJournalEntries(context.getLastApplied()), actor());
    }

    @Override
    public RaftActorBehavior handleMessage(ActorRef sender, Object message) {
        if (message instanceof AppendEntries) {
            return appendEntries(sender, (AppendEntries) message);
        } else if (message instanceof AppendEntriesReply) {
            return handleAppendEntriesReply(sender, (AppendEntriesReply) message);
        } else if (message instanceof RequestVote) {
            return requestVote(sender, (RequestVote) message);
        } else if (message instanceof RequestVoteReply) {
            return handleRequestVoteReply(sender, (RequestVoteReply) message);
        } else {
            return null;
        }
    }

    @Override
    public RaftActorBehavior switchBehavior(RaftActorBehavior behavior) {
        return internalSwitchBehavior(behavior);
    }

    protected RaftActorBehavior internalSwitchBehavior(RaftState newState) {
        return internalSwitchBehavior(createBehavior(context, newState));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected RaftActorBehavior internalSwitchBehavior(RaftActorBehavior newBehavior) {
        if (!context.getRaftPolicy().automaticElectionsEnabled()) {
            return this;
        }

        log.info("{} :- Switching from behavior {} to {}, election term: {}", logName(), this.state(),
                newBehavior.state(), context.getTermInformation().getCurrentTerm());
        try {
            close();
        } catch (RuntimeException e) {
            log.error("{}: Failed to close behavior : {}", logName(), this.state(), e);
        }
        return newBehavior;
    }


    protected int getMajorityVoteCount(int numPeers) {
        // Votes are required from a majority of the peers including self.
        // The numMajority field therefore stores a calculated value
        // of the number of votes required for this candidate to win an
        // election based on it's known peers.
        // If a peer was added during normal operation and raft replicas
        // came to know about them then the new peer would also need to be
        // taken into consideration when calculating this value.
        // Here are some examples for what the numMajority would be for n
        // peers
        // 0 peers = 1 numMajority -: (0 + 1) / 2 + 1 = 1
        // 2 peers = 2 numMajority -: (2 + 1) / 2 + 1 = 2
        // 4 peers = 3 numMajority -: (4 + 1) / 2 + 1 = 3

        int numMajority = 0;
        if (numPeers > 0) {
            int self = 1;
            numMajority = (numPeers + self) / 2 + 1;
        }
        return numMajority;

    }


    /**
     * Performs a snapshot with no capture on the replicated log. It clears the log from the supplied index or
     * lastApplied-1 which ever is minimum.
     *
     * @param snapshotCapturedIndex the index from which to clear
     */
    protected void performSnapshotWithoutCapture(final long snapshotCapturedIndex) {
        long actualIndex = context.getSnapshotManager().trimLog(snapshotCapturedIndex);

        if (actualIndex != -1) {
            setReplicatedToAllIndex(actualIndex);
        }
    }

    protected String getId() {
        return context.getId();
    }
}
