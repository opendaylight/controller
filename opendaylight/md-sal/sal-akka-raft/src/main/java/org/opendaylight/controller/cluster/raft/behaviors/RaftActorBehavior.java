/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLog;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.raft.api.RaftRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that provides common code for a RaftActor behavior.
 */
public abstract class RaftActorBehavior implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorBehavior.class);

    /**
     * Information about the RaftActor whose behavior this class represents.
     */
    final @NonNull RaftActorContext context;

    /**
     * The RaftState corresponding to his behavior.
     */
    private final @NonNull RaftRole raftRole;

    /**
     * Prepended to log messages to provide appropriate context.
     */
    final @NonNull String logName;

    /**
     * Used to cancel a scheduled election.
     */
    private Cancellable electionCancel = null;

    /**
     * The index of the last log entry that has been replicated to all raft peers.
     */
    private long replicatedToAllIndex = -1;

    RaftActorBehavior(final RaftActorContext context, final RaftRole raftRole) {
        this.context = requireNonNull(context);
        this.raftRole = requireNonNull(raftRole);
        logName = memberId() + " (" + raftRole + ")";
    }

    final @NonNull String memberId() {
        return context.getId();
    }

    final @NonNull ReplicatedLog replicatedLog() {
        return context.getReplicatedLog();
    }

    /**
     * Returns the {@linkplain RaftRole} associated with this behavior.
     *
     * @return the {@linkplain RaftRole} constant
     */
    public final @NonNull RaftRole raftRole() {
        return raftRole;
    }

    /**
     * Returns the id of the leader.
     *
     * @return the id of the leader or null if not known
     */
    public abstract @Nullable String getLeaderId();

    /**
     * Returns the leader's payload data version.
     *
     * @return a short representing the version
     */
    public abstract short getLeaderPayloadVersion();

    /**
     * Sets the index of the last log entry that has been replicated to all peers.
     *
     * @param replicatedToAllIndex the index
     */
    public void setReplicatedToAllIndex(final long replicatedToAllIndex) {
        this.replicatedToAllIndex = replicatedToAllIndex;
    }

    /**
     * Returns the index of the last log entry that has been replicated to all peers.
     *
     * @return the index or -1 if not known
     */
    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    /**
     * Derived classes should not directly handle AppendEntries messages it should let the base class handle it first.
     * Once the base class handles the AppendEntries message and does the common actions that are applicable in all
     * RaftState's it will delegate the handling of the AppendEntries message to the derived class to do more state
     * specific handling by calling this method.
     *
     * @param sender         The actor that sent this message
     * @param appendEntries  The AppendEntries message
     * @return a new behavior if it was changed or the current behavior
     */
    abstract RaftActorBehavior handleAppendEntries(ActorRef sender, AppendEntries appendEntries);

    /**
     * Handles the common logic for the AppendEntries message and delegates handling to the derived class.
     *
     * @param sender the ActorRef that sent the message
     * @param appendEntries the message
     * @return a new behavior if it was changed or the current behavior
     */
    final RaftActorBehavior appendEntries(final ActorRef sender, final AppendEntries appendEntries) {
        // 1. Reply false if term < currentTerm (§5.1)
        final var term = appendEntries.getTerm();
        final var current = currentTerm();
        if (term < current) {
            LOG.info("{}: Cannot append entries because sender's term {} is less than {}", logName, term, current);
            sender.tell(new AppendEntriesReply(memberId(), current, false, lastIndex(), lastTerm(),
                    context.getPayloadVersion(), false, false, appendEntries.getLeaderRaftVersion()), actor());
            return this;
        }

        return handleAppendEntries(sender, appendEntries);
    }

    /**
     * Derived classes should not directly handle AppendEntriesReply messages it should let the base class handle it
     * first. Once the base class handles the AppendEntriesReply message and does the common actions that are applicable
     * in all RaftState's it will delegate the handling of the AppendEntriesReply message to the derived class to do
     * more state specific handling by calling this method
     *
     * @param sender             The actor that sent this message
     * @param appendEntriesReply The AppendEntriesReply message
     * @return a new behavior if it was changed or the current behavior
     */
    abstract RaftActorBehavior handleAppendEntriesReply(ActorRef sender, AppendEntriesReply appendEntriesReply);

    /**
     * Handles the logic for the RequestVote message that is common for all behaviors.
     *
     * @param sender the ActorRef that sent the message
     * @param requestVote the message
     * @return a new behavior if it was changed or the current behavior
     */
    final RaftActorBehavior requestVote(final ActorRef sender, final RequestVote requestVote) {
        LOG.debug("{}: In requestVote: {} - currentTerm: {}, votedFor: {}, lastIndex: {}, lastTerm: {}", logName,
                requestVote, currentTerm(), votedFor(), lastIndex(), lastTerm());

        final var grantVote = canGrantVote(requestVote);
        if (grantVote) {
            try {
                context.persistTermInfo(new TermInfo(requestVote.getTerm(), requestVote.getCandidateId()));
            } catch (IOException e) {
                // FIXME: do not mask IOException
                throw new UncheckedIOException(e);
            }
        }

        final var reply = new RequestVoteReply(currentTerm(), grantVote);
        LOG.debug("{}: requestVote returning: {}", logName, reply);
        sender.tell(reply, actor());
        return this;
    }

    final boolean canGrantVote(final RequestVote requestVote) {
        //  Reply false if term < currentTerm (§5.1)
        if (requestVote.getTerm() < currentTerm()) {
            return false;
        }

        // If votedFor is null or candidateId, and candidate’s log is at least as up-to-date as receiver’s log, we can
        // grant vote (§5.2, §5.4).
        final var votedFor = votedFor();
        if (votedFor != null && !votedFor.equals(requestVote.getCandidateId())) {
            return false;
        }

        // From §5.4.1
        // Raft determines which of two logs is more up-to-date
        // by comparing the index and term of the last entries in the
        // logs. If the logs have last entries with different terms, then
        // the log with the later term is more up-to-date. If the logs
        // end with the same term, then whichever log is longer is
        // more up-to-date.
        final var lastTerm = lastTerm();
        final var reqLastLogTerm = requestVote.getLastLogTerm();
        return reqLastLogTerm > lastTerm || reqLastLogTerm == lastTerm && requestVote.getLastLogIndex() >= lastIndex();
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
    abstract RaftActorBehavior handleRequestVoteReply(ActorRef sender, RequestVoteReply requestVoteReply);

    /**
     * Returns a duration for election with an additional variance for randomness.
     *
     * @return a random election duration
     */
    Duration electionDuration() {
        return context.getConfigParams().getElectionTimeOutInterval()
            .plusMillis(ThreadLocalRandom.current().nextInt(context.getConfigParams().getElectionTimeVariance()));
    }

    /**
     * Stops the currently scheduled election.
     */
    final void stopElection() {
        if (electionCancel != null && !electionCancel.isCancelled()) {
            electionCancel.cancel();
        }
    }

    final boolean canStartElection() {
        return context.getRaftPolicy().automaticElectionsEnabled() && context.isVotingMember();
    }

    /**
     * Schedule a new election.
     *
     * @param interval the duration after which we should trigger a new election
     */
    // Non-final for testing
    final void scheduleElection(final Duration interval) {
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
    final long currentTerm() {
        return context.currentTerm();
    }

    /**
     * Returns the id of the candidate that this server voted for in current term.
     *
     * @return the candidate for whom we voted in the current term
     */
    final String votedFor() {
        return context.termInfo().votedFor();
    }

    /**
     * Returns the actor associated with this behavior.
     *
     * @return the actor
     */
    final ActorRef actor() {
        return context.getActor();
    }

    /**
     * Returns the term of the last entry in the log.
     *
     * @return the term
     */
    final long lastTerm() {
        return replicatedLog().lastTerm();
    }

    /**
     * Returns the index of the last entry in the log.
     *
     * @return the index
     */
    final long lastIndex() {
        return replicatedLog().lastIndex();
    }

    /**
     * Returns the actual index of the entry in replicated log for the given index or -1 if not found.
     *
     * @return the log entry index or -1 if not found
     */
    final long getLogEntryIndex(final long index) {
        final var replLog = replicatedLog();
        if (index == replLog.getSnapshotIndex()) {
            return index;
        }

        final var entry = replLog.get(index);
        return entry != null ? entry.index() : -1;
    }

    /**
     * Returns the actual term of the entry in the replicated log for the given index or -1 if not found.
     *
     * @return the log entry term or -1 if not found
     */
    final long getLogEntryTerm(final long index) {
        final var replLog = replicatedLog();
        if (index == replLog.getSnapshotIndex()) {
            return replLog.getSnapshotTerm();
        }

        final var entry = replLog.get(index);
        return entry != null ? entry.term() : -1;
    }

    /**
     * Returns the actual term of the entry in the replicated log for the given index or, if not present, returns the
     * snapshot term if the given index is in the snapshot or -1 otherwise.
     *
     * @return the term or -1 otherwise
     */
    final long getLogEntryOrSnapshotTerm(final long index) {
        final var replLog = replicatedLog();
        return replLog.isInSnapshot(index) ? replLog.getSnapshotTerm() : getLogEntryTerm(index);
    }

    /**
     * Applies the log entries up to the specified index that is known to be committed to the state machine.
     *
     * @param index the log index
     */
    final void applyLogToStateMachine(final long index) {
        // Now maybe we apply to the state machine
        final var replLog = replicatedLog();

        for (long i = replLog.getLastApplied() + 1; i < index + 1; i++) {

            final var replicatedLogEntry = replLog.get(i);
            if (replicatedLogEntry != null) {
                // Send a local message to the local RaftActor (it's derived class to be
                // specific to apply the log to it's index)

                final var applyState = getApplyStateFor(replicatedLogEntry);

                LOG.debug("{}: Setting last applied to {}", logName, i);

                replLog.setLastApplied(i);
                context.getApplyStateConsumer().accept(applyState);
            } else {
                //if one index is not present in the log, no point in looping
                // around as the rest wont be present either
                LOG.warn("{}: Missing index {} from log. Cannot apply state. Ignoring {} to {}", logName, i, i, index);
                break;
            }
        }

        // send a message to persist a ApplyLogEntries marker message into akka's persistent journal
        // will be used during recovery
        //in case if the above code throws an error and this message is not sent, it would be fine
        // as the  append entries received later would initiate add this message to the journal
        actor().tell(new ApplyJournalEntries(replLog.getLastApplied()), actor());
    }

    /**
     * Create an ApplyState message for a particular log entry so we can determine how to apply this entry.
     *
     * @param entry the log entry
     * @return ApplyState for this entry
     */
    abstract ApplyState getApplyStateFor(ReplicatedLogEntry entry);

    /**
     * Handle a message. If the processing of the message warrants a state
     * change then a new behavior should be returned otherwise this method should
     * return the current behavior.
     *
     * @param sender The sender of the message
     * @param message A message that needs to be processed
     *
     * @return The new behavior or current behavior, or null if the message was not handled.
     */
    public @Nullable RaftActorBehavior handleMessage(final ActorRef sender, final Object message) {
        return switch (message) {
            case AppendEntries appendEntries -> appendEntries(sender, appendEntries);
            case AppendEntriesReply appendEntriesReply -> handleAppendEntriesReply(sender, appendEntriesReply);
            case RequestVote requestVote -> requestVote(sender, requestVote);
            case RequestVoteReply requestVoteReply -> handleRequestVoteReply(sender, requestVoteReply);
            default -> null;
        };
    }

    @Override
    public abstract void close();

    /**
     * Closes the current behavior and switches to the specified behavior, if possible.
     *
     * @param behavior the new behavior to switch to
     * @return the new behavior
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    final RaftActorBehavior switchBehavior(final RaftActorBehavior newBehavior) {
        if (!context.getRaftPolicy().automaticElectionsEnabled()) {
            return this;
        }

        LOG.info("{} :- Switching from behavior {} to {}, election term: {}", logName, raftRole(), newBehavior.raftRole(),
            context.currentTerm());
        try {
            close();
        } catch (RuntimeException e) {
            LOG.warn("{}: Failed to close behavior : {}", logName, raftRole(), e);
        }
        return newBehavior;
    }

    static final int getMajorityVoteCount(final int numPeers) {
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
    final void performSnapshotWithoutCapture(final long snapshotCapturedIndex) {
        long actualIndex = context.getSnapshotManager().trimLog(snapshotCapturedIndex);

        if (actualIndex != -1) {
            setReplicatedToAllIndex(actualIndex);
        }
    }

    // Check whether we should update the term. In case of half-connected nodes, we want to ignore RequestVote
    // messages, as the candidate is not able to receive our response.
    final boolean shouldUpdateTerm(final RaftRPC rpc) {
        if (!(rpc instanceof RequestVote requestVote)) {
            return true;
        }

        LOG.debug("{}: Found higher term in RequestVote rpc, verifying whether it's safe to update term.", logName);
        final var cluster = context.cluster();
        if (cluster == null) {
            return true;
        }

        final var unreachable = cluster.state().getUnreachable();
        LOG.debug("{}: Cluster state: {}", logName, unreachable);

        for (var member : unreachable) {
            for (var role : member.getRoles()) {
                if (requestVote.getCandidateId().startsWith(role)) {
                    LOG.debug("{}: Unreachable member: {}, matches candidateId in: {}, not updating term", logName,
                        member, requestVote);
                    return false;
                }
            }
        }

        LOG.debug("{}: Candidate in requestVote:{} with higher term appears reachable, updating term.", logName,
            requestVote);
        return true;
    }
}
