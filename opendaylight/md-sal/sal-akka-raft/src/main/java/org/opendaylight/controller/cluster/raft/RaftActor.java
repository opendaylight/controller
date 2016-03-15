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
import akka.actor.PoisonPill;
import akka.japi.Procedure;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
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
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractRaftActorBehavior;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
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
     * This context should NOT be passed directly to any other actor it is
     * only to be consumed by the RaftActorBehaviors
     */
    private final RaftActorContextImpl context;

    private final DelegatingPersistentDataProvider delegatingPersistenceProvider;

    private final PersistentDataProvider persistentProvider;

    private RaftActorRecoverySupport raftRecovery;

    private RaftActorSnapshotMessageSupport snapshotSupport;

    private final BehaviorStateHolder reusableBehaviorStateHolder = new BehaviorStateHolder();

    private final SwitchBehaviorSupplier reusableSwitchBehaviorSupplier = new SwitchBehaviorSupplier();

    private RaftActorServerConfigurationSupport serverConfigurationSupport;

    private RaftActorLeadershipTransferCohort leadershipTransferInProgress;

    private boolean shuttingDown;

    public RaftActor(String id, Map<String, String> peerAddresses,
         Optional<ConfigParams> configParams, short payloadVersion) {

        persistentProvider = new PersistentDataProvider(this);
        delegatingPersistenceProvider = new RaftActorDelegatingPersistentDataProvider(null, persistentProvider);

        context = new RaftActorContextImpl(this.getSelf(),
            this.getContext(), id, new ElectionTermImpl(persistentProvider, id, LOG),
            -1, -1, peerAddresses,
            (configParams.isPresent() ? configParams.get(): new DefaultConfigParamsImpl()),
            delegatingPersistenceProvider, LOG);

        context.setPayloadVersion(payloadVersion);
        context.setReplicatedLog(ReplicatedLogImpl.newInstance(context));
    }

    @Override
    public void preStart() throws Exception {
        LOG.info("Starting recovery for {} with journal batch size {}", persistenceId(),
                context.getConfigParams().getJournalRecoveryLogBatchSize());

        super.preStart();

        snapshotSupport = newRaftActorSnapshotMessageSupport();
        serverConfigurationSupport = new RaftActorServerConfigurationSupport(this);
    }

    @Override
    public void postStop() {
        context.close();
        super.postStop();
    }

    @Override
    protected void handleRecover(Object message) {
        if(raftRecovery == null) {
            raftRecovery = newRaftActorRecoverySupport();
        }

        boolean recoveryComplete = raftRecovery.handleRecoveryMessage(message, persistentProvider);
        if(recoveryComplete) {
            onRecoveryComplete();

            initializeBehavior();

            raftRecovery = null;

            if (context.getReplicatedLog().size() > 0) {
                self().tell(new InitiateCaptureSnapshot(), self());
                LOG.info("{}: Snapshot capture initiated after recovery", persistenceId());
            } else {
                LOG.info("{}: Snapshot capture NOT initiated after recovery, journal empty", persistenceId());
            }
        }
    }

    protected RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return new RaftActorRecoverySupport(context, getRaftActorRecoveryCohort());
    }

    @VisibleForTesting
    void initializeBehavior(){
        changeCurrentBehavior(new Follower(context));
    }

    @VisibleForTesting
    protected void changeCurrentBehavior(RaftActorBehavior newBehavior){
        if(getCurrentBehavior() != null) {
            try {
                getCurrentBehavior().close();
            } catch(Exception e) {
                LOG.warn("{}: Error closing behavior {}", persistence(), getCurrentBehavior(), e);
            }
        }

        reusableBehaviorStateHolder.init(getCurrentBehavior());
        setCurrentBehavior(newBehavior);
        handleBehaviorChange(reusableBehaviorStateHolder, getCurrentBehavior());
    }

    @Override
    protected void handleCommand(final Object message) {
        if (serverConfigurationSupport.handleMessage(message, getSender())) {
            return;
        }
        if (snapshotSupport.handleSnapshotMessage(message, getSender())) {
            return;
        }

        if (message instanceof ApplyState) {
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

            if (!hasFollowers()) {
                // for single node, the capture should happen after the apply state
                // as we delete messages from the persistent journal which have made it to the snapshot
                // capturing the snapshot before applying makes the persistent journal and snapshot out of sync
                // and recovery shows data missing
                context.getReplicatedLog().captureSnapshotIfReady(applyState.getReplicatedLogEntry());

                context.getSnapshotManager().trimLog(context.getLastApplied());
            }

        } else if (message instanceof ApplyJournalEntries) {
            ApplyJournalEntries applyEntries = (ApplyJournalEntries) message;
            if(LOG.isDebugEnabled()) {
                LOG.debug("{}: Persisting ApplyJournalEntries with index={}", persistenceId(), applyEntries.getToIndex());
            }

            persistence().persist(applyEntries, NoopProcedure.instance());

        } else if (message instanceof FindLeader) {
            getSender().tell(
                new FindLeaderReply(getLeaderAddress()),
                getSelf()
            );
        } else if(message instanceof GetOnDemandRaftState) {
            onGetOnDemandRaftStats();
        } else if(message instanceof InitiateCaptureSnapshot) {
            captureSnapshot();
        } else if(message instanceof SwitchBehavior) {
            switchBehavior(((SwitchBehavior) message));
        } else if(message instanceof LeaderTransitioning) {
            onLeaderTransitioning();
        } else if(message instanceof Shutdown) {
            onShutDown();
        } else if(message instanceof Runnable) {
            ((Runnable)message).run();
        } else {
            switchBehavior(reusableSwitchBehaviorSupplier.handleMessage(getSender(), message));
        }
    }

    void initiateLeadershipTransfer(final RaftActorLeadershipTransferCohort.OnComplete onComplete) {
        LOG.debug("{}: Initiating leader transfer", persistenceId());

        if(leadershipTransferInProgress == null) {
            leadershipTransferInProgress = new RaftActorLeadershipTransferCohort(this, getSender());
            leadershipTransferInProgress.addOnComplete(new RaftActorLeadershipTransferCohort.OnComplete() {
                @Override
                public void onSuccess(ActorRef raftActorRef, ActorRef replyTo) {
                    leadershipTransferInProgress = null;
                }

                @Override
                public void onFailure(ActorRef raftActorRef, ActorRef replyTo) {
                    leadershipTransferInProgress = null;
                }
            });

            leadershipTransferInProgress.addOnComplete(onComplete);
            leadershipTransferInProgress.init();
        } else {
            LOG.debug("{}: prior leader transfer in progress - adding callback", persistenceId());
            leadershipTransferInProgress.addOnComplete(onComplete);
        }
    }

    private void onShutDown() {
        LOG.debug("{}: onShutDown", persistenceId());

        if(shuttingDown) {
            return;
        }

        shuttingDown = true;

        final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
        if(currentBehavior.state() == RaftState.Leader && context.hasFollowers()) {
            initiateLeadershipTransfer(new RaftActorLeadershipTransferCohort.OnComplete() {
                @Override
                public void onSuccess(ActorRef raftActorRef, ActorRef replyTo) {
                    LOG.debug("{}: leader transfer succeeded - sending PoisonPill", persistenceId());
                    raftActorRef.tell(PoisonPill.getInstance(), raftActorRef);
                }

                @Override
                public void onFailure(ActorRef raftActorRef, ActorRef replyTo) {
                    LOG.debug("{}: leader transfer failed - sending PoisonPill", persistenceId());
                    raftActorRef.tell(PoisonPill.getInstance(), raftActorRef);
                }
            });
        } else if(currentBehavior.state() == RaftState.Leader) {
            pauseLeader(new TimedRunnable(context.getConfigParams().getElectionTimeOutInterval(), this) {
                @Override
                protected void doRun() {
                    self().tell(PoisonPill.getInstance(), self());
                }

                @Override
                protected void doCancel() {
                    self().tell(PoisonPill.getInstance(), self());
                }
            });
        } else {
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    private void onLeaderTransitioning() {
        LOG.debug("{}: onLeaderTransitioning", persistenceId());
        Optional<ActorRef> roleChangeNotifier = getRoleChangeNotifier();
        if(getRaftState() == RaftState.Follower && roleChangeNotifier.isPresent()) {
            roleChangeNotifier.get().tell(newLeaderStateChanged(getId(), null,
                getCurrentBehavior().getLeaderPayloadVersion()), getSelf());
        }
    }

    private void switchBehavior(SwitchBehavior message) {
        if(!getRaftActorContext().getRaftPolicy().automaticElectionsEnabled()) {
            RaftState newState = message.getNewState();
            if( newState == RaftState.Leader || newState == RaftState.Follower) {
                switchBehavior(reusableSwitchBehaviorSupplier.handleMessage(getSender(), message));
                getRaftActorContext().getTermInformation().updateAndPersist(message.getNewTerm(), "");
            } else {
                LOG.warn("Switching to behavior : {} - not supported", newState);
            }
        }
    }

    private void switchBehavior(Supplier<RaftActorBehavior> supplier){
        reusableBehaviorStateHolder.init(getCurrentBehavior());

        setCurrentBehavior(supplier.get());

        handleBehaviorChange(reusableBehaviorStateHolder, getCurrentBehavior());
    }

    @VisibleForTesting
    RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        return new RaftActorSnapshotMessageSupport(context, getRaftActorSnapshotCohort());
    }

    private void onGetOnDemandRaftStats() {
        // Debugging message to retrieve raft stats.

        Map<String, String> peerAddresses = new HashMap<>();
        for(String peerId: context.getPeerIds()) {
            peerAddresses.put(peerId, context.getPeerAddress(peerId));
        }

        final RaftActorBehavior currentBehavior = context.getCurrentBehavior();
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
                .peerAddresses(peerAddresses)
                .customRaftPolicyClassName(context.getConfigParams().getCustomRaftPolicyImplementationClass());

        ReplicatedLogEntry lastLogEntry = replicatedLog().last();
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

        String lastValidLeaderId = oldBehavior == null ? null : oldBehaviorState.getLastValidLeaderId();
        String oldBehaviorStateName = oldBehavior == null ? null : oldBehavior.state().name();

        // it can happen that the state has not changed but the leader has changed.
        Optional<ActorRef> roleChangeNotifier = getRoleChangeNotifier();
        if(!Objects.equal(lastValidLeaderId, currentBehavior.getLeaderId()) ||
           oldBehaviorState.getLeaderPayloadVersion() != currentBehavior.getLeaderPayloadVersion()) {
            if(roleChangeNotifier.isPresent()) {
                roleChangeNotifier.get().tell(newLeaderStateChanged(getId(), currentBehavior.getLeaderId(),
                        currentBehavior.getLeaderPayloadVersion()), getSelf());
            }

            onLeaderChanged(lastValidLeaderId, currentBehavior.getLeaderId());

            if(leadershipTransferInProgress != null) {
                leadershipTransferInProgress.onNewLeader(currentBehavior.getLeaderId());
            }

            serverConfigurationSupport.onNewLeader(currentBehavior.getLeaderId());
        }

        if (roleChangeNotifier.isPresent() &&
                (oldBehavior == null || (oldBehavior.state() != currentBehavior.state()))) {
            roleChangeNotifier.get().tell(new RoleChanged(getId(), oldBehaviorStateName ,
                    currentBehavior.state().name()), getSelf());
        }
    }

    protected LeaderStateChanged newLeaderStateChanged(String memberId, String leaderId, short leaderPayloadVersion) {
        return new LeaderStateChanged(memberId, leaderId, leaderPayloadVersion);
    }

    @Override
    public long snapshotSequenceNr() {
        // When we do a snapshot capture, we also capture and save the sequence-number of the persistent journal,
        // so that we can delete the persistent journal based on the saved sequence-number
        // However , when akka replays the journal during recovery, it replays it from the sequence number when the snapshot
        // was saved and not the number we saved.
        // We would want to override it , by asking akka to use the last-sequence number known to us.
        return context.getSnapshotManager().getLastSequenceNumber();
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
            public void apply(ReplicatedLogEntry replicatedLogEntry) {
                if (!hasFollowers()){
                    // Increment the Commit Index and the Last Applied values
                    raftContext.setCommitIndex(replicatedLogEntry.getIndex());
                    raftContext.setLastApplied(replicatedLogEntry.getIndex());

                    // Apply the state immediately.
                    self().tell(new ApplyState(clientActor, identifier, replicatedLogEntry), self());

                    // Send a ApplyJournalEntries message so that we write the fact that we applied
                    // the state to durable storage
                    self().tell(new ApplyJournalEntries(replicatedLogEntry.getIndex()), self());

                } else if (clientActor != null) {
                    context.getReplicatedLog().captureSnapshotIfReady(replicatedLogEntry);

                    // Send message for replication
                    getCurrentBehavior().handleMessage(getSelf(),
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
        context.setCurrentBehavior(behavior);
    }

    protected RaftActorBehavior getCurrentBehavior() {
        return context.getCurrentBehavior();
    }

    /**
     * Derived actors can call the isLeader method to check if the current
     * RaftActor is the Leader or not
     *
     * @return true it this RaftActor is a Leader false otherwise
     */
    protected boolean isLeader() {
        return context.getId().equals(getCurrentBehavior().getLeaderId());
    }

    protected final boolean isLeaderActive() {
        return getRaftState() != RaftState.IsolatedLeader && !shuttingDown &&
                !isLeadershipTransferInProgress();
    }

    private boolean isLeadershipTransferInProgress() {
        return leadershipTransferInProgress != null && leadershipTransferInProgress.isTransferring();
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
    protected final String getLeaderId(){
        return getCurrentBehavior().getLeaderId();
    }

    @VisibleForTesting
    protected final RaftState getRaftState() {
        return getCurrentBehavior().state();
    }

    protected Long getCurrentTerm(){
        return context.getTermInformation().getCurrentTerm();
    }

    protected RaftActorContext getRaftActorContext() {
        return context;
    }

    protected void updateConfigParams(ConfigParams configParams) {

        // obtain the RaftPolicy for oldConfigParams and the updated one.
        String oldRaftPolicy = context.getConfigParams().
            getCustomRaftPolicyImplementationClass();
        String newRaftPolicy = configParams.
            getCustomRaftPolicyImplementationClass();

        LOG.debug("{}: RaftPolicy used with prev.config {}, RaftPolicy used with newConfig {}", persistenceId(),
            oldRaftPolicy, newRaftPolicy);
        context.setConfigParams(configParams);
        if (!Objects.equal(oldRaftPolicy, newRaftPolicy)) {
            // The RaftPolicy was modified. If the current behavior is Follower then re-initialize to Follower
            // but transfer the previous leaderId so it doesn't immediately try to schedule an election. This
            // avoids potential disruption. Otherwise, switch to Follower normally.
            RaftActorBehavior behavior = getCurrentBehavior();
            if(behavior instanceof Follower) {
                String previousLeaderId = ((Follower)behavior).getLeaderId();
                short previousLeaderPayloadVersion = behavior.getLeaderPayloadVersion();

                LOG.debug("{}: Re-initializing to Follower with previous leaderId {}", persistenceId(), previousLeaderId);

                changeCurrentBehavior(new Follower(context, previousLeaderId, previousLeaderPayloadVersion));
            } else {
                initializeBehavior();
            }
        }
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

    /**
     * This method is called prior to operations such as leadership transfer and actor shutdown when the leader
     * must pause or stop its duties. This method allows derived classes to gracefully pause or finish current
     * work prior to performing the operation. On completion of any work, the run method must be called on the
     * given Runnable to proceed with the given operation. <b>Important:</b> the run method must be called on
     * this actor's thread dispatcher as as it modifies internal state.
     * <p>
     * The default implementation immediately runs the operation.
     *
     * @param operation the operation to run
     */
    protected void pauseLeader(Runnable operation) {
        operation.run();
    }

    protected void onLeaderChanged(String oldLeader, String newLeader) {

    };

    private String getLeaderAddress(){
        if(isLeader()){
            return getSelf().path().toString();
        }
        String leaderId = getLeaderId();
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

    private void captureSnapshot() {
        SnapshotManager snapshotManager = context.getSnapshotManager();

        if (!snapshotManager.isCapturing()) {
            final long idx = getCurrentBehavior().getReplicatedToAllIndex();
            LOG.debug("Take a snapshot of current state. lastReplicatedLog is {} and replicatedToAllIndex is {}",
                replicatedLog().last(), idx);

            snapshotManager.capture(replicatedLog().last(), idx);
        }
    }

    /**
     * @deprecated Deprecated in favor of {@link org.opendaylight.controller.cluster.raft.base.messages.DeleteEntries}
     *             whose type for fromIndex is long instead of int. This class was kept for backwards
     *             compatibility with Helium.
     */
    // Suppressing this warning as we can't set serialVersionUID to maintain backwards compatibility.
    @SuppressWarnings("serial")
    @Deprecated
    static class DeleteEntries implements Serializable {
        private final int fromIndex;

        public DeleteEntries(int fromIndex) {
            this.fromIndex = fromIndex;
        }

        public int getFromIndex() {
            return fromIndex;
        }
    }

    /**
     * @deprecated Deprecated in favor of non-inner class {@link org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm}
     *             which has serialVersionUID set. This class was kept for backwards compatibility with Helium.
     */
    // Suppressing this warning as we can't set serialVersionUID to maintain backwards compatibility.
    @SuppressWarnings("serial")
    @Deprecated
    static class UpdateElectionTerm implements Serializable {
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
        private String lastValidLeaderId;
        private short leaderPayloadVersion;

        void init(RaftActorBehavior behavior) {
            this.behavior = behavior;
            this.leaderPayloadVersion = behavior != null ? behavior.getLeaderPayloadVersion() : -1;

            String behaviorLeaderId = behavior != null ? behavior.getLeaderId() : null;
            if(behaviorLeaderId != null) {
                this.lastValidLeaderId = behaviorLeaderId;
            }
        }

        RaftActorBehavior getBehavior() {
            return behavior;
        }

        String getLastValidLeaderId() {
            return lastValidLeaderId;
        }

        short getLeaderPayloadVersion() {
            return leaderPayloadVersion;
        }
    }

    private class SwitchBehaviorSupplier implements Supplier<RaftActorBehavior> {
        private Object message;
        private ActorRef sender;

        public SwitchBehaviorSupplier handleMessage(ActorRef sender, Object message){
            this.sender = sender;
            this.message = message;
            return this;
        }

        @Override
        public RaftActorBehavior get() {
            if(this.message instanceof SwitchBehavior){
                return AbstractRaftActorBehavior.createBehavior(context, ((SwitchBehavior) message).getNewState());
            }
            return getCurrentBehavior().handleMessage(sender, message);
        }
    }
}
