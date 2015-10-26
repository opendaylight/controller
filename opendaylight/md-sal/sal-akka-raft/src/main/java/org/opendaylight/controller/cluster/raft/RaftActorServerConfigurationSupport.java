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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.FollowerCatchUpTimeout;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
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

    private final RaftActorContext raftContext;

    private final Queue<ServerOperationContext<?>> pendingOperationsQueue = new LinkedList<>();

    private OperationState currentOperationState = IDLE;

    RaftActorServerConfigurationSupport(RaftActorContext context) {
        this.raftContext = context;
    }

    boolean handleMessage(Object message, RaftActor raftActor, ActorRef sender) {
        if(message instanceof AddServer) {
            onAddServer((AddServer)message, raftActor, sender);
            return true;
        } else if (message instanceof FollowerCatchUpTimeout) {
            currentOperationState.onFollowerCatchupTimeout(raftActor, (FollowerCatchUpTimeout)message);
            return true;
        } else if (message instanceof UnInitializedFollowerSnapshotReply) {
            currentOperationState.onUnInitializedFollowerSnapshotReply(raftActor,
                    (UnInitializedFollowerSnapshotReply)message);
            return true;
        } else if(message instanceof ApplyState) {
            return onApplyState((ApplyState) message, raftActor);
        } else {
            return false;
        }
    }

    private boolean onApplyState(ApplyState applyState, RaftActor raftActor) {
        Payload data = applyState.getReplicatedLogEntry().getData();
        if(data instanceof ServerConfigurationPayload) {
            currentOperationState.onApplyState(raftActor, applyState);
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
    private void onAddServer(AddServer addServer, RaftActor raftActor, ActorRef sender) {
        LOG.debug("{}: onAddServer: {}", raftContext.getId(), addServer);

        onNewOperation(raftActor, new AddServerContext(addServer, sender));
    }

    private void onNewOperation(RaftActor raftActor, ServerOperationContext<?> operationContext) {
        if (raftActor.isLeader()) {
            currentOperationState.onNewOperation(raftActor, operationContext);
        } else {
            ActorSelection leader = raftActor.getLeader();
            if (leader != null) {
                LOG.debug("{}: Not leader - forwarding to leader {}", raftContext.getId(), leader);
                leader.forward(operationContext.getOperation(), raftActor.getContext());
            } else {
                LOG.debug("{}: No leader - returning NO_LEADER reply", raftContext.getId());
                operationContext.getClientRequestor().tell(operationContext.newReply(
                        ServerChangeStatus.NO_LEADER, null), raftActor.self());
            }
        }
    }

    /**
     * Interface for a server operation FSM state.
     */
    private interface OperationState {
        void onNewOperation(RaftActor raftActor, ServerOperationContext<?> operationContext);

        void onFollowerCatchupTimeout(RaftActor raftActor, FollowerCatchUpTimeout followerTimeout);

        void onUnInitializedFollowerSnapshotReply(RaftActor raftActor, UnInitializedFollowerSnapshotReply reply);

        void onApplyState(RaftActor raftActor, ApplyState applyState);
    }

    /**
     * Interface for the initial state for a server operation.
     */
    private interface InitialOperationState {
        void initiate(RaftActor raftActor);
    }

    /**
     * Abstract base class for server operation FSM state. Handles common behavior for all states.
     */
    private abstract class AbstractOperationState implements OperationState {
        @Override
        public void onNewOperation(RaftActor raftActor, ServerOperationContext<?> operationContext) {
            // We're currently processing another operation so queue it to be processed later.

            LOG.debug("{}: Server operation already in progress - queueing {}", raftContext.getId(),
                    operationContext.getOperation());

            pendingOperationsQueue.add(operationContext);
        }

        @Override
        public void onFollowerCatchupTimeout(RaftActor raftActor, FollowerCatchUpTimeout followerTimeout) {
            LOG.debug("onFollowerCatchupTimeout should not be called in state {}", this);
        }

        @Override
        public void onUnInitializedFollowerSnapshotReply(RaftActor raftActor, UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("onUnInitializedFollowerSnapshotReply was called in state {}", this);
        }

        @Override
        public void onApplyState(RaftActor raftActor, ApplyState applyState) {
            LOG.debug("onApplyState was called in state {}", this);
        }

        protected void persistNewServerConfiguration(RaftActor raftActor, ServerOperationContext<?> operationContext){
            Collection<PeerInfo> peers = raftContext.getPeers();
            List<ServerInfo> newConfig = new ArrayList<>(peers.size() + 1);
            for(PeerInfo peer: peers) {
                newConfig.add(new ServerInfo(peer.getId(), peer.isVoting()));
            }

            newConfig.add(new ServerInfo(raftContext.getId(), true));

            LOG.debug("{}: New server configuration : {}", raftContext.getId(), newConfig);

            ServerConfigurationPayload payload = new ServerConfigurationPayload(newConfig);

            raftActor.persistData(operationContext.getClientRequestor(), operationContext.getContextId(), payload);

            currentOperationState = new Persisting(operationContext);
        }

        protected void operationComplete(RaftActor raftActor, ServerOperationContext<?> operationContext,
                ServerChangeStatus status) {

            LOG.debug("{}: Returning {} for operation {}", raftContext.getId(), status, operationContext.getOperation());

            operationContext.getClientRequestor().tell(operationContext.newReply(status, raftActor.getLeaderId()),
                    raftActor.self());

            currentOperationState = IDLE;

            ServerOperationContext<?> nextOperation = pendingOperationsQueue.poll();
            if(nextOperation != null) {
                RaftActorServerConfigurationSupport.this.onNewOperation(raftActor, nextOperation);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * The state when no server operation is in progress. It immediately initiates new server operations.
     */
    private class Idle extends AbstractOperationState {
        @Override
        public void onNewOperation(RaftActor raftActor, ServerOperationContext<?> operationContext) {
            operationContext.newInitialOperationState(RaftActorServerConfigurationSupport.this).initiate(raftActor);
        }

        @Override
        public void onApplyState(RaftActor raftActor, ApplyState applyState) {
            // Noop - we override b/c ApplyState is called normally for followers in the idle state.
        }
    }

    /**
     * The state when a new server configuration is being persisted and replicated.
     */
    private class Persisting extends AbstractOperationState {
        private final ServerOperationContext<?> operationContext;

        Persisting(ServerOperationContext<?> operationContext) {
            this.operationContext = operationContext;
        }

        @Override
        public void onApplyState(RaftActor raftActor, ApplyState applyState) {
            // Sanity check - we could get an ApplyState from a previous operation that timed out so make
            // sure it's meant for us.
            if(operationContext.getContextId().equals(applyState.getIdentifier())) {
                LOG.info("{}: {} has been successfully replicated to a majority of followers",
                        applyState.getReplicatedLogEntry().getData());

                operationComplete(raftActor, operationContext, ServerChangeStatus.OK);
            }
        }
    }

    /**
     * Abstract base class for an AddServer operation state.
     */
    private abstract class AddServerState extends AbstractOperationState {
        private final AddServerContext addServerContext;

        AddServerState(AddServerContext addServerContext) {
            this.addServerContext = addServerContext;
        }

        AddServerContext getAddServerContext() {
            return addServerContext;
        }
    }

    /**
     * The initial state for the AddServer operation. It adds the new follower as a peer and initiates
     * snapshot capture, if necessary.
     */
    private class InitialAddServerState extends AddServerState implements InitialOperationState {
        InitialAddServerState(AddServerContext addServerContext) {
            super(addServerContext);
        }

        @Override
        public void initiate(RaftActor raftActor) {
            AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();

            AddServer addServer = getAddServerContext().getOperation();

            LOG.debug("{}: Initiating {}", raftContext.getId(), addServer);

            VotingState votingState = addServer.isVotingMember() ? VotingState.VOTING_NOT_INITIALIZED :
                    VotingState.NON_VOTING;
            raftContext.addToPeers(addServer.getNewServerId(), addServer.getNewServerAddress(), votingState);

            leader.addFollower(addServer.getNewServerId());

            if(votingState == VotingState.VOTING_NOT_INITIALIZED){
                LOG.debug("{}: Leader sending initiate capture snapshot to new follower {}", raftContext.getId(),
                        addServer.getNewServerId());

                leader.initiateCaptureSnapshot(addServer.getNewServerId());

                // schedule the install snapshot timeout timer
                Cancellable installSnapshotTimer = raftContext.getActorSystem().scheduler().scheduleOnce(
                        new FiniteDuration(((raftContext.getConfigParams().getElectionTimeOutInterval().toMillis()) * 2),
                                TimeUnit.MILLISECONDS), raftContext.getActor(),
                                new FollowerCatchUpTimeout(addServer.getNewServerId()),
                                raftContext.getActorSystem().dispatcher(), raftContext.getActor());

                currentOperationState = new InstallingSnapshot(getAddServerContext(), installSnapshotTimer);
            } else {
                LOG.debug("{}: New follower is non-voting - directly persisting new server configuration",
                        raftContext.getId());

                persistNewServerConfiguration(raftActor, getAddServerContext());
            }
        }
    }

    /**
     * The AddServer operation state for when the catch-up snapshot is being installed. It handles successful
     * reply or timeout.
     */
    private class InstallingSnapshot extends AddServerState {
        private final Cancellable installSnapshotTimer;

        InstallingSnapshot(AddServerContext addServerContext, Cancellable installSnapshotTimer) {
            super(addServerContext);
            this.installSnapshotTimer = Preconditions.checkNotNull(installSnapshotTimer);
        }

        @Override
        public void onFollowerCatchupTimeout(RaftActor raftActor, FollowerCatchUpTimeout followerTimeout) {
            String serverId = followerTimeout.getNewServerId();

            LOG.debug("{}: onFollowerCatchupTimeout: {}", raftContext.getId(), serverId);

            AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();

            // cleanup
            raftContext.removePeer(serverId);
            leader.removeFollower(serverId);

            LOG.warn("{}: Timeout occured for new server {} while installing snapshot", raftContext.getId(), serverId);

            operationComplete(raftActor, getAddServerContext(), ServerChangeStatus.TIMEOUT);
        }

        @Override
        public void onUnInitializedFollowerSnapshotReply(RaftActor raftActor, UnInitializedFollowerSnapshotReply reply) {
            LOG.debug("{}: onUnInitializedFollowerSnapshotReply: {}", raftContext.getId(), reply);

            String followerId = reply.getFollowerId();

            // Sanity check to guard against receiving an UnInitializedFollowerSnapshotReply from a prior
            // add server operation that timed out.
            if(getAddServerContext().getOperation().getNewServerId().equals(followerId)) {
                AbstractLeader leader = (AbstractLeader) raftActor.getCurrentBehavior();
                raftContext.getPeerInfo(followerId).setVotingState(VotingState.VOTING);
                leader.updateMinReplicaCount();

                persistNewServerConfiguration(raftActor, getAddServerContext());

                installSnapshotTimer.cancel();
            }
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

        abstract Object newReply(ServerChangeStatus status, String leaderId);

        abstract InitialOperationState newInitialOperationState(RaftActorServerConfigurationSupport support);
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
    }
}
