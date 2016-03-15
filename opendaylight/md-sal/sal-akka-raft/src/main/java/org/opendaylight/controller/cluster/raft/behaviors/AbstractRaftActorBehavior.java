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
import org.opendaylight.controller.cluster.raft.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.RequestVoteReply;
import org.slf4j.Logger;
import scala.concurrent.duration.FiniteDuration;

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
    protected final Logger LOG;

    /**
     *
     */
    private Cancellable electionCancel = null;

    /**
     *
     */
    protected String leaderId = null;

    private short leaderPayloadVersion = -1;

    private long replicatedToAllIndex = -1;

    private final String logName;

    private final RaftState state;

    AbstractRaftActorBehavior(final RaftActorContext context, final RaftState state) {
        this.context = Preconditions.checkNotNull(context);
        this.state = Preconditions.checkNotNull(state);
        this.LOG = context.getLogger();

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
            default:
                throw new IllegalArgumentException("Unhandled state " + state);
        }
    }

    @Override
    public final RaftState state() {
        return state;
    }

    public String logName() {
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
     * appendEntries first processes the AppendEntries message and then
     * delegates handling to a specific behavior
     *
     * @param sender
     * @param appendEntries
     * @return a new behavior if it was changed or the current behavior
     */
    protected RaftActorBehavior appendEntries(ActorRef sender,
        AppendEntries appendEntries) {

        // 1. Reply false if term < currentTerm (§5.1)
        if (appendEntries.getTerm() < currentTerm()) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Cannot append entries because sender term {} is less than {}",
                        logName(), appendEntries.getTerm(), currentTerm());
            }

            sender.tell(
                new AppendEntriesReply(context.getId(), currentTerm(), false,
                    lastIndex(), lastTerm(), context.getPayloadVersion()), actor()
            );
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
     * requestVote handles the RequestVote message. This logic is common
     * for all behaviors
     *
     * @param sender
     * @param requestVote
     * @return a new behavior if it was changed or the current behavior
     */
    protected RaftActorBehavior requestVote(ActorRef sender, RequestVote requestVote) {

        LOG.debug("{}: In requestVote:  {}", logName(), requestVote);

        boolean grantVote = canGrantVote(requestVote);

        if(grantVote) {
            context.getTermInformation().updateAndPersist(requestVote.getTerm(), requestVote.getCandidateId());
        }

        RequestVoteReply reply = new RequestVoteReply(currentTerm(), grantVote);

        LOG.debug("{}: requestVote returning: {}", logName(), reply);

        sender.tell(reply, actor());

        return this;
    }

    protected boolean canGrantVote(RequestVote requestVote){
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
     *
     * @return a random election duration
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

    protected boolean canStartElection() {
        return context.getRaftPolicy().automaticElectionsEnabled() && context.isVotingMember();
    }

    /**
     * schedule a new election
     *
     * @param interval the duration after which we should trigger a new election
     */
    protected void scheduleElection(FiniteDuration interval) {
        stopElection();

        if(canStartElection()) {
            // Schedule an election. When the scheduler triggers an ElectionTimeout message is sent to itself
            electionCancel = context.getActorSystem().scheduler().scheduleOnce(interval, context.getActor(),
                    ElectionTimeout.INSTANCE, context.getActorSystem().dispatcher(), context.getActor());
        }
    }

    /**
     * @return the current term
     */
    protected long currentTerm() {
        return context.getTermInformation().getCurrentTerm();
    }

    /**
     * @return the candidate for whom we voted in the current term
     */
    protected String votedFor() {
        return context.getTermInformation().getVotedFor();
    }

    /**
     * @return the actor associated with this behavior
     */
    protected ActorRef actor() {
        return context.getActor();
    }

    /**
     *
     * @return the term from the last entry in the log
     */
    protected long lastTerm() {
        return context.getReplicatedLog().lastTerm();
    }

    /**
     * @return the index from the last entry in the log
     */
    protected long lastIndex() {
        return context.getReplicatedLog().lastIndex();
    }

    /**
     * @param logIndex
     * @return the client request tracker for the specified logIndex
     */
    protected ClientRequestTracker findClientRequestTracker(long logIndex) {
        return null;
    }

    /**
     * @param logIndex
     * @return the client request tracker for the specified logIndex
     */
    protected ClientRequestTracker removeClientRequestTracker(long logIndex) {
        return null;
    }


    /**
     *
     * @return log index from the previous to last entry in the log
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
     * @return log term from the previous to last entry in the log
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
        long newLastApplied = context.getLastApplied();
        // Now maybe we apply to the state machine
        for (long i = context.getLastApplied() + 1;
             i < index + 1; i++) {
            ActorRef clientActor = null;
            String identifier = null;
            ClientRequestTracker tracker = removeClientRequestTracker(i);

            if (tracker != null) {
                clientActor = tracker.getClientActor();
                identifier = tracker.getIdentifier();
            }
            ReplicatedLogEntry replicatedLogEntry =
                context.getReplicatedLog().get(i);

            if (replicatedLogEntry != null) {
                // Send a local message to the local RaftActor (it's derived class to be
                // specific to apply the log to it's index)
                actor().tell(new ApplyState(clientActor, identifier,
                    replicatedLogEntry), actor());
                newLastApplied = i;
            } else {
                //if one index is not present in the log, no point in looping
                // around as the rest wont be present either
                LOG.warn(
                        "{}: Missing index {} from log. Cannot apply state. Ignoring {} to {}",
                        logName(), i, i, index);
                break;
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Setting last applied to {}", logName(), newLastApplied);
        }
        context.setLastApplied(newLastApplied);

        // send a message to persist a ApplyLogEntries marker message into akka's persistent journal
        // will be used during recovery
        //in case if the above code throws an error and this message is not sent, it would be fine
        // as the  append entries received later would initiate add this message to the journal
        actor().tell(new ApplyJournalEntries(context.getLastApplied()), actor());
    }

    protected Object fromSerializableMessage(Object serializable){
        return SerializationUtils.fromSerializable(serializable);
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
        }
        return this;
    }

    @Override public String getLeaderId() {
        return leaderId;
    }

    @Override
    public short getLeaderPayloadVersion() {
        return leaderPayloadVersion;
    }

    public void setLeaderPayloadVersion(short leaderPayloadVersion) {
        this.leaderPayloadVersion = leaderPayloadVersion;
    }

    @Override
    public RaftActorBehavior switchBehavior(RaftActorBehavior behavior) {
        return internalSwitchBehavior(behavior);
    }

    protected RaftActorBehavior internalSwitchBehavior(RaftState newState) {
        if(context.getRaftPolicy().automaticElectionsEnabled()){
            return internalSwitchBehavior(createBehavior(context, newState));
        }
        return this;
    }

    private RaftActorBehavior internalSwitchBehavior(RaftActorBehavior newBehavior) {
        LOG.info("{} :- Switching from behavior {} to {}", logName(), this.state(), newBehavior.state());
        try {
            close();
        } catch (Exception e) {
            LOG.error("{}: Failed to close behavior : {}", logName(), this.state(), e);
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
     * Performs a snapshot with no capture on the replicated log.
     * It clears the log from the supplied index or last-applied-1 which ever is minimum.
     *
     * @param snapshotCapturedIndex
     */
    protected void performSnapshotWithoutCapture(final long snapshotCapturedIndex) {
        long actualIndex = context.getSnapshotManager().trimLog(snapshotCapturedIndex);

        if(actualIndex != -1){
            setReplicatedToAllIndex(actualIndex);
        }
    }

    protected String getId(){
        return context.getId();
    }
}
