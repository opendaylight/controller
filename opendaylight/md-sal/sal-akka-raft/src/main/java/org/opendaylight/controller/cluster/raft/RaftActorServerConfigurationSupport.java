/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ChangeServersVotingStatus;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.messages.UnInitializedFollowerSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles server configuration related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorServerConfigurationSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorServerConfigurationSupport.class);

    private final OperationState IDLE = new Idle();

    private final RaftActor raftActor;

    private final RaftActorContext raftContext;

    private final Queue<ServerOperationContext<?>> pendingOperationsQueue = new ArrayDeque<>();

    private OperationState currentOperationState = IDLE;

    RaftActorServerConfigurationSupport(RaftActor raftActor) {
        this.raftActor = raftActor;
        this.raftContext = raftActor.getRaftActorContext();
    }

    boolean handleMessage(Object message, ActorRef sender) {
        if(message instanceof AddServer) {
            onAddServer((AddServer) message, sender);
            return true;
        } else if(message instanceof RemoveServer) {
            onRemoveServer((RemoveServer) message, sender);
            return true;
        } else if(message instanceof ChangeServersVotingStatus) {
            onChangeServersVotingStatus((ChangeServersVotingStatus) message, sender);
            return true;
        } else if (message instanceof ServerOperationTimeout) {
            currentOperationState.onServerOperationTimeout((ServerOperationTimeout) message);
            return true;
        } else if (message instanceof UnInitializedFollowerSnapshotReply) {
            currentOperationState.onUnInitializedFollowerSnapshotReply((UnInitializedFollowerSnapshotReply) message);
            return true;
        } else if(message instanceof ApplyState) {
            return onApplyState((ApplyState) message);
        } else if(message instanceof SnapshotComplete) {
            currentOperationState.onSnapshotComplete();
            return false;
        } else {
            return false;
        }
    }

    void onNewLeader(String leaderId) {
        currentOperationState.onNewLeader(leaderId);
    }

    private void onChangeServersVotingStatus(ChangeServersVotingStatus message, ActorRef sender) {
        LOG.debug("{}: onChangeServersVotingStatus: {}, state: {}", raftContext.getId(), message,
                currentOperationState);

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
        boolean localServerChangingToVoting = Boolean.TRUE.equals(message.
                getServerVotingStatusMap().get(raftActor.getRaftActorContext().getId()));
        boolean hasNoLeader = raftActor.getLeaderId() == null;
        if(localServerChangingToVoting && !raftContext.isVotingMember() && hasNoLeader) {
            currentOperationState.onNewOperation(new ChangeServersVotingStatusContext(message, sender, true));
        } else {
            onNewOperation(new ChangeServersVotingStatusContext(message, sender, false));
        }
    }

    private void onRemoveServer(RemoveServer removeServer, ActorRef sender) {
        LOG.debug("{}: onRemoveServer: {}, state: {}", raftContext.getId(), removeServer, currentOperationState);
        boolean isSelf = removeServer.getServerId().equals(raftContext.getId());
        if(isSelf && !raftContext.hasFollowers()) {
            sender.tell(new RemoveServerReply(ServerChangeStatus.NOT_SUPPORTED, raftActor.getLeaderId()),
                    raftActor.getSelf());
        } else if(!isSelf && !raftContext.getPeerIds().contains(removeServer.getServerId())) {
            sender.tell(new RemoveServerReply(ServerChangeStatus.DOES_NOT_EXIST, raftActor.getLeaderId()),
                    raftActor.getSelf());
        } else {
            String serverAddress = isSelf ? raftActor.self().path().toString() :
                raftContext.getPeerAddress(removeServer.getServerId());
            onNewOperation(new RemoveServerContext(removeServer, serverAddress, sender));
        }
    }

    private boolean onApplyState(ApplyState applyState) {
        Payload data = applyState.getReplicatedLogEntry().getData();
        if(data instanceof ServerConfigurationPayload) {
            currentOperationState.onApplyState(applyState);
            return true;
        }

        return false;
    }

    /**
     * The algorithm for AddServer is as follows:
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
    private void onAddServer(AddServer addServer, ActorRef sender) {
        LOG.debug("{}: onAddServer: {}, state: {}", raftContext.getId(), addServer, currentOperationState);

        onNewOperation(new AddServerContext(addServer, sender));
    }

    private void onNewOperation(ServerOperationContext<?> operationContext) {
        if (raftActor.isLeader()) {
            currentOperationState.onNewOperation(operationContext);
        } else {
            ActorSelection leader = raftActor.getLeader();
            if (leader != null) {
                LOG.debug("{}: Not leader - forwarding to leader {}", raftContext.getId(), leader);
                leader.tell(operationContext.getOperation(), operationContext.getClientRequestor());
            } else {
                LOG.debug("{}: No leader - returning NO_LEADER reply", raftContext.getId());
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
        void onNewOperation(ServerOperationContext<?> operationContext) {
            // We're currently processing another operation so queue it to be processed later.

            LOG.debug("{}: Server operation already in progress - queueing {}", raftContext.getId(),
                    operationContext.getOperation());

            pendingOperationsQueue.add(operationContext);
        }

        void onServerOperationTimeout(ServerOperationTimeout timeout) {
            LOG.debug("onServerOperationTimeout should not be called in state {}", this);
        }

        void onUnInitializedFollowerSnapshotReply(UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("onUnInitializedFollowerSnapshotReply was called in state {}", this);
        }

        void onApplyState(ApplyState applyState) {
            LOG.debug("onApplyState was called in state {}", this);
        }

        void onSnapshotComplete() {

        }

        void onNewLeader(String newLeader) {
        }

        protected void persistNewServerConfiguration(ServerOperationContext<?> operationContext){
            raftContext.setDynamicServerConfigurationInUse();

            ServerConfigurationPayload payload = raftContext.getPeerServerInfo(
                    operationContext.includeSelfInNewConfiguration(raftActor));
            LOG.debug("{}: New server configuration : {}", raftContext.getId(), payload.getServerConfig());

            raftActor.persistData(operationContext.getClientRequestor(), operationContext.getContextId(), payload);

            currentOperationState = new Persisting(operationContext, newTimer(new ServerOperationTimeout(
                    operationContext.getLoggingContext())));

            sendReply(operationContext, ServerChangeStatus.OK);
        }

        protected void operationComplete(ServerOperationContext<?> operationContext, @Nullable ServerChangeStatus replyStatus) {
            if(replyStatus != null) {
                sendReply(operationContext, replyStatus);
            }

            operationContext.operationComplete(raftActor, replyStatus == null || replyStatus == ServerChangeStatus.OK);

            changeToIdleState();
        }

        protected void changeToIdleState() {
            currentOperationState = IDLE;

            ServerOperationContext<?> nextOperation = pendingOperationsQueue.poll();
            if(nextOperation != null) {
                RaftActorServerConfigurationSupport.this.onNewOperation(nextOperation);
            }
        }

        protected void sendReply(ServerOperationContext<?> operationContext, ServerChangeStatus status) {
            LOG.debug("{}: Returning {} for operation {}", raftContext.getId(), status, operationContext.getOperation());

            operationContext.getClientRequestor().tell(operationContext.newReply(status, raftActor.getLeaderId()),
                    raftActor.self());
        }

        Cancellable newTimer(Object message) {
            return newTimer(raftContext.getConfigParams().getElectionTimeOutInterval().$times(2), message);
        }

        Cancellable newTimer(FiniteDuration timeout, Object message) {
            return raftContext.getActorSystem().scheduler().scheduleOnce(
                    timeout, raftContext.getActor(), message,
                            raftContext.getActorSystem().dispatcher(), raftContext.getActor());
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
        public void onNewOperation(ServerOperationContext<?> operationContext) {
            operationContext.newInitialOperationState(RaftActorServerConfigurationSupport.this).initiate();
        }

        @Override
        public void onApplyState(ApplyState applyState) {
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

        Persisting(ServerOperationContext<?> operationContext, Cancellable timer) {
            this.operationContext = operationContext;
            this.timer = timer;
        }

        @Override
        public void onApplyState(ApplyState applyState) {
            // Sanity check - we could get an ApplyState from a previous operation that timed out so make
            // sure it's meant for us.
            if(operationContext.getContextId().equals(applyState.getIdentifier())) {
                LOG.info("{}: {} has been successfully replicated to a majority of followers", raftContext.getId(),
                        applyState.getReplicatedLogEntry().getData());

                timer.cancel();
                operationComplete(operationContext, null);
            }
        }

        @Override
        public void onServerOperationTimeout(ServerOperationTimeout timeout) {
            LOG.warn("{}: Timeout occured while replicating the new server configuration for {}", raftContext.getId(),
                    timeout.getLoggingContext());

            timedOut = true;

            // Fail any pending operations
            ServerOperationContext<?> nextOperation = pendingOperationsQueue.poll();
            while(nextOperation != null) {
                sendReply(nextOperation, ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT);
                nextOperation = pendingOperationsQueue.poll();
            }
        }

        @Override
        public void onNewOperation(ServerOperationContext<?> operationContext) {
            if(timedOut) {
                sendReply(operationContext, ServerChangeStatus.PRIOR_REQUEST_CONSENSUS_TIMEOUT);
            } else {
                super.onNewOperation(operationContext);
            }
        }
    }

    /**
     * Abstract base class for an AddServer operation state.
     */
    private abstract class AddServerState extends OperationState {
        private final AddServerContext addServerContext;

        AddServerState(AddServerContext addServerContext) {
            this.addServerContext = addServerContext;
        }

        AddServerContext getAddServerContext() {
            return addServerContext;
        }

        Cancellable newInstallSnapshotTimer() {
            return newTimer(new ServerOperationTimeout(addServerContext.getOperation().getNewServerId()));
        }

        void handleInstallSnapshotTimeout(ServerOperationTimeout timeout) {
            String serverId = timeout.getLoggingContext();

            LOG.debug("{}: handleInstallSnapshotTimeout for new server {}", raftContext.getId(), serverId);

            // cleanup
            raftContext.removePeer(serverId);

            boolean isLeader = raftActor.isLeader();
            if(isLeader) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                leader.removeFollower(serverId);
            }

            operationComplete(getAddServerContext(), isLeader ? ServerChangeStatus.TIMEOUT : ServerChangeStatus.NO_LEADER);
        }

    }

    /**
     * The initial state for the AddServer operation. It adds the new follower as a peer and initiates
     * snapshot capture, if necessary.
     */
    private final class InitialAddServerState extends AddServerState implements InitialOperationState {
        InitialAddServerState(AddServerContext addServerContext) {
            super(addServerContext);
        }

        @Override
        public void initiate() {
            AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
            AddServer addServer = getAddServerContext().getOperation();

            LOG.debug("{}: Initiating {}", raftContext.getId(), addServer);

            if(raftContext.getPeerInfo(addServer.getNewServerId()) != null) {
                operationComplete(getAddServerContext(), ServerChangeStatus.ALREADY_EXISTS);
                return;
            }

            VotingState votingState = addServer.isVotingMember() ? VotingState.VOTING_NOT_INITIALIZED :
                    VotingState.NON_VOTING;
            raftContext.addToPeers(addServer.getNewServerId(), addServer.getNewServerAddress(), votingState);

            leader.addFollower(addServer.getNewServerId());

            if(votingState == VotingState.VOTING_NOT_INITIALIZED){
                // schedule the install snapshot timeout timer
                Cancellable installSnapshotTimer = newInstallSnapshotTimer();
                if(leader.initiateCaptureSnapshot(addServer.getNewServerId())) {
                    LOG.debug("{}: Initiating capture snapshot for new server {}", raftContext.getId(),
                            addServer.getNewServerId());

                    currentOperationState = new InstallingSnapshot(getAddServerContext(), installSnapshotTimer);
                } else {
                    LOG.debug("{}: Snapshot already in progress - waiting for completion", raftContext.getId());

                    currentOperationState = new WaitingForPriorSnapshotComplete(getAddServerContext(),
                            installSnapshotTimer);
                }
            } else {
                LOG.debug("{}: New follower is non-voting - directly persisting new server configuration",
                        raftContext.getId());

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

        InstallingSnapshot(AddServerContext addServerContext, Cancellable installSnapshotTimer) {
            super(addServerContext);
            this.installSnapshotTimer = Preconditions.checkNotNull(installSnapshotTimer);
        }

        @Override
        public void onServerOperationTimeout(ServerOperationTimeout timeout) {
            handleInstallSnapshotTimeout(timeout);

            LOG.warn("{}: Timeout occured for new server {} while installing snapshot", raftContext.getId(),
                    timeout.getLoggingContext());
        }

        @Override
        public void onUnInitializedFollowerSnapshotReply(UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("{}: onUnInitializedFollowerSnapshotReply: {}", raftContext.getId(), reply);

            String followerId = reply.getFollowerId();

            // Sanity check to guard against receiving an UnInitializedFollowerSnapshotReply from a prior
            // add server operation that timed out.
            if(getAddServerContext().getOperation().getNewServerId().equals(followerId) && raftActor.isLeader()) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                raftContext.getPeerInfo(followerId).setVotingState(VotingState.VOTING);
                leader.updateMinReplicaCount();

                persistNewServerConfiguration(getAddServerContext());

                installSnapshotTimer.cancel();
            } else {
                LOG.debug("{}: Dropping UnInitializedFollowerSnapshotReply for server {}: {}",
                        raftContext.getId(), followerId,
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

        WaitingForPriorSnapshotComplete(AddServerContext addServerContext, Cancellable snapshotTimer) {
            super(addServerContext);
            this.snapshotTimer = Preconditions.checkNotNull(snapshotTimer);
        }

        @Override
        public void onSnapshotComplete() {
            LOG.debug("{}: onSnapshotComplete", raftContext.getId());

            if(!raftActor.isLeader()) {
                LOG.debug("{}: No longer the leader", raftContext.getId());
                return;
            }

            AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
            if(leader.initiateCaptureSnapshot(getAddServerContext().getOperation().getNewServerId())) {
                LOG.debug("{}: Initiating capture snapshot for new server {}", raftContext.getId(),
                        getAddServerContext().getOperation().getNewServerId());

                currentOperationState = new InstallingSnapshot(getAddServerContext(),
                        newInstallSnapshotTimer());

                snapshotTimer.cancel();
            }
        }

        @Override
        public void onServerOperationTimeout(ServerOperationTimeout timeout) {
            handleInstallSnapshotTimeout(timeout);

            LOG.warn("{}: Timeout occured for new server {} while waiting for prior snapshot to complete",
                    raftContext.getId(), timeout.getLoggingContext());
        }
    }

    /**
     * Stores context information for a server operation.
     *
     * @param <T> the operation type
     */
    private static abstract class ServerOperationContext<T> {
        private final T operation;
        private final ActorRef clientRequestor;
        private final String contextId;

        ServerOperationContext(T operation, ActorRef clientRequestor){
            this.operation = operation;
            this.clientRequestor = clientRequestor;
            contextId = UUID.randomUUID().toString();
        }

        String getContextId() {
            return contextId;
        }

        T getOperation() {
            return operation;
        }

        ActorRef getClientRequestor() {
            return clientRequestor;
        }

        void operationComplete(RaftActor raftActor, boolean succeeded) {
        }

        boolean includeSelfInNewConfiguration(RaftActor raftActor) {
            return true;
        }

        abstract Object newReply(ServerChangeStatus status, String leaderId);

        abstract InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support);

        abstract String getLoggingContext();
    }

    /**
     * Stores context information for an AddServer operation.
     */
    private static class AddServerContext extends ServerOperationContext<AddServer> {
        AddServerContext(AddServer addServer, ActorRef clientRequestor) {
            super(addServer, clientRequestor);
        }

        @Override
        Object newReply(ServerChangeStatus status, String leaderId) {
            return new AddServerReply(status, leaderId);
        }

        @Override
        InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support) {
            return support.new InitialAddServerState(this);
        }

        @Override
        String getLoggingContext() {
            return getOperation().getNewServerId();
        }
    }

    private abstract class RemoveServerState extends OperationState {
        private final RemoveServerContext removeServerContext;

        protected RemoveServerState(RemoveServerContext removeServerContext) {
            this.removeServerContext = Preconditions.checkNotNull(removeServerContext);

        }

        public RemoveServerContext getRemoveServerContext() {
            return removeServerContext;
        }
    }

    private final class InitialRemoveServerState extends RemoveServerState implements InitialOperationState{

        protected InitialRemoveServerState(RemoveServerContext removeServerContext) {
            super(removeServerContext);
        }

        @Override
        public void initiate() {
            String serverId = getRemoveServerContext().getOperation().getServerId();
            raftContext.removePeer(serverId);
            ((AbstractLeader)raftActor.getCurrentBehavior()).removeFollower(serverId);

            persistNewServerConfiguration(getRemoveServerContext());
        }
    }

    private static class RemoveServerContext extends ServerOperationContext<RemoveServer> {
        private final String peerAddress;

        RemoveServerContext(RemoveServer operation, String peerAddress, ActorRef clientRequestor) {
            super(operation, clientRequestor);
            this.peerAddress = peerAddress;
        }

        @Override
        Object newReply(ServerChangeStatus status, String leaderId) {
            return new RemoveServerReply(status, leaderId);
        }

        @Override
        InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support) {
            return support.new InitialRemoveServerState(this);
        }

        @Override
        void operationComplete(RaftActor raftActor, boolean succeeded) {
            if(peerAddress != null) {
                raftActor.context().actorSelection(peerAddress).tell(new ServerRemoved(getOperation().getServerId()), raftActor.getSelf());
            }
        }

        @Override
        boolean includeSelfInNewConfiguration(RaftActor raftActor) {
            return !getOperation().getServerId().equals(raftActor.getId());
        }

        @Override
        String getLoggingContext() {
            return getOperation().getServerId();
        }
    }

    private static class ChangeServersVotingStatusContext extends ServerOperationContext<ChangeServersVotingStatus> {
        private final boolean tryToElectLeader;

        ChangeServersVotingStatusContext(ChangeServersVotingStatus convertMessage, ActorRef clientRequestor,
                boolean tryToElectLeader) {
            super(convertMessage, clientRequestor);
            this.tryToElectLeader = tryToElectLeader;
        }

        @Override
        InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support) {
            return support.new ChangeServersVotingStatusState(this, tryToElectLeader);
        }

        @Override
        Object newReply(ServerChangeStatus status, String leaderId) {
            return new ServerChangeReply(status, leaderId);
        }

        @Override
        void operationComplete(final RaftActor raftActor, boolean succeeded) {
            // If this leader changed to non-voting we need to step down as leader so we'll try to transfer
            // leadership.
            boolean localServerChangedToNonVoting = Boolean.FALSE.equals(getOperation().
                    getServerVotingStatusMap().get(raftActor.getRaftActorContext().getId()));
            if(succeeded && localServerChangedToNonVoting && raftActor.isLeader()) {
                raftActor.initiateLeadershipTransfer(new RaftActorLeadershipTransferCohort.OnComplete() {
                    @Override
                    public void onSuccess(ActorRef raftActorRef, ActorRef replyTo) {
                        LOG.debug("{}: leader transfer succeeded after change to non-voting", raftActor.persistenceId());
                        ensureFollowerState(raftActor);
                    }

                    @Override
                    public void onFailure(ActorRef raftActorRef, ActorRef replyTo) {
                        LOG.debug("{}: leader transfer failed after change to non-voting", raftActor.persistenceId());
                        ensureFollowerState(raftActor);
                    }

                    private void ensureFollowerState(RaftActor raftActor) {
                        // Whether or not leadership transfer succeeded, we have to step down as leader and
                        // switch to Follower so ensure that.
                        if(raftActor.getRaftState() != RaftState.Follower) {
                            raftActor.initializeBehavior();
                        }
                    }
                });
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

        ChangeServersVotingStatusState(ChangeServersVotingStatusContext changeVotingStatusContext,
                boolean tryToElectLeader) {
            this.changeVotingStatusContext = changeVotingStatusContext;
            this.tryToElectLeader = tryToElectLeader;
        }

        @Override
        public void initiate() {
            LOG.debug("Initiating ChangeServersVotingStatusState");

            if(tryToElectLeader) {
                initiateLocalLeaderElection();
            } else {
                updateLocalPeerInfo();

                persistNewServerConfiguration(changeVotingStatusContext);
            }
        }

        private void initiateLocalLeaderElection() {
            LOG.debug("{}: Sending local ElectionTimeout to start leader election", raftContext.getId());

            ServerConfigurationPayload previousServerConfig = raftContext.getPeerServerInfo(true);
            updateLocalPeerInfo();

            raftContext.getActor().tell(ElectionTimeout.INSTANCE, raftContext.getActor());

            currentOperationState = new WaitingForLeaderElected(changeVotingStatusContext, previousServerConfig);
        }

        private void updateLocalPeerInfo() {
            List<ServerInfo> newServerInfoList = newServerInfoList();

            raftContext.updatePeerIds(new ServerConfigurationPayload(newServerInfoList));
            if(raftActor.getCurrentBehavior() instanceof AbstractLeader) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                leader.updateMinReplicaCount();
            }
        }

        private List<ServerInfo> newServerInfoList() {
            Map<String, Boolean> serverVotingStatusMap = changeVotingStatusContext.getOperation().getServerVotingStatusMap();
            List<ServerInfo> newServerInfoList = new ArrayList<>();
            for(String peerId: raftContext.getPeerIds()) {
                newServerInfoList.add(new ServerInfo(peerId, serverVotingStatusMap.containsKey(peerId) ?
                        serverVotingStatusMap.get(peerId) : raftContext.getPeerInfo(peerId).isVoting()));
            }

            newServerInfoList.add(new ServerInfo(raftContext.getId(), serverVotingStatusMap.containsKey(
                    raftContext.getId()) ? serverVotingStatusMap.get(raftContext.getId()) : raftContext.isVotingMember()));

            return newServerInfoList;
        }
    }

    private class WaitingForLeaderElected extends OperationState {
        private final ServerConfigurationPayload previousServerConfig;
        private final ChangeServersVotingStatusContext operationContext;
        private final Cancellable timer;

        WaitingForLeaderElected(ChangeServersVotingStatusContext operationContext,
                ServerConfigurationPayload previousServerConfig) {
            this.operationContext = operationContext;
            this.previousServerConfig = previousServerConfig;

            timer = newTimer(raftContext.getConfigParams().getElectionTimeOutInterval(),
                    new ServerOperationTimeout(operationContext.getLoggingContext()));
        }

        @Override
        void onNewLeader(String newLeader) {
            LOG.debug("{}: New leader {} elected", raftContext.getId(), newLeader);

            timer.cancel();

            if(raftActor.isLeader()) {
                persistNewServerConfiguration(operationContext);
            } else {
                // Edge case - some other node became leader so forward the operation.
                LOG.debug("{}: Forwarding {} to new leader", raftContext.getId(), operationContext.getOperation());

                // Revert the local server config change.
                raftContext.updatePeerIds(previousServerConfig);

                changeToIdleState();
                RaftActorServerConfigurationSupport.this.onNewOperation(operationContext);
            }
        }

        @Override
        void onServerOperationTimeout(ServerOperationTimeout timeout) {
            LOG.warn("{}: Leader election timed out - cannot apply operation {}",
                    raftContext.getId(), timeout.getLoggingContext());

            // Revert the local server config change.
            raftContext.updatePeerIds(previousServerConfig);
            raftActor.initializeBehavior();

            tryToForwardOperationToAnotherServer();
        }

        private void tryToForwardOperationToAnotherServer() {
            Collection<String> serversVisited = new HashSet<>(operationContext.getOperation().getServersVisited());

            LOG.debug("{}: tryToForwardOperationToAnotherServer - servers already visited {}", raftContext.getId(),
                    serversVisited);

            serversVisited.add(raftContext.getId());

            // Try to find another whose state is being changed from non-voting to voting and that we haven't
            // tried yet.
            Map<String, Boolean> serverVotingStatusMap = operationContext.getOperation().getServerVotingStatusMap();
            ActorSelection forwardToPeerActor = null;
            for(Map.Entry<String, Boolean> e: serverVotingStatusMap.entrySet()) {
                Boolean isVoting = e.getValue();
                String serverId = e.getKey();
                PeerInfo peerInfo = raftContext.getPeerInfo(serverId);
                if(isVoting && peerInfo != null && !peerInfo.isVoting() && !serversVisited.contains(serverId)) {
                    ActorSelection actor = raftContext.getPeerActorSelection(serverId);
                    if(actor != null) {
                        forwardToPeerActor = actor;
                        break;
                    }
                }
            }

            if(forwardToPeerActor != null) {
                LOG.debug("{}: Found server {} to forward to", raftContext.getId(), forwardToPeerActor);

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

        ServerOperationTimeout(String loggingContext){
           this.loggingContext = Preconditions.checkNotNull(loggingContext, "loggingContext should not be null");
        }

        String getLoggingContext() {
            return loggingContext;
        }
    }
}
