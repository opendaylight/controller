/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.NonPersistentDataProvider;
import org.opendaylight.controller.cluster.PersistentDataProvider;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.DelegatingRaftActorBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RaftActor encapsulates a state machine that needs to be kept synchronized
 * in a cluster. It implements the RAFT algorithm as described in the paper
 * <a href='https://ramcloud.stanford.edu/wiki/download/attachments/11370504/raft.pdf'>
 * In Search of an Understandable Consensus Algorithm</a>
 * <p/>
 * RaftActor has 3 states and each state has a certain behavior associated
 * with it. A Raft actor can behave as,
 * <ul>
 * <li> A Leader </li>
 * <li> A Follower (or) </li>
 * <li> A Candidate </li>
 * </ul>
 * <p/>
 * <p/>
 * A RaftActor MUST be a Leader in order to accept requests from clients to
 * change the state of it's encapsulated state machine. Once a RaftActor becomes
 * a Leader it is also responsible for ensuring that all followers ultimately
 * have the same log and therefore the same state machine as itself.
 * <p/>
 * <p/>
 * The current behavior of a RaftActor determines how election for leadership
 * is initiated and how peer RaftActors react to request for votes.
 * <p/>
 * <p/>
 * Each RaftActor also needs to know the current election term. It uses this
 * information for a couple of things. One is to simply figure out who it
 * voted for in the last election. Another is to figure out if the message
 * it received to update it's state is stale.
 * <p/>
 * <p/>
 * The RaftActor uses akka-persistence to store it's replicated log.
 * Furthermore through it's behaviors a Raft Actor determines
 * <p/>
 * <ul>
 * <li> when a log entry should be persisted </li>
 * <li> when a log entry should be applied to the state machine (and) </li>
 * <li> when a snapshot should be saved </li>
 * </ul>
 */
public abstract class RaftActor extends AbstractUntypedPersistentActor {

    private static final long APPLY_STATE_DELAY_THRESHOLD_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(50L); // 50 millis

    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * The current state determines the current behavior of a RaftActor
     * A Raft Actor always starts off in the Follower State
     */
    private final DelegatingRaftActorBehavior currentBehavior = new DelegatingRaftActorBehavior();

    /**
     * This context should NOT be passed directly to any other actor it is
     * only to be consumed by the RaftActorBehaviors
     */
    private final RaftActorContextImpl context;

    private final DelegatingPersistentDataProvider delegatingPersistenceProvider = new DelegatingPersistentDataProvider(null);

    private RaftActorRecoverySupport raftRecovery;

    private RaftActorSnapshotMessageSupport snapshotSupport;

    private final BehaviorStateHolder reusableBehaviorStateHolder = new BehaviorStateHolder();

    public RaftActor(String id, Map<String, String> peerAddresses) {
        this(id, peerAddresses, Optional.<ConfigParams>absent());
    }

    public RaftActor(String id, Map<String, String> peerAddresses,
         Optional<ConfigParams> configParams) {

        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(), id, new ElectionTermImpl(delegatingPersistenceProvider, id, LOG),
            -1, -1, peerAddresses,
            (configParams.isPresent() ? configParams.get(): new DefaultConfigParamsImpl()),
            delegatingPersistenceProvider, LOG);

        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context, delegatingPersistenceProvider, currentBehavior));
    }

    @Override
    public void preStart() throws Exception {
        LOG.info("Starting recovery for {} with journal batch size {}", persistenceId(),
                context.getConfigParams().getJournalRecoveryLogBatchSize());

        super.preStart();

        snapshotSupport = newRaftActorSnapshotMessageSupport();
    }

    @Override
    public void postStop() {
        if(currentBehavior.getDelegate() != null) {
            try {
                currentBehavior.close();
            } catch (Exception e) {
                LOG.debug("{}: Error closing behavior {}", persistenceId(), currentBehavior.state());
            }
        }

        super.postStop();
    }

    @Override
    public void handleRecover(Object message) {
        if(raftRecovery == null) {
            raftRecovery = newRaftActorRecoverySupport();
        }

        boolean recoveryComplete = raftRecovery.handleRecoveryMessage(message);
        if(recoveryComplete) {
            if(!persistence().isRecoveryApplicable()) {
                // Delete all the messages from the akka journal so that we do not end up with consistency issues
                // Note I am not using the dataPersistenceProvider and directly using the akka api here
                deleteMessages(lastSequenceNr());

                // Delete all the akka snapshots as they will not be needed
                deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), scala.Long.MaxValue()));
            }

            onRecoveryComplete();

            initializeBehavior();

            raftRecovery = null;
        }
    }

    protected RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return new RaftActorRecoverySupport(delegatingPersistenceProvider, context, currentBehavior,
                getRaftActorRecoveryCohort());
    }

    protected void initializeBehavior(){
        changeCurrentBehavior(new Follower(context));
    }

    protected void changeCurrentBehavior(RaftActorBehavior newBehavior){
        reusableBehaviorStateHolder.init(getCurrentBehavior());
        setCurrentBehavior(newBehavior);
        handleBehaviorChange(reusableBehaviorStateHolder, getCurrentBehavior());
    }

    @Override
    public void handleCommand(Object message) {
        boolean handled = snapshotSupport.handleSnapshotMessage(message);
        if(handled) {
            return;
        }

        if (message instanceof ApplyState){
            ApplyState applyState = (ApplyState) message;

            long elapsedTime = (System.nanoTime() - applyState.getStartTime());
            if(elapsedTime >= APPLY_STATE_DELAY_THRESHOLD_IN_NANOS){
                LOG.warn("ApplyState took more time than expected. Elapsed Time = {} ms ApplyState = {}",
                        TimeUnit.NANOSECONDS.toMillis(elapsedTime), applyState);
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Applying state for log index {} data {}",
                    persistenceId(), applyState.getReplicatedLogEntry().getIndex(),
                    applyState.getReplicatedLogEntry().getData());
            }

            applyState(applyState.getClientActor(), applyState.getIdentifier(),
                applyState.getReplicatedLogEntry().getData());

        } else if (message instanceof ApplyJournalEntries){
            ApplyJournalEntries applyEntries = (ApplyJournalEntries) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Persisting ApplyLogEntries with index={}", persistenceId(), applyEntries.getToIndex());
            }

            persistence().persist(applyEntries, NoopProcedure.instance());

        } else if (message instanceof FindLeader) {
            getSender().tell(
                new FindLeaderReply(getLeaderAddress()),
                getSelf()
            );
        } else if(message instanceof GetOnDemandRaftState) {
            onGetOnDemandRaftStats();
        } else {
            reusableBehaviorStateHolder.init(getCurrentBehavior());

            setCurrentBehavior(currentBehavior.handleMessage(getSender(), message));

            handleBehaviorChange(reusableBehaviorStateHolder, getCurrentBehavior());
        }
    }

    protected RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        return new RaftActorSnapshotMessageSupport(delegatingPersistenceProvider, context,
                currentBehavior, getRaftActorSnapshotCohort());
    }

    private void onGetOnDemandRaftStats() {
        // Debugging message to retrieve raft stats.

        OnDemandRaftState.Builder builder = OnDemandRaftState.builder()
                .commitIndex(context.getCommitIndex())
                .currentTerm(context.getTermInformation().getCurrentTerm())
                .inMemoryJournalDataSize(replicatedLog().dataSize())
                .inMemoryJournalLogSize(replicatedLog().size())
                .isSnapshotCaptureInitiated(context.getSnapshotManager().isCapturing())
                .lastApplied(context.getLastApplied())
                .lastIndex(replicatedLog().lastIndex())
                .lastTerm(replicatedLog().lastTerm())
                .leader(getLeaderId())
                .raftState(currentBehavior.state().toString())
                .replicatedToAllIndex(currentBehavior.getReplicatedToAllIndex())
                .snapshotIndex(replicatedLog().getSnapshotIndex())
                .snapshotTerm(replicatedLog().getSnapshotTerm())
                .votedFor(context.getTermInformation().getVotedFor())
                .peerAddresses(ImmutableMap.copyOf(context.getPeerAddresses()));

        ReplicatedLogEntry lastLogEntry = getLastLogEntry();
        if (lastLogEntry != null) {
            builder.lastLogIndex(lastLogEntry.getIndex());
            builder.lastLogTerm(lastLogEntry.getTerm());
        }

        if(getCurrentBehavior() instanceof AbstractLeader) {
            AbstractLeader leader = (AbstractLeader)getCurrentBehavior();
            Collection<String> followerIds = leader.getFollowerIds();
            List<FollowerInfo> followerInfoList = Lists.newArrayListWithCapacity(followerIds.size());
            for(String id: followerIds) {
                final FollowerLogInformation info = leader.getFollower(id);
                followerInfoList.add(new FollowerInfo(id, info.getNextIndex(), info.getMatchIndex(),
                        info.isFollowerActive(), DurationFormatUtils.formatDurationHMS(info.timeSinceLastActivity())));
            }

            builder.followerInfoList(followerInfoList);
        }

        sender().tell(builder.build(), self());

    }

    private void handleBehaviorChange(BehaviorStateHolder oldBehaviorState, RaftActorBehavior currentBehavior) {
        RaftActorBehavior oldBehavior = oldBehaviorState.getBehavior();

        if (oldBehavior != currentBehavior){
            onStateChanged();
        }

        String oldBehaviorLeaderId = oldBehavior == null ? null : oldBehaviorState.getLeaderId();
        String oldBehaviorStateName = oldBehavior == null ? null : oldBehavior.state().name();

        // it can happen that the state has not changed but the leader has changed.
        Optional<ActorRef> roleChangeNotifier = getRoleChangeNotifier();
        if(!Objects.equal(oldBehaviorLeaderId, currentBehavior.getLeaderId())) {
            if(roleChangeNotifier.isPresent()) {
                roleChangeNotifier.get().tell(new LeaderStateChanged(getId(), currentBehavior.getLeaderId()), getSelf());
            }

            onLeaderChanged(oldBehaviorLeaderId, currentBehavior.getLeaderId());
        }

        if (roleChangeNotifier.isPresent() &&
                (oldBehavior == null || (oldBehavior.state() != currentBehavior.state()))) {
            roleChangeNotifier.get().tell(new RoleChanged(getId(), oldBehaviorStateName ,
                    currentBehavior.state().name()), getSelf());
        }
    }

    /**
     * When a derived RaftActor needs to persist something it must call
     * persistData.
     *
     * @param clientActor
     * @param identifier
     * @param data
     */
    protected void persistData(final ActorRef clientActor, final String identifier,
        final Payload data) {

        ReplicatedLogEntry replicatedLogEntry = new ReplicatedLogImplEntry(
            context.getReplicatedLog().lastIndex() + 1,
            context.getTermInformation().getCurrentTerm(), data);

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Persist data {}", persistenceId(), replicatedLogEntry);
        }

        final RaftActorContext raftContext = getRaftActorContext();

        replicatedLog().appendAndPersist(replicatedLogEntry, new Procedure<ReplicatedLogEntry>() {
            @Override
            public void apply(ReplicatedLogEntry replicatedLogEntry) throws Exception {
                if(!hasFollowers()){
                    // Increment the Commit Index and the Last Applied values
                    raftContext.setCommitIndex(replicatedLogEntry.getIndex());
                    raftContext.setLastApplied(replicatedLogEntry.getIndex());

                    // Apply the state immediately
                    applyState(clientActor, identifier, data);

                    // Send a ApplyJournalEntries message so that we write the fact that we applied
                    // the state to durable storage
                    self().tell(new ApplyJournalEntries(replicatedLogEntry.getIndex()), self());

                    context.getSnapshotManager().trimLog(context.getLastApplied(), currentBehavior);

                } else if (clientActor != null) {
                    // Send message for replication
                    currentBehavior.handleMessage(getSelf(),
                            new Replicate(clientActor, identifier, replicatedLogEntry));
                }
            }
        });
    }

    private ReplicatedLog replicatedLog() {
        return context.getReplicatedLog();
    }

    protected String getId() {
        return context.getId();
    }

    @VisibleForTesting
    void setCurrentBehavior(RaftActorBehavior behavior) {
        currentBehavior.setDelegate(behavior);
    }

    protected RaftActorBehavior getCurrentBehavior() {
        return currentBehavior.getDelegate();
    }

    /**
     * Derived actors can call the isLeader method to check if the current
     * RaftActor is the Leader or not
     *
     * @return true it this RaftActor is a Leader false otherwise
     */
    protected boolean isLeader() {
        return context.getId().equals(currentBehavior.getLeaderId());
    }

    /**
     * Derived actor can call getLeader if they need a reference to the Leader.
     * This would be useful for example in forwarding a request to an actor
     * which is the leader
     *
     * @return A reference to the leader if known, null otherwise
     */
    protected ActorSelection getLeader(){
        String leaderAddress = getLeaderAddress();

        if(leaderAddress == null){
            return null;
        }

        return context.actorSelection(leaderAddress);
    }

    /**
     *
     * @return the current leader's id
     */
    protected String getLeaderId(){
        return currentBehavior.getLeaderId();
    }

    protected RaftState getRaftState() {
        return currentBehavior.state();
    }

    protected ReplicatedLogEntry getLastLogEntry() {
        return replicatedLog().last();
    }

    protected Long getCurrentTerm(){
        return context.getTermInformation().getCurrentTerm();
    }

    protected Long getCommitIndex(){
        return context.getCommitIndex();
    }

    protected Long getLastApplied(){
        return context.getLastApplied();
    }

    protected RaftActorContext getRaftActorContext() {
        return context;
    }

    protected void updateConfigParams(ConfigParams configParams) {
        context.setConfigParams(configParams);
    }

    public final DataPersistenceProvider persistence() {
        return delegatingPersistenceProvider.getDelegate();
    }

    public void setPersistence(DataPersistenceProvider provider) {
        delegatingPersistenceProvider.setDelegate(provider);
    }

    protected void setPersistence(boolean persistent) {
        if(persistent) {
            setPersistence(new PersistentDataProvider(this));
        } else {
            setPersistence(new NonPersistentDataProvider() {
                /**
                 * The way snapshotting works is,
                 * <ol>
                 * <li> RaftActor calls createSnapshot on the Shard
                 * <li> Shard sends a CaptureSnapshotReply and RaftActor then calls saveSnapshot
                 * <li> When saveSnapshot is invoked on the akka-persistence API it uses the SnapshotStore to save
                 * the snapshot. The SnapshotStore sends SaveSnapshotSuccess or SaveSnapshotFailure. When the
                 * RaftActor gets SaveSnapshot success it commits the snapshot to the in-memory journal. This
                 * commitSnapshot is mimicking what is done in SaveSnapshotSuccess.
                 * </ol>
                 */
                @Override
                public void saveSnapshot(Object o) {
                    // Make saving Snapshot successful
                    // Committing the snapshot here would end up calling commit in the creating state which would
                    // be a state violation. That's why now we send a message to commit the snapshot.
                    self().tell(RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT, self());
                }
            });
        }
    }

    /**
     * setPeerAddress sets the address of a known peer at a later time.
     * <p>
     * This is to account for situations where a we know that a peer
     * exists but we do not know an address up-front. This may also be used in
     * situations where a known peer starts off in a different location and we
     * need to change it's address
     * <p>
     * Note that if the peerId does not match the list of peers passed to
     * this actor during construction an IllegalStateException will be thrown.
     *
     * @param peerId
     * @param peerAddress
     */
    protected void setPeerAddress(String peerId, String peerAddress){
        context.setPeerAddress(peerId, peerAddress);
    }

    /**
     * The applyState method will be called by the RaftActor when some data
     * needs to be applied to the actor's state
     *
     * @param clientActor A reference to the client who sent this message. This
     *                    is the same reference that was passed to persistData
     *                    by the derived actor. clientActor may be null when
     *                    the RaftActor is behaving as a follower or during
     *                    recovery.
     * @param identifier  The identifier of the persisted data. This is also
     *                    the same identifier that was passed to persistData by
     *                    the derived actor. identifier may be null when
     *                    the RaftActor is behaving as a follower or during
     *                    recovery
     * @param data        A piece of data that was persisted by the persistData call.
     *                    This should NEVER be null.
     */
    protected abstract void applyState(ActorRef clientActor, String identifier,
        Object data);

    /**
     * Returns the RaftActorRecoveryCohort to participate in persistence recovery.
     */
    @Nonnull
    protected abstract RaftActorRecoveryCohort getRaftActorRecoveryCohort();

    /**
     * This method is called when recovery is complete.
     */
    protected abstract void onRecoveryComplete();

    /**
     * Returns the RaftActorSnapshotCohort to participate in persistence recovery.
     */
    @Nonnull
    protected abstract RaftActorSnapshotCohort getRaftActorSnapshotCohort();

    /**
     * This method will be called by the RaftActor when the state of the
     * RaftActor changes. The derived actor can then use methods like
     * isLeader or getLeader to do something useful
     */
    protected abstract void onStateChanged();

    /**
     * Notifier Actor for this RaftActor to notify when a role change happens
     * @return ActorRef - ActorRef of the notifier or Optional.absent if none.
     */
    protected abstract Optional<ActorRef> getRoleChangeNotifier();

    protected void onLeaderChanged(String oldLeader, String newLeader){};

    private String getLeaderAddress(){
        if(isLeader()){
            return getSelf().path().toString();
        }
        String leaderId = currentBehavior.getLeaderId();
        if (leaderId == null) {
            return null;
        }
        String peerAddress = context.getPeerAddress(leaderId);
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: getLeaderAddress leaderId = {} peerAddress = {}",
                    persistenceId(), leaderId, peerAddress);
        }

        return peerAddress;
    }

    protected boolean hasFollowers(){
        return getRaftActorContext().hasFollowers();
    }

    /**
     * @deprecated Deprecated in favor of {@link org.opendaylight.controller.cluster.raft.base.messages.DeleteEntriesTest}
     *             whose type for fromIndex is long instead of int. This class was kept for backwards
     *             compatibility with Helium.
     */
    @Deprecated
    static class DeleteEntries implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int fromIndex;

        public DeleteEntries(int fromIndex) {
            this.fromIndex = fromIndex;
        }

        public int getFromIndex() {
            return fromIndex;
        }
    }

    static class UpdateElectionTerm implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long currentTerm;
        private final String votedFor;

        public UpdateElectionTerm(long currentTerm, String votedFor) {
            this.currentTerm = currentTerm;
            this.votedFor = votedFor;
        }

        public long getCurrentTerm() {
            return currentTerm;
        }

        public String getVotedFor() {
            return votedFor;
        }
    }

    private static class BehaviorStateHolder {
        private RaftActorBehavior behavior;
        private String leaderId;

        void init(RaftActorBehavior behavior) {
            this.behavior = behavior;
            this.leaderId = behavior != null ? behavior.getLeaderId() : null;
        }

        RaftActorBehavior getBehavior() {
            return behavior;
        }

        String getLeaderId() {
            return leaderId;
        }
    }
}
