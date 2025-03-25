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
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
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
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.raft.api.TermInfo;
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
public abstract class RaftActor extends AbstractUntypedPersistentActor {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActor.class);
    private static final long APPLY_STATE_DELAY_THRESHOLD_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

    // This context should NOT be passed directly to any other actor it is  only to be consumed
    // by the RaftActorBehaviors.
    private final @NonNull LocalAccess localAccess;
    private final @NonNull PersistenceControl persistenceControl;
    private final @NonNull BehaviorStateTracker behaviorStateTracker = new BehaviorStateTracker();

    // FIXME: should be valid only after recovery
    private final @NonNull RaftActorContextImpl context;

    private RaftActorRecoverySupport raftRecovery;
    private RaftActorSnapshotMessageSupport snapshotSupport;
    private RaftActorServerConfigurationSupport serverConfigurationSupport;
    private boolean shuttingDown;

    protected RaftActor(final @NonNull Path stateDir, final @NonNull String memberId,
            final Map<String, String> peerAddresses, final Optional<ConfigParams> configParams,
            final short payloadVersion) {
        super(memberId);
        localAccess = new LocalAccess(memberId, stateDir.resolve(memberId));

        final var params = configParams.orElseGet(DefaultConfigParamsImpl::new);
        // FIXME: propagate to context
        final var streamFactory = new FileBackedOutputStreamFactory(
            params.getFileBackedStreamingThreshold(), params.getTempFileDirectory());
        persistenceControl = new PersistenceControl(this, streamFactory);

        context = new RaftActorContextImpl(self(), getContext(), localAccess, peerAddresses, params, payloadVersion,
            persistenceControl, this::handleApplyState, this::executeInSelf);
    }

    /**
     * Return the unique member name of this actor.
     *
     * @return The member name
     */
    public final @NonNull String memberId() {
        return persistenceId();
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

        context.getSnapshotManager().setSnapshotCohort(getRaftActorSnapshotCohort());
        snapshotSupport = newRaftActorSnapshotMessageSupport();
        serverConfigurationSupport = new RaftActorServerConfigurationSupport(this);
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
        super.postStop();
    }

    @Override
    protected void handleRecover(final Object message) {
        if (raftRecovery == null) {
            raftRecovery = newRaftActorRecoverySupport();
        }

        boolean recoveryComplete = raftRecovery.handleRecoveryMessage(this, message);
        if (recoveryComplete) {
            onRecoveryComplete();

            initializeBehavior();

            raftRecovery = null;
        }
    }

    @VisibleForTesting
    RaftActorRecoverySupport newRaftActorRecoverySupport() {
        return new RaftActorRecoverySupport(localAccess, context, getRaftActorRecoveryCohort());
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
     * Method exposed for subclasses to plug-in their logic. This method is invoked by {@link #handleCommand(Object)}
     * for messages which are not handled by this class. Subclasses overriding this class should fall back to this
     * implementation for messages which they do not handle
     *
     * @param message Incoming command message
     */
    protected void handleNonRaftCommand(final Object message) {
        unhandled(message);
    }

    /**
     * Handles a message.
     *
     * @deprecated This method is not final for testing purposes. DO NOT OVERRIDE IT, override
     *             {@link #handleNonRaftCommand(Object)} instead.
     */
    @Deprecated
    @Override
    // FIXME: make this method final once our unit tests do not need to override it
    protected void handleCommand(final Object message) {
        if (serverConfigurationSupport.handleMessage(message, getSender())) {
            return;
        }
        if (snapshotSupport.handleSnapshotMessage(message)) {
            return;
        }
        if (message instanceof ApplyState applyState) {
            if (!hasFollowers()) {
                // for single node, the capture should happen after the apply state
                // as we delete messages from the persistent journal which have made it to the snapshot
                // capturing the snapshot before applying makes the persistent journal and snapshot out of sync
                // and recovery shows data missing
                replicatedLog().captureSnapshotIfReady(applyState.getReplicatedLogEntry());

                context.getSnapshotManager().trimLog(replicatedLog().getLastApplied());
            }

            possiblyHandleBehaviorMessage(message);
        } else if (message instanceof ApplyJournalEntries applyEntries) {
            LOG.debug("{}: Persisting ApplyJournalEntries with index={}", memberId(), applyEntries.getToIndex());
            persistence().persistAsync(applyEntries, unused -> { });
        } else if (message instanceof FindLeader) {
            getSender().tell(new FindLeaderReply(getLeaderAddress()), self());
        } else if (message instanceof GetOnDemandRaftState) {
            getSender().tell(getOnDemandRaftState(), self());
        } else if (message instanceof GetSnapshot) {
            getSender().tell(new GetSnapshotReply(memberId(), getSnapshot()), ActorRef.noSender());
        } else if (message instanceof InitiateCaptureSnapshot) {
            captureSnapshot();
        } else if (message instanceof BecomeFollower(var newTerm)) {
            switchBehavior(Follower::new, newTerm);
        } else if (message instanceof BecomeLeader(var newTerm)) {
            switchBehavior(Leader::new, newTerm);
        } else if (message instanceof LeaderTransitioning leaderTransitioning) {
            onLeaderTransitioning(leaderTransitioning);
        } else if (message instanceof Shutdown) {
            onShutDown();
        } else if (message instanceof Runnable runnable) {
            runnable.run();
        } else if (message instanceof NoopPayload noopPayload) {
            persistData(null, null, noopPayload, false);
        } else if (message instanceof RequestLeadership requestLeadership) {
            onRequestLeadership(requestLeadership);
        } else if (!possiblyHandleBehaviorMessage(message)) {
            if (message instanceof JournalProtocol.Response response
                && persistenceControl.handleJournalResponse(response)) {
                LOG.debug("{}: handled a journal response", memberId());
            } else if (message instanceof SnapshotProtocol.Response response
                && persistenceControl.handleSnapshotResponse(response)) {
                LOG.debug("{}: handled a snapshot response", memberId());
            } else {
                handleNonRaftCommand(message);
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
                .customRaftPolicyClassName(context.getConfigParams().getCustomRaftPolicyImplementationClass());

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

            serverConfigurationSupport.onNewLeader(leaderId);
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

    private void handleApplyState(final ApplyState applyState) {
        final long startTime = System.nanoTime();

        final var entry = applyState.getReplicatedLogEntry();
        final var payload = entry.getData();
        if (LOG.isDebugEnabled()) {
            LOG.debug("{}: Applying state for log index {} data {}", memberId(), entry.index(), payload);
        }

        if (!(payload instanceof NoopPayload) && !(payload instanceof ClusterConfig)) {
            applyState(applyState.getClientActor(), applyState.getIdentifier(), payload);
        }

        final long elapsedTime = System.nanoTime() - startTime;
        if (elapsedTime >= APPLY_STATE_DELAY_THRESHOLD_IN_NANOS) {
            LOG.debug("{}: ApplyState took more time than expected. Elapsed Time = {} ms ApplyState = {}", memberId(),
                TimeUnit.NANOSECONDS.toMillis(elapsedTime), applyState);
        }

        // Send the ApplyState message back to self to handle further processing asynchronously.
        self().tell(applyState, self());
    }

    @NonNullByDefault
    final LeaderStateChanged newLeaderStateChanged(final String memberId, final @Nullable String leaderId,
            final short leaderPayloadVersion) {
        return wrapLeaderStateChanged(new DefaultLeaderStateChanged(memberId, leaderId, leaderPayloadVersion));
    }

    @NonNullByDefault
    protected LeaderStateChanged wrapLeaderStateChanged(final LeaderStateChanged change) {
        return change;
    }

    @Override
    public long snapshotSequenceNr() {
        // When we do a snapshot capture, we also capture and save the sequence-number of the persistent journal,
        // so that we can delete the persistent journal based on the saved sequence-number.
        // However, when Akka replays the journal during recovery, it replays it from the sequence number when the
        // snapshot was saved and not the number we saved. We would want to override it, by asking Akka to use the
        // last-sequence number known to us.
        return context.getSnapshotManager().getLastSequenceNumber();
    }

    /**
     * Persists the given Payload in the journal and replicates to any followers. After successful completion,
     * {@link #applyState(ActorRef, Identifier, Object)} is notified.
     *
     * @param clientActor optional ActorRef that is provided via the applyState callback
     * @param identifier the payload identifier
     * @param data the payload data to persist
     * @param batchHint if true, an attempt is made to delay immediate replication and batch the payload with
     *        subsequent payloads for efficiency. Otherwise the payload is immediately replicated.
     */
    protected final void persistData(final ActorRef clientActor, final Identifier identifier, final Payload data,
            final boolean batchHint) {
        final var replLog = replicatedLog();
        final var logEntry = new SimpleReplicatedLogEntry(replLog.lastIndex() + 1, context.currentTerm(), data);
        logEntry.setPersistencePending(true);

        LOG.debug("{}: Persist data {}", memberId(), logEntry);

        boolean wasAppended = replLog.appendAndPersist(logEntry, persistedEntry -> {
            // Clear the persistence pending flag in the log entry.
            persistedEntry.setPersistencePending(false);

            final var currentLog = replicatedLog();

            if (!hasFollowers()) {
                // Increment the Commit Index and the Last Applied values
                currentLog.setCommitIndex(persistedEntry.index());
                currentLog.setLastApplied(persistedEntry.index());

                // Apply the state immediately.
                handleApplyState(new ApplyState(clientActor, identifier, persistedEntry));

                // Send a ApplyJournalEntries message so that we write the fact that we applied
                // the state to durable storage
                self().tell(new ApplyJournalEntries(persistedEntry.index()), self());

            } else {
                currentLog.captureSnapshotIfReady(persistedEntry);

                // Local persistence is complete so send the CheckConsensusReached message to the behavior (which
                // normally should still be the leader) to check if consensus has now been reached in conjunction with
                // follower replication.
                if (getCurrentBehavior() instanceof AbstractLeader leader) {
                    leader.checkConsensusReached();
                }
            }
        }, true);

        if (wasAppended && hasFollowers()) {
            // Send log entry for replication.
            getCurrentBehavior().handleMessage(self(),
                new Replicate(logEntry.index(), !batchHint, clientActor, identifier));
        }
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
        String oldRaftPolicy = context.getConfigParams().getCustomRaftPolicyImplementationClass();
        String newRaftPolicy = configParams.getCustomRaftPolicyImplementationClass();

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

    public final boolean isRecoveryApplicable() {
        return persistence().isRecoveryApplicable();
    }

    protected final @NonNull DataPersistenceProvider persistence() {
        return persistenceControl.delegate();
    }

    @Deprecated
    @VisibleForTesting
    protected final void setPersistence(final DataPersistenceProvider provider) {
        persistenceControl.setDelegate(requireNonNull(provider));
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

    /**
     * The applyState method will be called by the RaftActor when some data
     * needs to be applied to the actor's state.
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
    protected abstract void applyState(ActorRef clientActor, Identifier identifier, Object data);

    /**
     * Returns the RaftActorRecoveryCohort to participate in persistence recovery.
     */
    protected abstract @NonNull RaftActorRecoveryCohort getRaftActorRecoveryCohort();

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

    protected boolean hasFollowers() {
        return context.hasFollowers();
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
            final var captureSnapshot = context.getSnapshotManager().newCaptureSnapshot(replicatedLog().lastMeta(), -1,
                true);

            return Snapshot.create(getRaftActorSnapshotCohort().takeSnapshot(), captureSnapshot.getUnAppliedEntries(),
                captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(),
                captureSnapshot.getLastAppliedIndex(), captureSnapshot.getLastAppliedTerm(), termInfo, clusterConfig);
        }

        return Snapshot.create(EmptyState.INSTANCE, List.of(), -1, -1, -1, -1, termInfo, clusterConfig);
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

    /**
     * A point-in-time capture of {@link RaftActorBehavior} state critical for transitioning between behaviors.
     */
    private abstract static class BehaviorState implements Immutable {
        @Nullable abstract RaftActorBehavior getBehavior();

        @Nullable abstract String getLastValidLeaderId();

        @Nullable abstract String getLastLeaderId();

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
