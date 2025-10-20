/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;
import org.opendaylight.controller.cluster.notifications.DefaultLeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.base.messages.Replicate;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeFollower;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior.BecomeLeader;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadership;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.AbstractRaftCommand;
import org.opendaylight.controller.cluster.raft.spi.AbstractStateCommand;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.NoopRecoveryObserver;
import org.opendaylight.controller.cluster.raft.spi.RaftCommand;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.controller.cluster.raft.spi.RecoveryObserver;
import org.opendaylight.controller.cluster.raft.spi.StateCommand;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RaftActor encapsulates a state machine that needs to be kept synchronized in a cluster. It implements the RAFT
 * algorithm as described in the paper
 * <a href='https://ramcloud.stanford.edu/wiki/download/attachments/11370504/raft.pdf'>
 * In Search of an Understandable Consensus Algorithm</a>
 *
 * <p>RaftActor has 3 states and each state has a certain behavior associated with it. A Raft actor can behave as,
 * <ul>
 * <li> A Leader </li>
 * <li> A Follower (or) </li>
 * <li> A Candidate </li>
 * </ul>
 *
 * <p>A RaftActor MUST be a Leader in order to accept requests from clients to change the state of it's encapsulated
 * state machine. Once a RaftActor becomes a Leader it is also responsible for ensuring that all followers ultimately
 * have the same log and therefore the same state machine as itself.
 *
 * <p>The current behavior of a RaftActor determines how election for leadership is initiated and how peer RaftActors
 * react to request for votes.
 *
 * <p>Each RaftActor also needs to know the current election term. It uses this information for a couple of things.
 * One is to simply figure out who it voted for in the last election. Another is to figure out if the message it
 * received to update it's state is stale.
 *
 * <p>The RaftActor uses akka-persistence to store it's replicated log. Furthermore through it's behaviors a Raft Actor
 * determines
 * <ul>
 * <li> when a log entry should be persisted </li>
 * <li> when a log entry should be applied to the state machine (and) </li>
 * <li> when a snapshot should be saved </li>
 * </ul>
 */
public abstract class RaftActor extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActor.class);
    private static final long APPLY_STATE_DELAY_THRESHOLD_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    private final @NonNull BehaviorStateTracker behaviorStateTracker = new BehaviorStateTracker();
    private final @NonNull PersistenceControl persistenceControl;
    private final @NonNull RestrictedObjectStreams objectStreams;
    private final @NonNull RaftStorageCompleter completer;
    // This context should NOT be passed directly to any other actor it is  only to be consumed
    // by the RaftActorBehaviors.
    private final @NonNull LocalAccess localAccess;
    private final @NonNull PeerInfos peerInfos;
    private final @NonNull String memberId;

    // FIXME: should be valid only after recovery
    private final @NonNull RaftActorContextImpl context;

    private RaftActorSnapshotMessageSupport snapshotSupport;
    private RaftActorVotingConfigSupport votingConfigSupport;
    private boolean shuttingDown;

    @NonNullByDefault
    protected RaftActor(final Path stateDir, final String memberId, final Map<String, String> peerAddresses,
            final Optional<ConfigParams> configParams, final short payloadVersion,
            final RestrictedObjectStreams objectStreams) {
        this.memberId = requireNonNull(memberId);
        this.objectStreams = requireNonNull(objectStreams);

        completer = new RaftStorageCompleter(memberId, this);
        peerInfos = new PeerInfos(memberId, peerAddresses);

        final var config = configParams.orElseGet(DefaultConfigParamsImpl::new);
        localAccess = new LocalAccess(memberId, stateDir.resolve(memberId));
        final var streamConfig = new FileBackedOutputStream.Configuration(config.getFileBackedStreamingThreshold(),
            config.getTempFileDirectory());
        persistenceControl = new PersistenceControl(this, completer, localAccess.stateDir(),
            config.getPreferredCompression(), streamConfig);

        context = new RaftActorContextImpl(self(), getContext(), localAccess, peerInfos, config, payloadVersion,
            objectStreams, persistenceControl, this::applyCommand);
    }

    /**
     * Return the unique member name of this actor.
     *
     * @return The member name
     */
    public final @NonNull String memberId() {
        return memberId;
    }

    final @NonNull LocalAccess localAccess() {
        return localAccess;
    }

    final @NonNull PeerInfos peerInfos() {
        return peerInfos;
    }

    /**
     * {@return the {@link RestrictedObjectStreams} instance to use for {@link StateMachineCommand} serialization}
     */
    final @NonNull RestrictedObjectStreams objectStreams() {
        return objectStreams;
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    public void preStart() throws Exception {
        LOG.info("{}: Starting recovery with journal batch size {}", memberId(),
            context.getConfigParams().getJournalRecoveryLogBatchSize());

        super.preStart();

        persistenceControl.start();
        context.getSnapshotManager().setSnapshotCohort(getRaftActorSnapshotCohort());
        snapshotSupport = newRaftActorSnapshotMessageSupport();
        votingConfigSupport = new RaftActorVotingConfigSupport(this);

        runRecovery();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void postStop() throws Exception {
        final var behavior = getCurrentBehavior();
        if (behavior != null) {
            try {
                behavior.close();
            } catch (Exception e) {
                LOG.warn("{}: Error closing behavior {}", memberId(), behavior.raftRole(), e);
            }
        }
        persistenceControl.stop();

        super.postStop();
    }

    private void runRecovery() throws IOException {
        final Recovery<?> recovery;
        final var journal = persistenceControl.journal();
        if (journal != null) {
            LOG.debug("{}: starting journal recovery", memberId());
            recovery = new JournalRecovery<>(this, getRaftActorSnapshotCohort(), getRaftActorRecoveryCohort(),
                context.getConfigParams(), journal);
        } else {
            LOG.debug("{}: recovering to non-persistent", memberId());
            recovery = new TransientRecovery<>(this, getRaftActorSnapshotCohort(), getRaftActorRecoveryCohort(),
                context.getConfigParams());
        }

        recoveryCompeleted(recovery.recover());
    }

    @NonNullByDefault
    private void recoveryCompeleted(final RecoveryResult journalResult) throws IOException {
        LOG.debug("{}: Recovery completed and {} restore from snapshot", memberId(),
            journalResult.canRestoreFromSnapshot() ? "can" : "cannot");

        final var recoveryCohort = getRaftActorRecoveryCohort();
        final var restoreFrom = getRestoreFromSnapshot();

        final RecoveryLog recoveredLog;
        if (restoreFrom != null) {
            if (journalResult.canRestoreFromSnapshot()) {
                LOG.debug("{}: Restoring snapshot: {}", memberId(), restoreFrom);
                final var timestamp = Instant.now();

                localAccess.termInfoStore().storeAndSetTerm(restoreFrom.termInfo());

                final var votingConfig = restoreFrom.votingConfig();
                if (votingConfig != null) {
                    peerInfos().updateVotingConfig(votingConfig);
                }

                recoveredLog = new RecoveryLog(memberId());
                recoveredLog.resetToSnapshot(restoreFrom);

                final var restoreState = restoreFrom.state();
                if (restoreState != null) {
                    recoveryCohort.applyRecoveredSnapshot(restoreState);
                }

                persistenceControl.snapshotStore().saveSnapshot(
                    new RaftSnapshot(peerInfos().votingConfig(true), restoreFrom.getUnAppliedEntries()),
                    restoreFrom.lastApplied(), toStorage(getRaftActorSnapshotCohort().support(), restoreState),
                    timestamp);
            } else {
                LOG.warn("{}: The provided restore snapshot was not applied because the persistence store is not empty",
                    memberId());
                recoveredLog = journalResult.recoveryLog();
            }
        } else {
            recoveredLog = journalResult.recoveryLog();
        }

        replicatedLog().resetToLog(overridePekkoRecoveredLog(recoveredLog));
        finishRecovery();
    }

    @NonNullByDefault
    static final <T extends Snapshot.State> @Nullable ToStorage<T> toStorage(final StateSnapshot.Support<T> support,
            final Snapshot.@Nullable State state) {
        return ToStorage.ofNullable(support.writer(), support.snapshotType().cast(state));
    }

    @NonNullByDefault
    @VisibleForTesting
    protected ReplicatedLog overridePekkoRecoveredLog(final ReplicatedLog recoveredLog) {
        return recoveredLog;
    }

    @NonNullByDefault
    private void finishRecovery() {
        recoveryObserver().onRecoveryCompleted();
        onRecoveryComplete();
        initializeBehavior();
    }

    @VisibleForTesting
    void initializeBehavior() {
        changeCurrentBehavior(new Follower(context));
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:IllegalCatch")
    final void changeCurrentBehavior(final RaftActorBehavior newBehavior) {
        final var currentBehavior = getCurrentBehavior();
        if (currentBehavior != null) {
            try {
                currentBehavior.close();
            } catch (Exception e) {
                LOG.warn("{}: Error closing behavior {}", memberId(), currentBehavior, e);
            }
        }

        final var state = behaviorStateTracker.capture(currentBehavior);
        setCurrentBehavior(newBehavior);
        handleBehaviorChange(state, newBehavior);
    }

    /**
     * Method exposed for subclasses to plug-in their logic. This method is invoked by {@link #handleReceive(Object)}
     * for messages which are not handled by this class. Subclasses overriding this class should fall back to this
     * implementation for messages which they do not handle
     *
     * @param message Incoming command message
     */
    protected void handleNonRaftCommand(final Object message) {
        unhandled(message);
    }

    @Override
    protected final void handleReceive(final Object message) {
        // dispatch any pending completions first ...
        completer.completeUntilEmpty();
        // ... then handle the message ...
        handleCommandImpl(requireNonNull(message));
        // ... and finally wait for deferred tasks
        try {
            completer.completeUntilSynchronized();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while synchronizing completions", e);
        }
    }

    /**
     * Handles a message.
     *
     * @deprecated This method is not final for testing purposes. DO NOT OVERRIDE IT, override
     *             {@link #handleNonRaftCommand(Object)} instead.
     */
    @Deprecated
    @VisibleForTesting
    // FIXME: make this method final once our unit tests do not need to override it
    protected void handleCommandImpl(final @NonNull Object message) {
        if (votingConfigSupport.handleMessage(message, getSender())) {
            return;
        }
        if (snapshotSupport.handleSnapshotMessage(message)) {
            return;
        }
        switch (message) {
            case ApplyState msg -> {
                if (!hasFollowers()) {
                    // for single node, the capture should happen after the apply state
                    // as we delete messages from the persistent journal which have made it to the snapshot
                    // capturing the snapshot before applying makes the persistent journal and snapshot out of sync
                    // and recovery shows data missing
                    replicatedLog().captureSnapshotIfReady(msg.entry());

                    context.getSnapshotManager().trimLog(replicatedLog().getLastApplied());
                }

                possiblyHandleBehaviorMessage(msg);
            }
            case FindLeader msg -> getSender().tell(new FindLeaderReply(getLeaderAddress()), self());
            case GetOnDemandRaftState msg -> getSender().tell(getOnDemandRaftState(), self());
            case GetSnapshot msg ->
                getSender().tell(new GetSnapshotReply(memberId(), getSnapshot()), ActorRef.noSender());
            case InitiateCaptureSnapshot msg -> captureSnapshot();
            case BecomeFollower(var newTerm) -> switchBehavior(Follower::new, newTerm);
            case BecomeLeader(var newTerm) -> switchBehavior(Leader::new, newTerm);
            case LeaderTransitioning leaderTransitioning -> onLeaderTransitioning(leaderTransitioning);
            case Shutdown msg -> onShutDown();
            // FIXME: remove this as it should be either an explicit command or executeInSelf()
            case Runnable runnable -> runnable.run();
            case NoopPayload noopPayload -> submitCommand(null, noopPayload);
            case RequestLeadership msg -> onRequestLeadership(msg);
            default -> {
                if (!possiblyHandleBehaviorMessage(message)) {
                    handleNonRaftCommand(message);
                }
            }
        }
    }

    private void onRequestLeadership(final RequestLeadership message) {
        LOG.debug("{}: onRequestLeadership {}", memberId(), message);
        final var requestedFollowerId = message.getRequestedFollowerId();

        if (!isLeader()) {
            // non-leader cannot satisfy leadership request
            LOG.warn("{}: onRequestLeadership {} was sent to non-leader."
                    + " Current behavior: {}. Sending failure response",
                    memberId(), message, getCurrentBehavior().raftRole());
            message.getReplyTo().tell(new LeadershipTransferFailedException("Cannot transfer leader to "
                + requestedFollowerId + ". RequestLeadership message was sent to non-leader " + memberId()), self());
            return;
        }

        final ActorRef replyTo = message.getReplyTo();
        initiateLeadershipTransfer(new RaftActorLeadershipTransferCohort.OnComplete() {
            @Override
            public void onSuccess(final ActorRef raftActorRef) {
                // sanity check
                if (!requestedFollowerId.equals(getLeaderId())) {
                    onFailure(raftActorRef);
                }

                LOG.debug("{}: Leadership transferred successfully to {}", memberId(), requestedFollowerId);
                replyTo.tell(new Status.Success(null), self());
            }

            @Override
            public void onFailure(final ActorRef raftActorRef) {
                LOG.debug("{}: LeadershipTransfer request from {} failed", memberId(), requestedFollowerId);
                replyTo.tell(new Status.Failure(
                        new LeadershipTransferFailedException(
                                "Failed to transfer leadership to " + requestedFollowerId
                                        + ". Follower is not ready to become leader")),
                        self());
            }
        }, requestedFollowerId, RaftActorLeadershipTransferCohort.USE_DEFAULT_LEADER_TIMEOUT);
    }

    private boolean possiblyHandleBehaviorMessage(final Object message) {
        final RaftActorBehavior currentBehavior = getCurrentBehavior();
        final BehaviorState state = behaviorStateTracker.capture(currentBehavior);

        // A behavior indicates that it processed the change by returning a reference to the next behavior
        // to be used. A null return indicates it has not processed the message and we should be passing it to
        // the subclass for handling.
        final RaftActorBehavior nextBehavior = currentBehavior.handleMessage(getSender(), message);
        if (nextBehavior != null) {
            switchBehavior(state, nextBehavior);
            return true;
        }

        return false;
    }

    private void initiateLeadershipTransfer(final RaftActorLeadershipTransferCohort.OnComplete onComplete,
            final @Nullable String followerId, final long newLeaderTimeoutInMillis) {
        LOG.debug("{}: Initiating leader transfer", memberId());

        RaftActorLeadershipTransferCohort leadershipTransferInProgress = context.getRaftActorLeadershipTransferCohort();
        if (leadershipTransferInProgress == null) {
            leadershipTransferInProgress = new RaftActorLeadershipTransferCohort(this, followerId);
            leadershipTransferInProgress.setNewLeaderTimeoutInMillis(newLeaderTimeoutInMillis);
            leadershipTransferInProgress.addOnComplete(new RaftActorLeadershipTransferCohort.OnComplete() {
                @Override
                public void onSuccess(final ActorRef raftActorRef) {
                    context.setRaftActorLeadershipTransferCohort(null);
                }

                @Override
                public void onFailure(final ActorRef raftActorRef) {
                    context.setRaftActorLeadershipTransferCohort(null);
                }
            });

            leadershipTransferInProgress.addOnComplete(onComplete);

            context.setRaftActorLeadershipTransferCohort(leadershipTransferInProgress);
            leadershipTransferInProgress.init();

        } else {
            LOG.debug("{}: prior leader transfer in progress - adding callback", memberId());
            leadershipTransferInProgress.addOnComplete(onComplete);
        }
    }

    private void onShutDown() {
        LOG.debug("{}: onShutDown", memberId());

        if (shuttingDown) {
            return;
        }

        shuttingDown = true;

        switch (getCurrentBehavior().raftRole()) {
            case Leader:
            case PreLeader:
                // Fall-through to more work
                break;
            default:
                // For non-leaders shutdown is a no-op
                self().tell(PoisonPill.getInstance(), self());
                return;
        }

        if (context.hasFollowers()) {
            initiateLeadershipTransfer(new RaftActorLeadershipTransferCohort.OnComplete() {
                @Override
                public void onSuccess(final ActorRef raftActorRef) {
                    LOG.debug("{}: leader transfer succeeded - sending PoisonPill", memberId());
                    raftActorRef.tell(PoisonPill.getInstance(), raftActorRef);
                }

                @Override
                public void onFailure(final ActorRef raftActorRef) {
                    LOG.debug("{}: leader transfer failed - sending PoisonPill", memberId());
                    raftActorRef.tell(PoisonPill.getInstance(), raftActorRef);
                }
            }, null, TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        } else {
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
        }
    }

    private void onLeaderTransitioning(final LeaderTransitioning leaderTransitioning) {
        LOG.debug("{}: onLeaderTransitioning: {}", memberId(), leaderTransitioning);
        final var roleChangeNotifier = roleChangeNotifier();
        if (roleChangeNotifier != null && getRaftState() == RaftRole.Follower
                && leaderTransitioning.getLeaderId().equals(getCurrentBehavior().getLeaderId())) {
            roleChangeNotifier.tell(newLeaderStateChanged(memberId(), null,
                getCurrentBehavior().getLeaderPayloadVersion()), self());
        }
    }

    @NonNullByDefault
    private void switchBehavior(final Function<RaftActorContext, RaftActorBehavior> ctor, final long newTerm) {
        if (context.getRaftPolicy().automaticElectionsEnabled()) {
            LOG.warn("{}: Ignoring request to switch behavior when automatic elections are disabled", memberId());
            return;
        }

        try {
            context.persistTermInfo(new TermInfo(newTerm, ""));
        } catch (IOException e) {
            // FIXME: do not mask IOException
            throw new UncheckedIOException(e);
        }

        switchBehavior(behaviorStateTracker.capture(getCurrentBehavior()), ctor.apply(context));
    }

    private void switchBehavior(final BehaviorState oldBehaviorState, final RaftActorBehavior nextBehavior) {
        setCurrentBehavior(nextBehavior);
        handleBehaviorChange(oldBehaviorState, nextBehavior);
    }

    @VisibleForTesting
    RaftActorSnapshotMessageSupport newRaftActorSnapshotMessageSupport() {
        return new RaftActorSnapshotMessageSupport(context.getSnapshotManager());
    }

    private OnDemandRaftState getOnDemandRaftState() {
        // Debugging message to retrieve raft stats.

        final var peerAddresses = new HashMap<String, String>();
        final var peerVotingStates = new HashMap<String, Boolean>();
        for (var info : context.getPeers()) {
            peerVotingStates.put(info.getId(), info.isVoting());
            peerAddresses.put(info.getId(), info.getAddress() != null ? info.getAddress() : "");
        }

        final var currentBehavior = getCurrentBehavior();
        final var termInfo = context.termInfo();
        final var replLog = replicatedLog();

        final var builder = newOnDemandRaftStateBuilder()
                .commitIndex(replLog.getCommitIndex())
                .currentTerm(termInfo.term())
                .inMemoryJournalDataSize(replLog.dataSize())
                .inMemoryJournalLogSize(replLog.size())
                .isSnapshotCaptureInitiated(context.getSnapshotManager().isCapturing())
                .lastApplied(replLog.getLastApplied())
                .lastIndex(replLog.lastIndex())
                .lastTerm(replLog.lastTerm())
                .leader(getLeaderId())
                .raftState(currentBehavior.raftRole())
                .replicatedToAllIndex(currentBehavior.getReplicatedToAllIndex())
                .snapshotIndex(replLog.getSnapshotIndex())
                .snapshotTerm(replLog.getSnapshotTerm())
                .votedFor(termInfo.votedFor())
                .isVoting(context.isVotingMember())
                .peerAddresses(peerAddresses)
                .peerVotingStates(peerVotingStates)
                .raftPolicySymbolicName(context.getConfigParams().getRaftPolicy().symbolicName());

        final var lastLogEntry = replicatedLog().lastMeta();
        if (lastLogEntry != null) {
            builder.lastLogIndex(lastLogEntry.index()).lastLogTerm(lastLogEntry.term());
        }

        if (currentBehavior instanceof AbstractLeader leader) {
            builder.followerInfoList(leader.getFollowerIds().stream()
                .map(leader::getFollower)
                .map(this::formatLogInfo)
                .collect(ImmutableList.toImmutableList()));
        }

        return builder.build();
    }

    private @NonNull FollowerInfo formatLogInfo(final FollowerLogInformation logInfo) {
        final var followerId = logInfo.getId();
        final var peerInfo = context.getPeerInfo(followerId);

        // "HH:mm:ss.SSS"
        final var d = Duration.ofNanos(logInfo.nanosSinceLastActivity());
        final var hrs = d.toHours();
        var sinceLast = "%02d:%02d:%02d.%03d".formatted(hrs, d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
        if (hrs > 23) {
            LOG.warn("{}: reporting follower {} time since active as 23:59:59.999 instead of {}", memberId(),
                followerId, sinceLast);
            sinceLast = "23:59:59.999";
        }

        return new FollowerInfo(followerId, logInfo.getNextIndex(), logInfo.getMatchIndex(), logInfo.isFollowerActive(),
            sinceLast, peerInfo.isVoting());
    }

    protected OnDemandRaftState.@NonNull AbstractBuilder<?, ?> newOnDemandRaftStateBuilder() {
        return new OnDemandRaftState.Builder();
    }

    private void handleBehaviorChange(final BehaviorState oldBehaviorState, final RaftActorBehavior currentBehavior) {
        final var oldBehavior = oldBehaviorState.getBehavior();
        if (oldBehavior != currentBehavior) {
            onStateChanged();
        }

        final var lastLeaderId = oldBehavior == null ? null : oldBehaviorState.getLastLeaderId();
        final var lastValidLeaderId = oldBehavior == null ? null : oldBehaviorState.getLastValidLeaderId();

        // it can happen that the state has not changed but the leader has changed.
        final var leaderId = currentBehavior.getLeaderId();
        final var roleChangeNotifier = roleChangeNotifier();
        if (!Objects.equals(lastLeaderId, leaderId)
                || oldBehaviorState.getLeaderPayloadVersion() != currentBehavior.getLeaderPayloadVersion()) {
            if (roleChangeNotifier != null) {
                roleChangeNotifier.tell(newLeaderStateChanged(memberId(), leaderId,
                    currentBehavior.getLeaderPayloadVersion()), self());
            }

            onLeaderChanged(lastValidLeaderId, leaderId);

            final var leadershipTransferInProgress = context.getRaftActorLeadershipTransferCohort();
            if (leadershipTransferInProgress != null) {
                leadershipTransferInProgress.onNewLeader(leaderId);
            }

            votingConfigSupport.onNewLeader(leaderId);
        }

        if (roleChangeNotifier != null) {
            notifyRoleChange(roleChangeNotifier, currentBehavior.raftRole(), oldBehavior);
        }
    }

    @NonNullByDefault
    private void notifyRoleChange(final ActorRef target, final RaftRole newRole,
            final @Nullable RaftActorBehavior oldBehavior) {
        final RaftRole oldRole;
        if (oldBehavior != null) {
            oldRole = oldBehavior.raftRole();
            if (newRole.equals(oldRole)) {
                return;
            }
        } else {
            oldRole = null;
        }
        target.tell(new RoleChanged(memberId(), newRole, oldRole), self());
    }

    @NonNullByDefault
    private void applyCommand(final @Nullable Identifier identifier, final LogEntry entry) {
        final long startTime = System.nanoTime();

        final var payload = entry.command();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: Applying state for log index {} data {}", memberId(), entry.index(), payload);
        }

        if (payload instanceof StateCommand stateCommand) {
            applyCommand(identifier, stateCommand);
        }

        final long elapsedTime = System.nanoTime() - startTime;
        if (elapsedTime >= APPLY_STATE_DELAY_THRESHOLD_IN_NANOS) {
            LOG.debug("{}: ApplyState took more time than expected. Elapsed Time = {} ms Entry = {}", memberId(),
                TimeUnit.NANOSECONDS.toMillis(elapsedTime), entry);
        }

        // Send the ApplyState message back to self to handle further processing asynchronously.
        // FIXME: executeInSelf() invoking the further processing part
        self().tell(new ApplyState(identifier, entry), self());
    }

    /**
     * Apply a {@link StateCommand} to update the actor's state.
     *
     * @param identifier  The identifier of the persisted data. This is also the same identifier that was passed to
     *                    {@link #submitCommand(Identifier, AbstractStateCommand, boolean)} by the derived actor. May be
     *                     {@code null} when the RaftActor is behaving as a follower or during recovery
     * @param command     the {@link StateCommand} to apply
     */
    @NonNullByDefault
    protected abstract void applyCommand(@Nullable Identifier identifier, StateCommand command);

    @NonNullByDefault
    final LeaderStateChanged newLeaderStateChanged(final String myMemberId, final @Nullable String leaderId,
            final short leaderPayloadVersion) {
        return wrapLeaderStateChanged(new DefaultLeaderStateChanged(myMemberId, leaderId, leaderPayloadVersion));
    }

    @NonNullByDefault
    protected LeaderStateChanged wrapLeaderStateChanged(final LeaderStateChanged change) {
        return change;
    }

    /**
     * Request a {@link RaftCommand} to be applied to the finite state machine. Once consensus is reached,
     * {@link #applyCommand(ApplyState)} will be called with matching arguments.
     *
     * @param identifier optional identifier to report back
     * @param command the command
     */
    @VisibleForTesting
    @NonNullByDefault
    final void submitCommand(final @Nullable Identifier identifier, final AbstractRaftCommand command) {
        submitCommand(identifier, command, false);
    }

    /**
     * Request a {@link StateCommand} to be applied to the finite state machine. Once consensus is reached,
     * {@link #applyCommand(Identifier, StateCommand)} will be called with matching arguments.
     *
     * @param identifier optional identifier to report back
     * @param command the command
     */
    @VisibleForTesting
    @NonNullByDefault
    public final void submitCommand(final Identifier identifier, final AbstractStateCommand command,
            final boolean batchHint) {
        requireNonNull(identifier);
        requireNonNull(command);

        if (hasFollowers() || isRecoveryApplicable()) {
            submitCommand(identifier, (Payload) command, batchHint);
        } else {
            applyCommand(identifier, command);
        }
    }

    @NonNullByDefault
    private void submitCommand(final @Nullable Identifier identifier, final Payload command, final boolean batchHint) {
        requireNonNull(command);
        final var replLog = replicatedLog();
        final var entryIndex = replLog.lastIndex() + 1;
        final var entryTerm = context.currentTerm();

        LOG.debug("{}: Persist data index={} term={} command={}", memberId(), entryIndex, entryTerm, command);
        boolean wasAppended = replLog.appendSubmitted(entryIndex, entryTerm, command, entry -> {
            final var currentLog = replicatedLog();

            if (!hasFollowers()) {
                // Increment the Commit Index and the Last Applied values
                currentLog.setCommitIndex(entry.index());
                currentLog.setLastApplied(entry.index());

                // Apply the state immediately.
                applyCommand(identifier, entry);

                // We have finished applying the command, tell ReplicatedLog about that
                currentLog.markLastApplied();
            } else {
                currentLog.captureSnapshotIfReady(entry);

                // Local persistence is complete so send the CheckConsensusReached message to the behavior (which
                // normally should still be the leader) to check if consensus has now been reached in conjunction with
                // follower replication.
                if (getCurrentBehavior() instanceof AbstractLeader leader) {
                    leader.checkConsensusReached();
                }
            }
        });

        if (wasAppended && hasFollowers()) {
            // Send log entry for replication.
            getCurrentBehavior().handleMessage(self(), new Replicate(entryIndex, !batchHint, identifier));
        }
    }

    private boolean hasFollowers() {
        return context.hasFollowers();
    }

    private ReplicatedLog replicatedLog() {
        return context.getReplicatedLog();
    }

    @VisibleForTesting
    void setCurrentBehavior(final RaftActorBehavior behavior) {
        context.setCurrentBehavior(behavior);
    }

    protected final RaftActorBehavior getCurrentBehavior() {
        return context.getCurrentBehavior();
    }

    /**
     * Derived actors can call the isLeader method to check if the current
     * RaftActor is the Leader or not.
     *
     * @return true it this RaftActor is a Leader false otherwise
     */
    protected boolean isLeader() {
        return memberId().equals(getCurrentBehavior().getLeaderId());
    }

    protected final boolean isLeaderActive() {
        return getRaftState() != RaftRole.IsolatedLeader && getRaftState() != RaftRole.PreLeader
                && !shuttingDown && !isLeadershipTransferInProgress();
    }

    protected boolean isLeadershipTransferInProgress() {
        RaftActorLeadershipTransferCohort leadershipTransferInProgress = context.getRaftActorLeadershipTransferCohort();
        return leadershipTransferInProgress != null && leadershipTransferInProgress.isTransferring();
    }

    /**
     * Derived actor can call getLeader if they need a reference to the Leader. This would be useful for example in
     * forwarding a request to an actor which is the leader.
     *
     * @return A reference to the leader if known, {@code null} otherwise
     */
    protected final @Nullable ActorSelection getLeader() {
        final var leaderAddress = getLeaderAddress();
        return leaderAddress != null ? getContext().actorSelection(leaderAddress) : null;
    }

    /**
     * Returns the id of the current leader.
     *
     * @return the current leader's id
     */
    protected final String getLeaderId() {
        return getCurrentBehavior().getLeaderId();
    }

    @VisibleForTesting
    protected final RaftRole getRaftState() {
        return getCurrentBehavior().raftRole();
    }

    public final RaftActorContext getRaftActorContext() {
        return context;
    }

    protected void updateConfigParams(final ConfigParams configParams) {

        // obtain the RaftPolicy for oldConfigParams and the updated one.
        final var oldRaftPolicy = context.getConfigParams().getRaftPolicy();
        final var newRaftPolicy = configParams.getRaftPolicy();

        LOG.debug("{}: RaftPolicy used with prev.config {}, RaftPolicy used with newConfig {}", memberId(),
            oldRaftPolicy, newRaftPolicy);
        context.setConfigParams(configParams);
        if (!Objects.equals(oldRaftPolicy, newRaftPolicy)) {
            // The RaftPolicy was modified. If the current behavior is Follower then re-initialize to Follower
            // but transfer the previous leaderId so it doesn't immediately try to schedule an election. This
            // avoids potential disruption. Otherwise, switch to Follower normally.
            if (getCurrentBehavior() instanceof Follower follower) {
                LOG.debug("{}: Re-initializing to Follower with previous leaderId {}", memberId(),
                    follower.getLeaderId());
                changeCurrentBehavior(follower.copy());
            } else {
                initializeBehavior();
            }
        }
    }

    @VisibleForTesting
    public final boolean isRecoveryApplicable() {
        return persistenceControl.isRecoveryApplicable();
    }

    @VisibleForTesting
    @NonNullByDefault
    protected final PersistenceProvider persistence() {
        return persistenceControl;
    }

    protected final void setPersistence(final boolean persistent) {
        if (persistent) {
            if (persistenceControl.becomePersistent() && getCurrentBehavior() != null) {
                LOG.info("{}: Persistence has been enabled - capturing snapshot", memberId());
                captureSnapshot();
            }
        } else {
            persistenceControl.becomeTransient();
        }
    }

    /**
     * setPeerAddress sets the address of a known peer at a later time.
     *
     * <p>This is to account for situations where a we know that a peer
     * exists but we do not know an address up-front. This may also be used in
     * situations where a known peer starts off in a different location and we
     * need to change it's address
     *
     * <p>Note that if the peerId does not match the list of peers passed to
     * this actor during construction an IllegalStateException will be thrown.
     */
    protected void setPeerAddress(final String peerId, final String peerAddress) {
        context.setPeerAddress(peerId, peerAddress);
    }

    protected @NonNull RecoveryObserver recoveryObserver() {
        return NoopRecoveryObserver.INSTANCE;
    }

    /**
     * Returns the RaftActorRecoveryCohort to participate in persistence recovery.
     */
    protected abstract @NonNull RaftActorRecoveryCohort getRaftActorRecoveryCohort();

    /**
     * Returns the snapshot to restore from on recovery.
     *
     * @return the snapshot or null if there's no snapshot to restore
     */
    protected abstract @Nullable Snapshot getRestoreFromSnapshot();

    /**
     * This method is called when recovery is complete.
     */
    protected abstract void onRecoveryComplete();

    /**
     * Returns the RaftActorSnapshotCohort to participate in snapshot captures.
     */
    protected abstract @NonNull RaftActorSnapshotCohort<?> getRaftActorSnapshotCohort();

    /**
     * This method will be called by the RaftActor when the state of the
     * RaftActor changes. The derived actor can then use methods like
     * isLeader or getLeader to do something useful
     */
    protected abstract void onStateChanged();

    /**
     * Notifier Actor for this RaftActor to notify when a role change happens.
     *
     * @return ActorRef - ActorRef of the notifier or {@code null} if none.
     */
    protected abstract @Nullable ActorRef roleChangeNotifier();

    /**
     * This method is called on the leader when a voting change operation completes.
     */
    protected void onVotingStateChangeComplete() {
    }

    /**
     * This method is called prior to operations such as leadership transfer and actor shutdown when the leader
     * must pause or stop its duties. This method allows derived classes to gracefully pause or finish current
     * work prior to performing the operation. On completion of any work, the run method must be called on the
     * given Runnable to proceed with the given operation. <b>Important:</b> the run method must be called on
     * this actor's thread dispatcher as as it modifies internal state.
     *
     * <p>The default implementation immediately runs the operation.
     *
     * @param operation the operation to run
     */
    protected void pauseLeader(final Runnable operation) {
        operation.run();
    }

    /**
     * This method is invoked when the actions hooked to the leader becoming paused failed to execute and the leader
     * should resume normal operations.
     *
     * <p>Note this method can be invoked even before the operation supplied to {@link #pauseLeader(Runnable)} is
     * invoked.
     */
    protected void unpauseLeader() {

    }

    protected void onLeaderChanged(final String oldLeader, final String newLeader) {
    }

    private String getLeaderAddress() {
        if (isLeader()) {
            return self().path().toString();
        }
        String leaderId = getLeaderId();
        if (leaderId == null) {
            return null;
        }
        String peerAddress = context.getPeerAddress(leaderId);
        LOG.debug("{}: getLeaderAddress leaderId = {} peerAddress = {}", memberId(), leaderId, peerAddress);

        return peerAddress;
    }

    private void captureSnapshot() {
        final var snapshotManager = context.getSnapshotManager();

        if (!snapshotManager.isCapturing()) {
            final long idx = getCurrentBehavior().getReplicatedToAllIndex();
            final var last = replicatedLog().lastMeta();
            LOG.debug("{}: Take a snapshot of current state. lastReplicatedLog is {} and replicatedToAllIndex is {}",
                memberId(), last, idx);

            snapshotManager.captureWithForcedTrim(last, idx);
        }
    }

    private @NonNull Snapshot getSnapshot() {
        LOG.debug("{}: onGetSnapshot", memberId());

        final var termInfo = context.termInfo();
        final var clusterConfig = context.getPeerServerInfo(true);
        if (isRecoveryApplicable()) {
            final var replLog = replicatedLog();
            final var captureSnapshot = replLog.newCaptureSnapshot(replLog.lastMeta(), -1, true, hasFollowers());

            return Snapshot.create(getRaftActorSnapshotCohort().takeSnapshot(), captureSnapshot.getUnAppliedEntries(),
                captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm(), termInfo, clusterConfig);
        }

        return Snapshot.create(null, List.of(), -1, -1, -1, -1, termInfo, clusterConfig);
    }

    /**
     * Switch this member to non-voting status. This is a no-op for all behaviors except when we are the leader,
     * in which case we need to step down.
     */
    void becomeNonVoting() {
        if (isLeader()) {
            initiateLeadershipTransfer(new RaftActorLeadershipTransferCohort.OnComplete() {
                @Override
                public void onSuccess(final ActorRef raftActorRef) {
                    LOG.debug("{}: leader transfer succeeded after change to non-voting", memberId());
                    ensureFollowerState();
                }

                @Override
                public void onFailure(final ActorRef raftActorRef) {
                    LOG.debug("{}: leader transfer failed after change to non-voting", memberId());
                    ensureFollowerState();
                }

                private void ensureFollowerState() {
                    // Whether or not leadership transfer succeeded, we have to step down as leader and
                    // switch to Follower so ensure that.
                    if (getRaftState() != RaftRole.Follower) {
                        initializeBehavior();
                    }
                }
            }, null, RaftActorLeadershipTransferCohort.USE_DEFAULT_LEADER_TIMEOUT);
        }
    }

    final void saveEmptySnapshot() throws IOException {
        persistenceControl.saveVotingConfig(context.getPeerServerInfo(true));
    }

    /**
     * A point-in-time capture of {@link RaftActorBehavior} state critical for transitioning between behaviors.
     */
    private abstract static class BehaviorState implements Immutable {

        abstract @Nullable RaftActorBehavior getBehavior();

        abstract @Nullable String getLastValidLeaderId();

        abstract @Nullable String getLastLeaderId();

        abstract short getLeaderPayloadVersion();
    }

    /**
     * A {@link BehaviorState} corresponding to non-null {@link RaftActorBehavior} state.
     */
    private static final class SimpleBehaviorState extends BehaviorState {
        private final RaftActorBehavior behavior;
        private final String lastValidLeaderId;
        private final String lastLeaderId;
        private final short leaderPayloadVersion;

        SimpleBehaviorState(final String lastValidLeaderId, final String lastLeaderId,
                final RaftActorBehavior behavior) {
            this.lastValidLeaderId = lastValidLeaderId;
            this.lastLeaderId = lastLeaderId;
            this.behavior = requireNonNull(behavior);
            leaderPayloadVersion = behavior.getLeaderPayloadVersion();
        }

        @Override
        RaftActorBehavior getBehavior() {
            return behavior;
        }

        @Override
        String getLastValidLeaderId() {
            return lastValidLeaderId;
        }

        @Override
        short getLeaderPayloadVersion() {
            return leaderPayloadVersion;
        }

        @Override
        String getLastLeaderId() {
            return lastLeaderId;
        }
    }

    /**
     * Class tracking behavior-related information, which we need to keep around and pass across behavior switches.
     * An instance is created for each RaftActor. It has two functions:
     * - it keeps track of the last leader ID we have encountered since we have been created
     * - it creates state capture needed to transition from one behavior to the next
     */
    private static final class BehaviorStateTracker {
        /**
         * A {@link BehaviorState} corresponding to null {@link RaftActorBehavior} state. Since null behavior is only
         * allowed before we receive the first message, we know the leader ID to be null.
         */
        private static final BehaviorState NULL_BEHAVIOR_STATE = new BehaviorState() {
            @Override
            RaftActorBehavior getBehavior() {
                return null;
            }

            @Override
            String getLastValidLeaderId() {
                return null;
            }

            @Override
            short getLeaderPayloadVersion() {
                return -1;
            }

            @Override
            String getLastLeaderId() {
                return null;
            }
        };

        private String lastValidLeaderId;
        private String lastLeaderId;

        BehaviorState capture(final RaftActorBehavior behavior) {
            if (behavior == null) {
                verify(lastValidLeaderId == null, "Null behavior with non-null last leader");
                return NULL_BEHAVIOR_STATE;
            }

            lastLeaderId = behavior.getLeaderId();
            if (lastLeaderId != null) {
                lastValidLeaderId = lastLeaderId;
            }

            return new SimpleBehaviorState(lastValidLeaderId, lastLeaderId, behavior);
        }
    }
}
