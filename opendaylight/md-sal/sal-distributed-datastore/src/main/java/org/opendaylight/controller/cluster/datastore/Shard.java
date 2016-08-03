/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.MessageTracker;
import org.opendaylight.controller.cluster.common.actor.MessageTracker.Error;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardMBeanFactory;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * A Shard represents a portion of the logical data tree <br/>
 * <p>
 * Our Shard uses InMemoryDataTree as it's internal representation and delegates all requests it
 * </p>
 */
public class Shard extends RaftActor {

    @VisibleForTesting
    static final Object TX_COMMIT_TIMEOUT_CHECK_MESSAGE = new Object() {
        @Override
        public String toString() {
            return "txCommitTimeoutCheck";
        }
    };

    @VisibleForTesting
    static final Object GET_SHARD_MBEAN_MESSAGE = new Object() {
        @Override
        public String toString() {
            return "getShardMBeanMessage";
        }
    };

    // FIXME: shard names should be encapsulated in their own class and this should be exposed as a constant.
    public static final String DEFAULT_NAME = "default";

    // The state of this Shard
    private final ShardDataTree store;

    /// The name of this shard
    private final String name;

    private final ShardStats shardMBean;

    private DatastoreContext datastoreContext;

    private final ShardCommitCoordinator commitCoordinator;

    private long transactionCommitTimeout;

    private Cancellable txCommitTimeoutCheckSchedule;

    private final Optional<ActorRef> roleChangeNotifier;

    private final MessageTracker appendEntriesReplyTracker;

    private final ShardTransactionActorFactory transactionActorFactory;

    private final ShardSnapshotCohort snapshotCohort;

    private final DataTreeChangeListenerSupport treeChangeSupport = new DataTreeChangeListenerSupport(this);
    private final DataChangeListenerSupport changeSupport = new DataChangeListenerSupport(this);


    private ShardSnapshot restoreFromSnapshot;

    private final ShardTransactionMessageRetrySupport messageRetrySupport;

    private final Map<FrontendIdentifier, LeaderFrontendState> knownFrontends = new HashMap<>();
    private final FrontendMetadata followerFrontendState = new FrontendMetadata();

    protected Shard(final AbstractBuilder<?, ?> builder) {
        super(builder.getId().toString(), builder.getPeerAddresses(),
                Optional.of(builder.getDatastoreContext().getShardRaftConfig()), DataStoreVersions.CURRENT_VERSION);

        this.name = builder.getId().toString();
        this.datastoreContext = builder.getDatastoreContext();
        this.restoreFromSnapshot = builder.getRestoreFromSnapshot();

        setPersistence(datastoreContext.isPersistent());

        LOG.info("Shard created : {}, persistent : {}", name, datastoreContext.isPersistent());

        ShardDataTreeChangeListenerPublisherActorProxy treeChangeListenerPublisher =
                new ShardDataTreeChangeListenerPublisherActorProxy(getContext(), name + "-DTCL-publisher");
        ShardDataChangeListenerPublisherActorProxy dataChangeListenerPublisher =
                new ShardDataChangeListenerPublisherActorProxy(getContext(), name + "-DCL-publisher");
        if(builder.getDataTree() != null) {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getDataTree(),
                    treeChangeListenerPublisher, dataChangeListenerPublisher, name);
        } else {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getTreeType(),
                    treeChangeListenerPublisher, dataChangeListenerPublisher, name);
        }

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name.toString(),
                datastoreContext.getDataStoreMXBeanType());
        shardMBean.setShard(this);

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }

        commitCoordinator = new ShardCommitCoordinator(store, LOG, this.name);

        setTransactionCommitTimeout();

        // create a notifier actor for each cluster member
        roleChangeNotifier = createRoleChangeNotifier(name.toString());

        appendEntriesReplyTracker = new MessageTracker(AppendEntriesReply.class,
                getRaftActorContext().getConfigParams().getIsolatedCheckIntervalInMillis());

        transactionActorFactory = new ShardTransactionActorFactory(store, datastoreContext,
                new Dispatchers(context().system().dispatchers()).getDispatcherPath(
                        Dispatchers.DispatcherType.Transaction), self(), getContext(), shardMBean);

        snapshotCohort = ShardSnapshotCohort.create(getContext(), builder.getId().getMemberName(), store, LOG,
            this.name);

        messageRetrySupport = new ShardTransactionMessageRetrySupport(this);
    }

    private void setTransactionCommitTimeout() {
        transactionCommitTimeout = TimeUnit.MILLISECONDS.convert(
                datastoreContext.getShardTransactionCommitTimeoutInSeconds(), TimeUnit.SECONDS) / 2;
    }

    private Optional<ActorRef> createRoleChangeNotifier(final String shardId) {
        ActorRef shardRoleChangeNotifier = this.getContext().actorOf(
            RoleChangeNotifier.getProps(shardId), shardId + "-notifier");
        return Optional.of(shardRoleChangeNotifier);
    }

    @Override
    public void postStop() {
        LOG.info("Stopping Shard {}", persistenceId());

        super.postStop();

        messageRetrySupport.close();

        if (txCommitTimeoutCheckSchedule != null) {
            txCommitTimeoutCheckSchedule.cancel();
        }

        commitCoordinator.abortPendingTransactions("Transaction aborted due to shutdown.", this);

        shardMBean.unregisterMBean();
    }

    @Override
    protected void handleRecover(final Object message) {
        LOG.debug("{}: onReceiveRecover: Received message {} from {}", persistenceId(), message.getClass(),
            getSender());

        super.handleRecover(message);
        if (LOG.isTraceEnabled()) {
            appendEntriesReplyTracker.begin();
        }
    }

    @Override
    protected void handleNonRaftCommand(final Object message) {
        try (final MessageTracker.Context context = appendEntriesReplyTracker.received(message)) {
            final Optional<Error> maybeError = context.error();
            if (maybeError.isPresent()) {
                LOG.trace("{} : AppendEntriesReply failed to arrive at the expected interval {}", persistenceId(),
                    maybeError.get());
            }

            if (message instanceof RequestEnvelope) {
                final RequestEnvelope envelope = (RequestEnvelope)message;
                try {
                    final RequestSuccess<?, ?> success = handleRequest(envelope);
                    if (success != null) {
                        envelope.sendSuccess(success);
                    }
                } catch (RequestException e) {
                    LOG.debug("{}: request {} failed", persistenceId(), envelope, e);
                    envelope.sendFailure(e);
                } catch (Exception e) {
                    LOG.debug("{}: request {} caused failure", persistenceId(), envelope, e);
                    envelope.sendFailure(new RuntimeRequestException("Request failed to process", e));
                }
            } else if (CreateTransaction.isSerializedType(message)) {
                handleCreateTransaction(message);
            } else if (message instanceof BatchedModifications) {
                handleBatchedModifications((BatchedModifications)message);
            } else if (message instanceof ForwardedReadyTransaction) {
                handleForwardedReadyTransaction((ForwardedReadyTransaction) message);
            } else if (message instanceof ReadyLocalTransaction) {
                handleReadyLocalTransaction((ReadyLocalTransaction)message);
            } else if (CanCommitTransaction.isSerializedType(message)) {
                handleCanCommitTransaction(CanCommitTransaction.fromSerializable(message));
            } else if (CommitTransaction.isSerializedType(message)) {
                handleCommitTransaction(CommitTransaction.fromSerializable(message));
            } else if (AbortTransaction.isSerializedType(message)) {
                handleAbortTransaction(AbortTransaction.fromSerializable(message));
            } else if (CloseTransactionChain.isSerializedType(message)) {
                closeTransactionChain(CloseTransactionChain.fromSerializable(message));
            } else if (message instanceof RegisterChangeListener) {
                changeSupport.onMessage((RegisterChangeListener) message, isLeader(), hasLeader());
            } else if (message instanceof RegisterDataTreeChangeListener) {
                treeChangeSupport.onMessage((RegisterDataTreeChangeListener) message, isLeader(), hasLeader());
            } else if (message instanceof UpdateSchemaContext) {
                updateSchemaContext((UpdateSchemaContext) message);
            } else if (message instanceof PeerAddressResolved) {
                PeerAddressResolved resolved = (PeerAddressResolved) message;
                setPeerAddress(resolved.getPeerId().toString(),
                        resolved.getPeerAddress());
            } else if (TX_COMMIT_TIMEOUT_CHECK_MESSAGE.equals(message)) {
                store.checkForExpiredTransactions(transactionCommitTimeout);
                commitCoordinator.checkForExpiredTransactions(transactionCommitTimeout, this);
            } else if (message instanceof DatastoreContext) {
                onDatastoreContext((DatastoreContext)message);
            } else if (message instanceof RegisterRoleChangeListener){
                roleChangeNotifier.get().forward(message, context());
            } else if (message instanceof FollowerInitialSyncUpStatus) {
                shardMBean.setFollowerInitialSyncStatus(((FollowerInitialSyncUpStatus) message).isInitialSyncDone());
                context().parent().tell(message, self());
            } else if (GET_SHARD_MBEAN_MESSAGE.equals(message)){
                sender().tell(getShardMBean(), self());
            } else if (message instanceof GetShardDataTree) {
                sender().tell(store.getDataTree(), self());
            } else if (message instanceof ServerRemoved){
                context().parent().forward(message, context());
            } else if (ShardTransactionMessageRetrySupport.TIMER_MESSAGE_CLASS.isInstance(message)) {
                messageRetrySupport.onTimerMessage(message);
            } else if (message instanceof DataTreeCohortActorRegistry.CohortRegistryCommand) {
                store.processCohortRegistryCommand(getSender(),
                        (DataTreeCohortActorRegistry.CohortRegistryCommand) message);
            } else {
                super.handleNonRaftCommand(message);
            }
        }
    }

    // Acquire our frontend tracking handle and verify generation matches
    private LeaderFrontendState getFrontend(final ClientIdentifier clientId) throws RequestException {
        final LeaderFrontendState existing = knownFrontends.get(clientId.getFrontendId());
        if (existing != null) {
            final int cmp = Long.compareUnsigned(existing.getGeneration(), clientId.getGeneration());
            if (cmp == 0) {
                return existing;
            }
            if (cmp > 0) {
                LOG.debug("{}: rejecting request from outdated client {}", persistenceId(), clientId);
                throw new RetiredGenerationException(existing.getGeneration());
            }

            LOG.info("{}: retiring state {}, outdated by request from client {}", persistenceId(), existing, clientId);
            existing.retire();
            knownFrontends.remove(clientId.getFrontendId());
        } else {
            LOG.debug("{}: client {} is not yet known", persistenceId(), clientId);
        }

        final LeaderFrontendState ret = new LeaderFrontendState(persistenceId(), clientId, store);
        knownFrontends.put(clientId.getFrontendId(), ret);
        LOG.debug("{}: created state {} for client {}", persistenceId(), ret, clientId);
        return ret;
    }

    private @Nullable RequestSuccess<?, ?> handleRequest(final RequestEnvelope envelope) throws RequestException {
        // We are not the leader, hence we want to fail-fast.
        if (!isLeader() || !isLeaderActive()) {
            LOG.debug("{}: not currently leader, rejecting request {}", persistenceId(), envelope);
            throw new NotLeaderException(getSelf());
        }

        final Request<?, ?> request = envelope.getMessage();
        if (request instanceof TransactionRequest) {
            final TransactionRequest<?> txReq = (TransactionRequest<?>)request;
            final ClientIdentifier clientId = txReq.getTarget().getHistoryId().getClientId();
            return getFrontend(clientId).handleTransactionRequest(txReq, envelope);
        } else if (request instanceof LocalHistoryRequest) {
            final LocalHistoryRequest<?> lhReq = (LocalHistoryRequest<?>)request;
            final ClientIdentifier clientId = lhReq.getTarget().getClientId();
            return getFrontend(clientId).handleLocalHistoryRequest(lhReq, envelope);
        } else {
            LOG.debug("{}: rejecting unsupported request {}", persistenceId(), request);
            throw new UnsupportedRequestException(request);
        }
    }

    private boolean hasLeader() {
        return getLeaderId() != null;
    }

    public int getPendingTxCommitQueueSize() {
        return store.getQueueSize();
    }

    public int getCohortCacheSize() {
        return commitCoordinator.getCohortCacheSize();
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override
    protected LeaderStateChanged newLeaderStateChanged(final String memberId, final String leaderId, final short leaderPayloadVersion) {
        return isLeader() ? new ShardLeaderStateChanged(memberId, leaderId, store.getDataTree(), leaderPayloadVersion)
                : new ShardLeaderStateChanged(memberId, leaderId, leaderPayloadVersion);
    }

    protected void onDatastoreContext(final DatastoreContext context) {
        datastoreContext = context;

        setTransactionCommitTimeout();

        if (datastoreContext.isPersistent() && !persistence().isRecoveryApplicable()) {
            setPersistence(true);
        } else if (!datastoreContext.isPersistent() && persistence().isRecoveryApplicable()) {
            setPersistence(false);
        }

        updateConfigParams(datastoreContext.getShardRaftConfig());
    }

    boolean canSkipPayload() {
        // If we do not have any followers and we are not using persistence we can apply modification to the state
        // immediately
        return !hasFollowers() && !persistence().isRecoveryApplicable();
    }

    // applyState() will be invoked once consensus is reached on the payload
    void persistPayload(final TransactionIdentifier transactionId, final Payload payload) {
        // We are faking the sender
        persistData(self(), transactionId, payload);
    }

    private void handleCommitTransaction(final CommitTransaction commit) {
        if (isLeader()) {
            commitCoordinator.handleCommit(commit.getTransactionID(), getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (leader == null) {
                messageRetrySupport.addMessageToRetry(commit, getSender(),
                        "Could not commit transaction " + commit.getTransactionID());
            } else {
                LOG.debug("{}: Forwarding CommitTransaction to leader {}", persistenceId(), leader);
                leader.forward(commit, getContext());
            }
        }
    }

    private void handleCanCommitTransaction(final CanCommitTransaction canCommit) {
        LOG.debug("{}: Can committing transaction {}", persistenceId(), canCommit.getTransactionID());

        if (isLeader()) {
        commitCoordinator.handleCanCommit(canCommit.getTransactionID(), getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (leader == null) {
                messageRetrySupport.addMessageToRetry(canCommit, getSender(),
                        "Could not canCommit transaction " + canCommit.getTransactionID());
            } else {
                LOG.debug("{}: Forwarding CanCommitTransaction to leader {}", persistenceId(), leader);
                leader.forward(canCommit, getContext());
            }
        }
    }

    protected void handleBatchedModificationsLocal(final BatchedModifications batched, final ActorRef sender) {
        try {
            commitCoordinator.handleBatchedModifications(batched, sender, this);
        } catch (Exception e) {
            LOG.error("{}: Error handling BatchedModifications for Tx {}", persistenceId(),
                    batched.getTransactionID(), e);
            sender.tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private void handleBatchedModifications(final BatchedModifications batched) {
        // This message is sent to prepare the modifications transaction directly on the Shard as an
        // optimization to avoid the extra overhead of a separate ShardTransaction actor. On the last
        // BatchedModifications message, the caller sets the ready flag in the message indicating
        // modifications are complete. The reply contains the cohort actor path (this actor) for the caller
        // to initiate the 3-phase commit. This also avoids the overhead of sending an additional
        // ReadyTransaction message.

        // If we're not the leader then forward to the leader. This is a safety measure - we shouldn't
        // normally get here if we're not the leader as the front-end (TransactionProxy) should determine
        // the primary/leader shard. However with timing and caching on the front-end, there's a small
        // window where it could have a stale leader during leadership transitions.
        //
        boolean isLeaderActive = isLeaderActive();
        if (isLeader() && isLeaderActive) {
            handleBatchedModificationsLocal(batched, getSender());
        } else {
            ActorSelection leader = getLeader();
            if (!isLeaderActive || leader == null) {
                messageRetrySupport.addMessageToRetry(batched, getSender(),
                        "Could not commit transaction " + batched.getTransactionID());
            } else {
                // If this is not the first batch and leadership changed in between batched messages,
                // we need to reconstruct previous BatchedModifications from the transaction
                // DataTreeModification, honoring the max batched modification count, and forward all the
                // previous BatchedModifications to the new leader.
                Collection<BatchedModifications> newModifications = commitCoordinator.createForwardedBatchedModifications(
                        batched, datastoreContext.getShardBatchedModificationCount());

                LOG.debug("{}: Forwarding {} BatchedModifications to leader {}", persistenceId(),
                        newModifications.size(), leader);

                for (BatchedModifications bm : newModifications) {
                    leader.forward(bm, getContext());
                }
            }
        }
    }

    private boolean failIfIsolatedLeader(final ActorRef sender) {
        if (isIsolatedLeader()) {
            sender.tell(new akka.actor.Status.Failure(new NoShardLeaderException(String.format(
                    "Shard %s was the leader but has lost contact with all of its followers. Either all" +
                    " other follower nodes are down or this node is isolated by a network partition.",
                    persistenceId()))), getSelf());
            return true;
        }

        return false;
    }

    protected boolean isIsolatedLeader() {
        return getRaftState() == RaftState.IsolatedLeader;
    }

    private void handleReadyLocalTransaction(final ReadyLocalTransaction message) {
        LOG.debug("{}: handleReadyLocalTransaction for {}", persistenceId(), message.getTransactionID());

        boolean isLeaderActive = isLeaderActive();
        if (isLeader() && isLeaderActive) {
            try {
                commitCoordinator.handleReadyLocalTransaction(message, getSender(), this);
            } catch (Exception e) {
                LOG.error("{}: Error handling ReadyLocalTransaction for Tx {}", persistenceId(),
                        message.getTransactionID(), e);
                getSender().tell(new akka.actor.Status.Failure(e), getSelf());
            }
        } else {
            ActorSelection leader = getLeader();
            if (!isLeaderActive || leader == null) {
                messageRetrySupport.addMessageToRetry(message, getSender(),
                        "Could not commit transaction " + message.getTransactionID());
            } else {
                LOG.debug("{}: Forwarding ReadyLocalTransaction to leader {}", persistenceId(), leader);
                message.setRemoteVersion(getCurrentBehavior().getLeaderPayloadVersion());
                leader.forward(message, getContext());
            }
        }
    }

    private void handleForwardedReadyTransaction(final ForwardedReadyTransaction forwardedReady) {
        LOG.debug("{}: handleForwardedReadyTransaction for {}", persistenceId(), forwardedReady.getTransactionID());

        boolean isLeaderActive = isLeaderActive();
        if (isLeader() && isLeaderActive) {
            commitCoordinator.handleForwardedReadyTransaction(forwardedReady, getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (!isLeaderActive || leader == null) {
                messageRetrySupport.addMessageToRetry(forwardedReady, getSender(),
                        "Could not commit transaction " + forwardedReady.getTransactionID());
            } else {
                LOG.debug("{}: Forwarding ForwardedReadyTransaction to leader {}", persistenceId(), leader);

                ReadyLocalTransaction readyLocal = new ReadyLocalTransaction(forwardedReady.getTransactionID(),
                        forwardedReady.getTransaction().getSnapshot(), forwardedReady.isDoImmediateCommit());
                readyLocal.setRemoteVersion(getCurrentBehavior().getLeaderPayloadVersion());
                leader.forward(readyLocal, getContext());
            }
        }
    }

    private void handleAbortTransaction(final AbortTransaction abort) {
        doAbortTransaction(abort.getTransactionID(), getSender());
    }

    void doAbortTransaction(final TransactionIdentifier transactionID, final ActorRef sender) {
        commitCoordinator.handleAbort(transactionID, sender, this);
    }

    private void handleCreateTransaction(final Object message) {
        if (isLeader()) {
            createTransaction(CreateTransaction.fromSerializable(message));
        } else if (getLeader() != null) {
            getLeader().forward(message, getContext());
        } else {
            getSender().tell(new akka.actor.Status.Failure(new NoShardLeaderException(
                    "Could not create a shard transaction", persistenceId())), getSelf());
        }
    }

    private void closeTransactionChain(final CloseTransactionChain closeTransactionChain) {
        store.closeTransactionChain(closeTransactionChain.getIdentifier());
    }

    private void createTransaction(final CreateTransaction createTransaction) {
        try {
            if (TransactionType.fromInt(createTransaction.getTransactionType()) != TransactionType.READ_ONLY &&
                    failIfIsolatedLeader(getSender())) {
                return;
            }

            ActorRef transactionActor = createTransaction(createTransaction.getTransactionType(),
                createTransaction.getTransactionId());

            getSender().tell(new CreateTransactionReply(Serialization.serializedActorPath(transactionActor),
                    createTransaction.getTransactionId(), createTransaction.getVersion()).toSerializable(), getSelf());
        } catch (Exception e) {
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private ActorRef createTransaction(final int transactionType, final TransactionIdentifier transactionId) {
        LOG.debug("{}: Creating transaction : {} ", persistenceId(), transactionId);
        return transactionActorFactory.newShardTransaction(TransactionType.fromInt(transactionType),
            transactionId);
    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
        updateSchemaContext(message.getSchemaContext());
    }

    @VisibleForTesting
    void updateSchemaContext(final SchemaContext schemaContext) {
        store.updateSchemaContext(schemaContext);
    }

    private boolean isMetricsCaptureEnabled() {
        CommonConfig config = new CommonConfig(getContext().system().settings().config());
        return config.isMetricCaptureEnabled();
    }

    @Override
    @VisibleForTesting
    public RaftActorSnapshotCohort getRaftActorSnapshotCohort() {
        return snapshotCohort;
    }

    @Override
    @Nonnull
    protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        return new ShardRecoveryCoordinator(store,
            restoreFromSnapshot != null ? restoreFromSnapshot.getSnapshot() : null, persistenceId(), LOG);
    }

    @Override
    protected void onRecoveryComplete() {
        restoreFromSnapshot = null;

        //notify shard manager
        getContext().parent().tell(new ActorInitialized(), getSelf());

        // Being paranoid here - this method should only be called once but just in case...
        if (txCommitTimeoutCheckSchedule == null) {
            // Schedule a message to be periodically sent to check if the current in-progress
            // transaction should be expired and aborted.
            FiniteDuration period = Duration.create(transactionCommitTimeout / 3, TimeUnit.MILLISECONDS);
            txCommitTimeoutCheckSchedule = getContext().system().scheduler().schedule(
                    period, period, getSelf(),
                    TX_COMMIT_TIMEOUT_CHECK_MESSAGE, getContext().dispatcher(), ActorRef.noSender());
        }
    }

    @Override
    protected void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        if (data instanceof Payload) {
            try {
                store.applyReplicatedPayload(identifier, (Payload)data);
            } catch (DataValidationFailedException | IOException e) {
                LOG.error("{}: Error applying replica {}", persistenceId(), identifier, e);
            }
        } else {
            LOG.error("{}: Unknown state for {} received {}", persistenceId(), identifier, data);
        }
    }

    @Override
    protected void onStateChanged() {
        boolean isLeader = isLeader();
        boolean hasLeader = hasLeader();
        changeSupport.onLeadershipChange(isLeader, hasLeader);
        treeChangeSupport.onLeadershipChange(isLeader, hasLeader);

        // If this actor is no longer the leader close all the transaction chains
        if (!isLeader) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "{}: onStateChanged: Closing all transaction chains because shard {} is no longer the leader",
                    persistenceId(), getId());
            }

            store.closeAllTransactionChains();
        }

        if (hasLeader && !isIsolatedLeader()) {
            messageRetrySupport.retryMessages();
        }
    }

    @Override
    protected void onLeaderChanged(final String oldLeader, final String newLeader) {
        shardMBean.incrementLeadershipChangeCount();

        boolean hasLeader = hasLeader();
        if (hasLeader && !isLeader()) {
            // Another leader was elected. If we were the previous leader and had pending transactions, convert
            // them to transaction messages and send to the new leader.
            ActorSelection leader = getLeader();
            if (leader != null) {
                Collection<?> messagesToForward = commitCoordinator.convertPendingTransactionsToMessages(
                            datastoreContext.getShardBatchedModificationCount());

                if (!messagesToForward.isEmpty()) {
                    LOG.debug("{}: Forwarding {} pending transaction messages to leader {}", persistenceId(),
                            messagesToForward.size(), leader);

                    for (Object message : messagesToForward) {
                        leader.tell(message, self());
                    }
                }
            } else {
                commitCoordinator.abortPendingTransactions(
                        "The transacton was aborted due to inflight leadership change and the leader address isn't available.",
                        this);
            }
        }

        if (hasLeader && !isIsolatedLeader()) {
            messageRetrySupport.retryMessages();
        }
    }

    @Override
    protected void pauseLeader(final Runnable operation) {
        LOG.debug("{}: In pauseLeader, operation: {}", persistenceId(), operation);
        store.setRunOnPendingTransactionsComplete(operation);
    }

    @Override
    public String persistenceId() {
        return this.name;
    }

    @VisibleForTesting
    ShardCommitCoordinator getCommitCoordinator() {
        return commitCoordinator;
    }

    public DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    @VisibleForTesting
    public ShardDataTree getDataStore() {
        return store;
    }

    @VisibleForTesting
    ShardStats getShardMBean() {
        return shardMBean;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static abstract class AbstractBuilder<T extends AbstractBuilder<T, S>, S extends Shard> {
        private final Class<S> shardClass;
        private ShardIdentifier id;
        private Map<String, String> peerAddresses = Collections.emptyMap();
        private DatastoreContext datastoreContext;
        private SchemaContext schemaContext;
        private DatastoreSnapshot.ShardSnapshot restoreFromSnapshot;
        private TipProducingDataTree dataTree;
        private volatile boolean sealed;

        protected AbstractBuilder(final Class<S> shardClass) {
            this.shardClass = shardClass;
        }

        protected void checkSealed() {
            Preconditions.checkState(!sealed, "Builder isalready sealed - further modifications are not allowed");
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public T id(final ShardIdentifier id) {
            checkSealed();
            this.id = id;
            return self();
        }

        public T peerAddresses(final Map<String, String> peerAddresses) {
            checkSealed();
            this.peerAddresses = peerAddresses;
            return self();
        }

        public T datastoreContext(final DatastoreContext datastoreContext) {
            checkSealed();
            this.datastoreContext = datastoreContext;
            return self();
        }

        public T schemaContext(final SchemaContext schemaContext) {
            checkSealed();
            this.schemaContext = schemaContext;
            return self();
        }

        public T restoreFromSnapshot(final DatastoreSnapshot.ShardSnapshot restoreFromSnapshot) {
            checkSealed();
            this.restoreFromSnapshot = restoreFromSnapshot;
            return self();
        }

        public T dataTree(final TipProducingDataTree dataTree) {
            checkSealed();
            this.dataTree = dataTree;
            return self();
        }

        public ShardIdentifier getId() {
            return id;
        }

        public Map<String, String> getPeerAddresses() {
            return peerAddresses;
        }

        public DatastoreContext getDatastoreContext() {
            return datastoreContext;
        }

        public SchemaContext getSchemaContext() {
            return schemaContext;
        }

        public DatastoreSnapshot.ShardSnapshot getRestoreFromSnapshot() {
            return restoreFromSnapshot;
        }

        public TipProducingDataTree getDataTree() {
            return dataTree;
        }

        public TreeType getTreeType() {
            switch (datastoreContext.getLogicalStoreType()) {
            case CONFIGURATION:
                return TreeType.CONFIGURATION;
            case OPERATIONAL:
                return TreeType.OPERATIONAL;
            }

            throw new IllegalStateException("Unhandled logical store type " + datastoreContext.getLogicalStoreType());
        }

        protected void verify() {
            Preconditions.checkNotNull(id, "id should not be null");
            Preconditions.checkNotNull(peerAddresses, "peerAddresses should not be null");
            Preconditions.checkNotNull(datastoreContext, "dataStoreContext should not be null");
            Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");
        }

        public Props props() {
            sealed = true;
            verify();
            return Props.create(shardClass, this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, Shard> {
        private Builder() {
            super(Shard.class);
        }
    }

    Ticker ticker() {
        return Ticker.systemTicker();
    }
}
