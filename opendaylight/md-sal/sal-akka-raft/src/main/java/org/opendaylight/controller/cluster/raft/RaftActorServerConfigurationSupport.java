/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Cancellable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.SnapshotManager.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ChangeServersVotingStatus;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeRequest;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.util.AbstractUUIDIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles server configuration related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
final class RaftActorServerConfigurationSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupport.class);

    @SuppressWarnings("checkstyle:MemberName")
    private final OperationState IDLE = new Idle();

    private final RaftActor raftActor;

    private final RaftActorContext raftContext;

    private final Queue<ServerOperationContext<?>> pendingOperationsQueue = new ArrayDeque<>();

    private OperationState currentOperationState = IDLE;

    RaftActorServerConfigurationSupport(final RaftActor raftActor) {
        this.raftActor = raftActor;
        raftContext = raftActor.getRaftActorContext();
    }

    boolean handleMessage(final Object message, final ActorRef sender) {
        return switch (message) {
            case ApplyState msg -> onApplyState(msg);
            case AddServer msg -> {
                onAddServer(msg, sender);
                yield true;
            }
            case RemoveServer msg -> {
                onRemoveServer(msg, sender);
                yield true;
            }
            case ChangeServersVotingStatus msg -> {
                onChangeServersVotingStatus(msg, sender);
                yield true;
            }
            case ServerOperationTimeout msg -> {
                currentOperationState.onServerOperationTimeout(msg);
                yield true;
            }
            case UnInitializedFollowerSnapshotReply msg -> {
                currentOperationState.onUnInitializedFollowerSnapshotReply(msg);
                yield true;
            }
            case SnapshotComplete msg -> {
                currentOperationState.onSnapshotComplete();
                yield false;
            }
            default -> false;
        };
    }

    void onNewLeader(final String leaderId) {
        currentOperationState.onNewLeader(leaderId);
    }

    private @NonNull String memberId() {
        return raftActor.memberId();
    }

    private void onChangeServersVotingStatus(final ChangeServersVotingStatus message, final ActorRef sender) {
        LOG.debug("{}: onChangeServersVotingStatus: {}, state: {}", memberId(), message, currentOperationState);

        // The following check is a special case. Normally we fail an operation if there's no leader.
        // Consider a scenario where one has 2 geographically-separated 3-node clusters, one a primary and
        // the other a backup such that if the primary cluster is lost, the backup can take over. In this
        // scenario, we have a logical 6-node cluster where the primary sub-cluster is configured as voting
        // and the backup sub-cluster as non-voting such that the primary cluster can make progress without
        // consensus from the backup cluster while still replicating to the backup. On fail-over to the backup,
        // a request would be sent to a member of the backup cluster to flip the voting states, ie make the
        // backup sub-cluster voting and the lost primary non-voting. However since the primary majority
        // cluster is lost, there would be no leader to apply, persist and replicate the server config change.
        // Therefore, if the local server is currently non-voting and is to be changed to voting and there is
        // no current leader, we will try to elect a leader using the new server config in order to replicate
        // the change and progress.
        boolean localServerChangingToVoting = Boolean.TRUE.equals(message.getServerVotingStatusMap().get(memberId()));
        boolean hasNoLeader = raftActor.getLeaderId() == null;
        if (localServerChangingToVoting && !raftContext.isVotingMember() && hasNoLeader) {
            currentOperationState.onNewOperation(new ChangeServersVotingStatusContext(message, sender, true));
        } else {
            onNewOperation(new ChangeServersVotingStatusContext(message, sender, false));
        }
    }

    private void onRemoveServer(final RemoveServer removeServer, final ActorRef sender) {
        LOG.debug("{}: onRemoveServer: {}, state: {}", memberId(), removeServer, currentOperationState);
        boolean isSelf = removeServer.getServerId().equals(memberId());
        if (isSelf && !raftContext.hasFollowers()) {
            sender.tell(new RemoveServerReply(ServerChangeStatus.NOT_SUPPORTED, raftActor.getLeaderId()),
                    raftActor.self());
        } else if (!isSelf && !raftContext.getPeerIds().contains(removeServer.getServerId())) {
            sender.tell(new RemoveServerReply(ServerChangeStatus.DOES_NOT_EXIST, raftActor.getLeaderId()),
                    raftActor.self());
        } else {
            String serverAddress = isSelf ? raftActor.self().path().toString() :
                raftContext.getPeerAddress(removeServer.getServerId());
            onNewOperation(new RemoveServerContext(removeServer, serverAddress, sender));
        }
    }

    private boolean onApplyState(final ApplyState applyState) {
        Payload data = applyState.getReplicatedLogEntry().getData();
        if (data instanceof ClusterConfig) {
            currentOperationState.onApplyState(applyState);
            return true;
        }

        return false;
    }

    /**
     * Add a server. The algorithm for AddServer is as follows:
     * <ul>
     * <li>Add the new server as a peer.</li>
     * <li>Add the new follower to the leader.</li>
     * <li>If new server should be voting member</li>
     * <ul>
     *     <li>Initialize FollowerState to VOTING_NOT_INITIALIZED.</li>
     *     <li>Initiate install snapshot to the new follower.</li>
     *     <li>When install snapshot complete, mark the follower as VOTING and re-calculate majority vote count.</li>
     * </ul>
     * <li>Persist and replicate ServerConfigurationPayload with the new server list.</li>
     * <li>On replication consensus, respond to caller with OK.</li>
     * </ul>
     * If the install snapshot times out after a period of 2 * election time out
     * <ul>
     *     <li>Remove the new server as a peer.</li>
     *     <li>Remove the new follower from the leader.</li>
     *     <li>Respond to caller with TIMEOUT.</li>
     * </ul>
     */
    private void onAddServer(final AddServer addServer, final ActorRef sender) {
        LOG.debug("{}: onAddServer: {}, state: {}", memberId(), addServer, currentOperationState);

        onNewOperation(new AddServerContext(addServer, sender));
    }

    private void onNewOperation(final ServerOperationContext<?> operationContext) {
        if (raftActor.isLeader()) {
            currentOperationState.onNewOperation(operationContext);
        } else {
            ActorSelection leader = raftActor.getLeader();
            if (leader != null) {
                LOG.debug("{}: Not leader - forwarding to leader {}", memberId(), leader);
                leader.tell(operationContext.getOperation(), operationContext.getClientRequestor());
            } else {
                LOG.debug("{}: No leader - returning NO_LEADER reply", memberId());
                operationContext.getClientRequestor().tell(operationContext.newReply(
                        ServerChangeStatus.NO_LEADER, null), raftActor.self());
            }
        }
    }

    /**
     * Interface for the initial state for a server operation.
     */
    private interface InitialOperationState {
        void initiate();
    }

    /**
     * Abstract base class for a server operation FSM state. Handles common behavior for all states.
     */
    private abstract class OperationState {
        void onNewOperation(final ServerOperationContext<?> operationContext) {
            // We're currently processing another operation so queue it to be processed later.

            LOG.debug("{}: Server operation already in progress - queueing {}", memberId(),
                    operationContext.getOperation());

            pendingOperationsQueue.add(operationContext);
        }

        void onServerOperationTimeout(final ServerOperationTimeout timeout) {
            LOG.debug("onServerOperationTimeout should not be called in state {}", this);
        }

        void onUnInitializedFollowerSnapshotReply(final UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("onUnInitializedFollowerSnapshotReply was called in state {}", this);
        }

        void onApplyState(final ApplyState applyState) {
            LOG.debug("onApplyState was called in state {}", this);
        }

        void onSnapshotComplete() {

        }

        void onNewLeader(final String newLeader) {
        }

        protected void persistNewServerConfiguration(final ServerOperationContext<?> operationContext) {
            raftContext.setDynamicServerConfigurationInUse();

            ClusterConfig payload = raftContext.getPeerServerInfo(
                    operationContext.includeSelfInNewConfiguration(raftActor));
            LOG.debug("{}: New server configuration : {}", memberId(), payload.serverInfo());

            raftActor.persistData(operationContext.getClientRequestor(), operationContext.getContextId(),
                    payload, false);

            currentOperationState = new Persisting(operationContext, newTimer(new ServerOperationTimeout(
                    operationContext.getLoggingContext())));

            sendReply(operationContext, ServerChangeStatus.OK);
        }

        protected void operationComplete(final ServerOperationContext<?> operationContext,
                final @Nullable ServerChangeStatus replyStatus) {
            if (replyStatus != null) {
                sendReply(operationContext, replyStatus);
            }

            operationContext.operationComplete(raftActor, replyStatus == null || replyStatus == ServerChangeStatus.OK);

            changeToIdleState();
        }

        protected void changeToIdleState() {
            currentOperationState = IDLE;

            ServerOperationContext<?> nextOperation = pendingOperationsQueue.poll();
            if (nextOperation != null) {
                RaftActorServerConfigurationSupport.this.onNewOperation(nextOperation);
            }
        }

        protected void sendReply(final ServerOperationContext<?> operationContext, final ServerChangeStatus status) {
            LOG.debug("{}: Returning {} for operation {}", memberId(), status, operationContext.getOperation());

            operationContext.getClientRequestor().tell(operationContext.newReply(status, raftActor.getLeaderId()),
                    raftActor.self());
        }

        Cancellable newTimer(final Object message) {
            return newTimer(raftContext.getConfigParams().getElectionTimeOutInterval().multipliedBy(2), message);
        }

        Cancellable newTimer(final Duration timeout, final Object message) {
            final var actorSystem = raftContext.getActorSystem();
            final var actor = raftContext.getActor();

            return actorSystem.scheduler().scheduleOnce(timeout, actor, message, actorSystem.dispatcher(), actor);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * The state when no server operation is in progress. It immediately initiates new server operations.
     */
    private final class Idle extends OperationState {
        @Override
        public void onNewOperation(final ServerOperationContext<?> operationContext) {
            operationContext.newInitialOperationState(RaftActorServerConfigurationSupport.this).initiate();
        }

        @Override
        public void onApplyState(final ApplyState applyState) {
            // Noop - we override b/c ApplyState is called normally for followers in the idle state.
        }
    }

    /**
     * The state when a new server configuration is being persisted and replicated.
     */
    private final class Persisting extends OperationState {
        private final ServerOperationContext<?> operationContext;
        private final Cancellable timer;
        private boolean timedOut = false;

        Persisting(final ServerOperationContext<?> operationContext, final Cancellable timer) {
            this.operationContext = operationContext;
            this.timer = timer;
        }

        @Override
        public void onApplyState(final ApplyState applyState) {
            // Sanity check - we could get an ApplyState from a previous operation that timed out so make
            // sure it's meant for us.
            if (operationContext.getContextId().equals(applyState.getIdentifier())) {
                LOG.info("{}: {} has been successfully replicated to a majority of followers", memberId(),
                        applyState.getReplicatedLogEntry().getData());

                timer.cancel();
                operationComplete(operationContext, null);
            }
        }

        @Override
        public void onServerOperationTimeout(final ServerOperationTimeout timeout) {
            LOG.warn("{}: Timeout occured while replicating the new server configuration for {}", memberId(),
                    timeout.getLoggingContext());

            timedOut = true;

            // Fail any pending operations
            ServerOperationContext<?> nextOperation = pendingOperationsQueue.poll();
            while (nextOperation != null) {
                sendReply(nextOperation, ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT);
                nextOperation = pendingOperationsQueue.poll();
            }
        }

        @Override
        public void onNewOperation(final ServerOperationContext<?> newOperationContext) {
            if (timedOut) {
                sendReply(newOperationContext, ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT);
            } else {
                super.onNewOperation(newOperationContext);
            }
        }
    }

    /**
     * Abstract base class for an AddServer operation state.
     */
    private abstract class AddServerState extends OperationState {
        private final AddServerContext addServerContext;

        AddServerState(final AddServerContext addServerContext) {
            this.addServerContext = addServerContext;
        }

        AddServerContext getAddServerContext() {
            return addServerContext;
        }

        Cancellable newInstallSnapshotTimer() {
            return newTimer(new ServerOperationTimeout(addServerContext.getOperation().getNewServerId()));
        }

        void handleInstallSnapshotTimeout(final ServerOperationTimeout timeout) {
            String serverId = timeout.getLoggingContext();

            LOG.debug("{}: handleInstallSnapshotTimeout for new server {}", memberId(), serverId);

            // cleanup
            raftContext.removePeer(serverId);

            boolean isLeader = raftActor.isLeader();
            if (isLeader) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                leader.removeFollower(serverId);
            }

            operationComplete(getAddServerContext(), isLeader ? ServerChangeStatus.TIMEOUT
                    : ServerChangeStatus.NO_LEADER);
        }
    }

    /**
     * The initial state for the AddServer operation. It adds the new follower as a peer and initiates
     * snapshot capture, if necessary.
     */
    private final class InitialAddServerState extends AddServerState implements InitialOperationState {
        InitialAddServerState(final AddServerContext addServerContext) {
            super(addServerContext);
        }

        @Override
        public void initiate() {
            final AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
            AddServer addServer = getAddServerContext().getOperation();

            LOG.debug("{}: Initiating {}", memberId(), addServer);

            if (raftContext.getPeerInfo(addServer.getNewServerId()) != null) {
                operationComplete(getAddServerContext(), ServerChangeStatus.ALREADY_EXISTS);
                return;
            }

            VotingState votingState = addServer.isVotingMember() ? VotingState.VOTING_NOT_INITIALIZED :
                    VotingState.NON_VOTING;
            raftContext.addToPeers(addServer.getNewServerId(), addServer.getNewServerAddress(), votingState);

            leader.addFollower(addServer.getNewServerId());

            if (votingState == VotingState.VOTING_NOT_INITIALIZED) {
                // schedule the install snapshot timeout timer
                final var installSnapshotTimer = newInstallSnapshotTimer();

                final var newServerId = addServer.getNewServerId();
                if (leader.initiateCaptureSnapshot(newServerId)) {
                    LOG.debug("{}: Initiating capture snapshot for new server {}", memberId(), newServerId);

                    currentOperationState = new InstallingSnapshot(getAddServerContext(), installSnapshotTimer);
                } else {
                    LOG.debug("{}: Snapshot already in progress - waiting for completion", memberId());

                    currentOperationState = new WaitingForPriorSnapshotComplete(getAddServerContext(),
                            installSnapshotTimer);
                }
            } else {
                LOG.debug("{}: New follower is non-voting - directly persisting new server configuration", memberId());

                persistNewServerConfiguration(getAddServerContext());
            }
        }
    }

    /**
     * The AddServer operation state for when the catch-up snapshot is being installed. It handles successful
     * reply or timeout.
     */
    private final class InstallingSnapshot extends AddServerState {
        private final Cancellable installSnapshotTimer;

        InstallingSnapshot(final AddServerContext addServerContext, final Cancellable installSnapshotTimer) {
            super(addServerContext);
            this.installSnapshotTimer = requireNonNull(installSnapshotTimer);
        }

        @Override
        public void onServerOperationTimeout(final ServerOperationTimeout timeout) {
            handleInstallSnapshotTimeout(timeout);

            LOG.warn("{}: Timeout occured for new server {} while installing snapshot", memberId(),
                    timeout.getLoggingContext());
        }

        @Override
        public void onUnInitializedFollowerSnapshotReply(final UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("{}: onUnInitializedFollowerSnapshotReply: {}", memberId(), reply);

            String followerId = reply.getFollowerId();

            // Sanity check to guard against receiving an UnInitializedFollowerSnapshotReply from a prior
            // add server operation that timed out.
            if (getAddServerContext().getOperation().getNewServerId().equals(followerId) && raftActor.isLeader()) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                raftContext.getPeerInfo(followerId).setVotingState(VotingState.VOTING);
                leader.updateMinReplicaCount();

                persistNewServerConfiguration(getAddServerContext());

                installSnapshotTimer.cancel();
            } else {
                LOG.debug("{}: Dropping UnInitializedFollowerSnapshotReply for server {}: {}", memberId(), followerId,
                    !raftActor.isLeader() ? "not leader" : "server Id doesn't match");
            }
        }
    }

    /**
     * The AddServer operation state for when there is a snapshot already in progress. When the current
     * snapshot completes, it initiates an install snapshot.
     */
    private final class WaitingForPriorSnapshotComplete extends AddServerState {
        private final Cancellable snapshotTimer;

        WaitingForPriorSnapshotComplete(final AddServerContext addServerContext, final Cancellable snapshotTimer) {
            super(addServerContext);
            this.snapshotTimer = requireNonNull(snapshotTimer);
        }

        @Override
        public void onSnapshotComplete() {
            LOG.debug("{}: onSnapshotComplete", memberId());

            if (!raftActor.isLeader()) {
                LOG.debug("{}: No longer the leader", memberId());
                return;
            }

            final var leader = (AbstractLeader) raftActor.getCurrentBehavior();
            if (leader.initiateCaptureSnapshot(getAddServerContext().getOperation().getNewServerId())) {
                LOG.debug("{}: Initiating capture snapshot for new server {}", memberId(),
                        getAddServerContext().getOperation().getNewServerId());

                currentOperationState = new InstallingSnapshot(getAddServerContext(),
                        newInstallSnapshotTimer());

                snapshotTimer.cancel();
            }
        }

        @Override
        public void onServerOperationTimeout(final ServerOperationTimeout timeout) {
            handleInstallSnapshotTimeout(timeout);

            LOG.warn("{}: Timeout occured for new server {} while waiting for prior snapshot to complete", memberId(),
                timeout.getLoggingContext());
        }
    }

    private static final class ServerOperationContextIdentifier
            extends AbstractUUIDIdentifier<ServerOperationContextIdentifier> {
        private static final long serialVersionUID = 1L;

        ServerOperationContextIdentifier() {
            super(UUID.randomUUID());
        }
    }

    /**
     * Stores context information for a server operation.
     *
     * @param <T> the operation type
     */
    private abstract static sealed class ServerOperationContext<T extends ServerChangeRequest<?>> {
        private final T operation;
        private final ActorRef clientRequestor;
        private final Identifier contextId;

        ServerOperationContext(final T operation, final ActorRef clientRequestor) {
            this.operation = operation;
            this.clientRequestor = clientRequestor;
            contextId = new ServerOperationContextIdentifier();
        }

        final Identifier getContextId() {
            return contextId;
        }

        final T getOperation() {
            return operation;
        }

        final ActorRef getClientRequestor() {
            return clientRequestor;
        }

        void operationComplete(final RaftActor raftActor, final boolean succeeded) {
            // No-op by default
        }

        boolean includeSelfInNewConfiguration(final RaftActor raftActor) {
            return true;
        }

        abstract Object newReply(ServerChangeStatus status, String leaderId);

        abstract InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support);

        abstract String getLoggingContext();
    }

    /**
     * Stores context information for an AddServer operation.
     */
    private static final class AddServerContext extends ServerOperationContext<AddServer> {
        AddServerContext(final AddServer addServer, final ActorRef clientRequestor) {
            super(addServer, clientRequestor);
        }

        @Override
        Object newReply(final ServerChangeStatus status, final String leaderId) {
            return new AddServerReply(status, leaderId);
        }

        @Override
        InitialOperationState newInitialOperationState(final RaftActorServerConfigurationSupport support) {
            return support.new InitialAddServerState(this);
        }

        @Override
        String getLoggingContext() {
            return getOperation().getNewServerId();
        }
    }

    private abstract class RemoveServerState extends OperationState {
        private final RemoveServerContext removeServerContext;

        protected RemoveServerState(final RemoveServerContext removeServerContext) {
            this.removeServerContext = requireNonNull(removeServerContext);

        }

        public RemoveServerContext getRemoveServerContext() {
            return removeServerContext;
        }
    }

    private final class InitialRemoveServerState extends RemoveServerState implements InitialOperationState {

        protected InitialRemoveServerState(final RemoveServerContext removeServerContext) {
            super(removeServerContext);
        }

        @Override
        public void initiate() {
            String serverId = getRemoveServerContext().getOperation().getServerId();
            raftContext.removePeer(serverId);
            AbstractLeader leader = (AbstractLeader)raftActor.getCurrentBehavior();
            leader.removeFollower(serverId);
            leader.updateMinReplicaCount();

            persistNewServerConfiguration(getRemoveServerContext());
        }
    }

    private static final class RemoveServerContext extends ServerOperationContext<RemoveServer> {
        private final String peerAddress;

        RemoveServerContext(final RemoveServer operation, final String peerAddress, final ActorRef clientRequestor) {
            super(operation, clientRequestor);
            this.peerAddress = peerAddress;
        }

        @Override
        Object newReply(final ServerChangeStatus status, final String leaderId) {
            return new RemoveServerReply(status, leaderId);
        }

        @Override
        InitialOperationState newInitialOperationState(final RaftActorServerConfigurationSupport support) {
            return support.new InitialRemoveServerState(this);
        }

        @Override
        void operationComplete(final RaftActor raftActor, final boolean succeeded) {
            if (peerAddress != null) {
                raftActor.context().actorSelection(peerAddress).tell(
                        new ServerRemoved(getOperation().getServerId()), raftActor.self());
            }
        }

        @Override
        boolean includeSelfInNewConfiguration(final RaftActor raftActor) {
            return !getOperation().getServerId().equals(raftActor.memberId());
        }

        @Override
        String getLoggingContext() {
            return getOperation().getServerId();
        }
    }

    private static final class ChangeServersVotingStatusContext extends
            ServerOperationContext<ChangeServersVotingStatus> {
        private final boolean tryToElectLeader;

        ChangeServersVotingStatusContext(final ChangeServersVotingStatus convertMessage, final ActorRef clientRequestor,
                final boolean tryToElectLeader) {
            super(convertMessage, clientRequestor);
            this.tryToElectLeader = tryToElectLeader;
        }

        @Override
        InitialOperationState newInitialOperationState(final RaftActorServerConfigurationSupport support) {
            return support.new ChangeServersVotingStatusState(this, tryToElectLeader);
        }

        @Override
        Object newReply(final ServerChangeStatus status, final String leaderId) {
            return new ServerChangeReply(status, leaderId);
        }

        @Override
        void operationComplete(final RaftActor raftActor, final boolean succeeded) {
            // If this leader changed to non-voting we need to step down as leader so we'll try to transfer
            // leadership.
            boolean localServerChangedToNonVoting = Boolean.FALSE.equals(getOperation()
                    .getServerVotingStatusMap().get(raftActor.memberId()));
            if (succeeded && localServerChangedToNonVoting) {
                LOG.debug("Leader changed to non-voting - trying leadership transfer");
                raftActor.becomeNonVoting();
            } else if (raftActor.isLeader()) {
                raftActor.onVotingStateChangeComplete();
            }
        }

        @Override
        String getLoggingContext() {
            return getOperation().toString();
        }
    }

    private class ChangeServersVotingStatusState extends OperationState implements InitialOperationState {
        private final ChangeServersVotingStatusContext changeVotingStatusContext;
        private final boolean tryToElectLeader;

        ChangeServersVotingStatusState(final ChangeServersVotingStatusContext changeVotingStatusContext,
                final boolean tryToElectLeader) {
            this.changeVotingStatusContext = changeVotingStatusContext;
            this.tryToElectLeader = tryToElectLeader;
        }

        @Override
        public void initiate() {
            LOG.debug("Initiating ChangeServersVotingStatusState");

            if (tryToElectLeader) {
                initiateLocalLeaderElection();
            } else if (updateLocalPeerInfo()) {
                persistNewServerConfiguration(changeVotingStatusContext);
            }
        }

        private void initiateLocalLeaderElection() {
            LOG.debug("{}: Sending local ElectionTimeout to start leader election", memberId());

            final var previousServerConfig = raftContext.getPeerServerInfo(true);
            if (!updateLocalPeerInfo()) {
                return;
            }

            raftContext.getActor().tell(TimeoutNow.INSTANCE, raftContext.getActor());

            currentOperationState = new WaitingForLeaderElected(changeVotingStatusContext, previousServerConfig);
        }

        private boolean updateLocalPeerInfo() {
            final var newServerInfoList = newServerInfoList();

            // Check if new voting state would leave us with no voting members.
            boolean atLeastOneVoting = false;
            for (ServerInfo info: newServerInfoList) {
                if (info.isVoting()) {
                    atLeastOneVoting = true;
                    break;
                }
            }

            if (!atLeastOneVoting) {
                operationComplete(changeVotingStatusContext, ServerChangeStatus.INVALID_REQUEST);
                return false;
            }

            raftContext.updatePeerIds(new ClusterConfig(newServerInfoList));
            if (raftActor.getCurrentBehavior() instanceof AbstractLeader leader) {
                leader.updateMinReplicaCount();
            }

            return true;
        }

        private ImmutableList<ServerInfo> newServerInfoList() {
            final var serverVotingStatusMap = changeVotingStatusContext.getOperation().getServerVotingStatusMap();
            final var peerInfos = raftContext.getPeers();
            final var newServerInfoList = ImmutableList.<ServerInfo>builderWithExpectedSize(peerInfos.size() + 1);
            for (var peerInfo : peerInfos) {
                final var peerId = peerInfo.getId();
                final var voting = serverVotingStatusMap.get(peerId);
                newServerInfoList.add(new ServerInfo(peerId, voting != null ? voting : peerInfo.isVoting()));
            }

            final var myId = memberId();
            final var myVoting = serverVotingStatusMap.get(myId);
            newServerInfoList.add(new ServerInfo(myId, myVoting != null ? myVoting : raftContext.isVotingMember()));

            return newServerInfoList.build();
        }
    }

    private class WaitingForLeaderElected extends OperationState {
        private final ClusterConfig previousServerConfig;
        private final ChangeServersVotingStatusContext operationContext;
        private final Cancellable timer;

        WaitingForLeaderElected(final ChangeServersVotingStatusContext operationContext,
                final ClusterConfig previousServerConfig) {
            this.operationContext = operationContext;
            this.previousServerConfig = previousServerConfig;

            timer = newTimer(raftContext.getConfigParams().getElectionTimeOutInterval(),
                    new ServerOperationTimeout(operationContext.getLoggingContext()));
        }

        @Override
        void onNewLeader(final String newLeader) {
            if (newLeader == null) {
                return;
            }

            LOG.debug("{}: New leader {} elected", memberId(), newLeader);

            timer.cancel();

            if (raftActor.isLeader()) {
                persistNewServerConfiguration(operationContext);
            } else {
                // Edge case - some other node became leader so forward the operation.
                LOG.debug("{}: Forwarding {} to new leader", memberId(), operationContext.getOperation());

                // Revert the local server config change.
                raftContext.updatePeerIds(previousServerConfig);

                changeToIdleState();
                RaftActorServerConfigurationSupport.this.onNewOperation(operationContext);
            }
        }

        @Override
        void onServerOperationTimeout(final ServerOperationTimeout timeout) {
            LOG.warn("{}: Leader election timed out - cannot apply operation {}", memberId(),
                timeout.getLoggingContext());

            // Revert the local server config change.
            raftContext.updatePeerIds(previousServerConfig);
            raftActor.initializeBehavior();

            tryToForwardOperationToAnotherServer();
        }

        private void tryToForwardOperationToAnotherServer() {
            final var serversVisited = new HashSet<>(operationContext.getOperation().getServersVisited());
            LOG.debug("{}: tryToForwardOperationToAnotherServer - servers already visited {}", memberId(),
                    serversVisited);

            serversVisited.add(memberId());

            // Try to find another whose state is being changed from non-voting to voting and that we haven't
            // tried yet.
            Map<String, Boolean> serverVotingStatusMap = operationContext.getOperation().getServerVotingStatusMap();
            ActorSelection forwardToPeerActor = null;
            for (Map.Entry<String, Boolean> e: serverVotingStatusMap.entrySet()) {
                Boolean isVoting = e.getValue();
                String serverId = e.getKey();
                PeerInfo peerInfo = raftContext.getPeerInfo(serverId);
                if (isVoting && peerInfo != null && !peerInfo.isVoting() && !serversVisited.contains(serverId)) {
                    ActorSelection actor = raftContext.getPeerActorSelection(serverId);
                    if (actor != null) {
                        forwardToPeerActor = actor;
                        break;
                    }
                }
            }

            if (forwardToPeerActor != null) {
                LOG.debug("{}: Found server {} to forward to", memberId(), forwardToPeerActor);

                forwardToPeerActor.tell(new ChangeServersVotingStatus(serverVotingStatusMap, serversVisited),
                        operationContext.getClientRequestor());
                changeToIdleState();
            } else {
                operationComplete(operationContext, ServerChangeStatus.NO_LEADER);
            }
        }
    }

    static class ServerOperationTimeout {
        private final String loggingContext;

        ServerOperationTimeout(final String loggingContext) {
            this.loggingContext = requireNonNull(loggingContext, "loggingContext should not be null");
        }

        String getLoggingContext() {
            return loggingContext;
        }
    }
}
