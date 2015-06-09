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
import akka.japi.Creator;
import akka.persistence.RecoveryFailure;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortEntry;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardMBeanFactory;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.ModificationPayload;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.datastore.utils.MessageTracker;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
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

    private static final Object TX_COMMIT_TIMEOUT_CHECK_MESSAGE = "txCommitTimeoutCheck";

    @VisibleForTesting
    static final Object GET_SHARD_MBEAN_MESSAGE = "getShardMBeanMessage";

    @VisibleForTesting
    static final String DEFAULT_NAME = "default";

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

    protected Shard(final ShardIdentifier name, final Map<String, String> peerAddresses,
            final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
        super(name.toString(), new HashMap<>(peerAddresses), Optional.of(datastoreContext.getShardRaftConfig()),
                DataStoreVersions.CURRENT_VERSION);

        this.name = name.toString();
        this.datastoreContext = datastoreContext;

        setPersistence(datastoreContext.isPersistent());

        LOG.info("Shard created : {}, persistent : {}", name, datastoreContext.isPersistent());

        store = new ShardDataTree(schemaContext);

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name.toString(),
                datastoreContext.getDataStoreMXBeanType());
        shardMBean.setShardActor(getSelf());

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }

        commitCoordinator = new ShardCommitCoordinator(store,
                datastoreContext.getShardCommitQueueExpiryTimeoutInMillis(),
                datastoreContext.getShardTransactionCommitQueueCapacity(), self(), LOG, this.name);

        setTransactionCommitTimeout();

        // create a notifier actor for each cluster member
        roleChangeNotifier = createRoleChangeNotifier(name.toString());

        appendEntriesReplyTracker = new MessageTracker(AppendEntriesReply.class,
                getRaftActorContext().getConfigParams().getIsolatedCheckIntervalInMillis());

        transactionActorFactory = new ShardTransactionActorFactory(store, datastoreContext,
                new Dispatchers(context().system().dispatchers()).getDispatcherPath(
                        Dispatchers.DispatcherType.Transaction), self(), getContext(), shardMBean);

        snapshotCohort = new ShardSnapshotCohort(transactionActorFactory, store, LOG, this.name);
    }

    private void setTransactionCommitTimeout() {
        transactionCommitTimeout = TimeUnit.MILLISECONDS.convert(
                datastoreContext.getShardTransactionCommitTimeoutInSeconds(), TimeUnit.SECONDS) / 2;
    }

    public static Props props(final ShardIdentifier name,
        final Map<String, String> peerAddresses,
        final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(peerAddresses, "peerAddresses should not be null");
        Preconditions.checkNotNull(datastoreContext, "dataStoreContext should not be null");
        Preconditions.checkNotNull(schemaContext, "schemaContext should not be null");

        return Props.create(new ShardCreator(name, peerAddresses, datastoreContext, schemaContext));
    }

    private Optional<ActorRef> createRoleChangeNotifier(String shardId) {
        ActorRef shardRoleChangeNotifier = this.getContext().actorOf(
            RoleChangeNotifier.getProps(shardId), shardId + "-notifier");
        return Optional.of(shardRoleChangeNotifier);
    }

    @Override
    public void postStop() {
        LOG.info("Stopping Shard {}", persistenceId());

        super.postStop();

        if(txCommitTimeoutCheckSchedule != null) {
            txCommitTimeoutCheckSchedule.cancel();
        }

        shardMBean.unregisterMBean();
    }

    @Override
    public void onReceiveRecover(final Object message) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: onReceiveRecover: Received message {} from {}", persistenceId(),
                message.getClass().toString(), getSender());
        }

        if (message instanceof RecoveryFailure){
            LOG.error("{}: Recovery failed because of this cause",
                    persistenceId(), ((RecoveryFailure) message).cause());

            // Even though recovery failed, we still need to finish our recovery, eg send the
            // ActorInitialized message and start the txCommitTimeoutCheckSchedule.
            onRecoveryComplete();
        } else {
            super.onReceiveRecover(message);
            if(LOG.isTraceEnabled()) {
                appendEntriesReplyTracker.begin();
            }
        }
    }

    @Override
    public void onReceiveCommand(final Object message) throws Exception {

        MessageTracker.Context context = appendEntriesReplyTracker.received(message);

        if(context.error().isPresent()){
            LOG.trace("{} : AppendEntriesReply failed to arrive at the expected interval {}", persistenceId(),
                    context.error());
        }

        try {
            if (CreateTransaction.SERIALIZABLE_CLASS.isInstance(message)) {
                handleCreateTransaction(message);
            } else if (BatchedModifications.class.isInstance(message)) {
                handleBatchedModifications((BatchedModifications)message);
            } else if (message instanceof ForwardedReadyTransaction) {
                commitCoordinator.handleForwardedReadyTransaction((ForwardedReadyTransaction) message,
                        getSender(), this);
            } else if (message instanceof ReadyLocalTransaction) {
                handleReadyLocalTransaction((ReadyLocalTransaction)message);
            } else if (CanCommitTransaction.SERIALIZABLE_CLASS.isInstance(message)) {
                handleCanCommitTransaction(CanCommitTransaction.fromSerializable(message));
            } else if (CommitTransaction.SERIALIZABLE_CLASS.isInstance(message)) {
                handleCommitTransaction(CommitTransaction.fromSerializable(message));
            } else if (AbortTransaction.SERIALIZABLE_CLASS.isInstance(message)) {
                handleAbortTransaction(AbortTransaction.fromSerializable(message));
            } else if (CloseTransactionChain.SERIALIZABLE_CLASS.isInstance(message)) {
                closeTransactionChain(CloseTransactionChain.fromSerializable(message));
            } else if (message instanceof RegisterChangeListener) {
                changeSupport.onMessage((RegisterChangeListener) message, isLeader());
            } else if (message instanceof RegisterDataTreeChangeListener) {
                treeChangeSupport.onMessage((RegisterDataTreeChangeListener) message, isLeader());
            } else if (message instanceof UpdateSchemaContext) {
                updateSchemaContext((UpdateSchemaContext) message);
            } else if (message instanceof PeerAddressResolved) {
                PeerAddressResolved resolved = (PeerAddressResolved) message;
                setPeerAddress(resolved.getPeerId().toString(),
                        resolved.getPeerAddress());
            } else if (message.equals(TX_COMMIT_TIMEOUT_CHECK_MESSAGE)) {
                handleTransactionCommitTimeoutCheck();
            } else if(message instanceof DatastoreContext) {
                onDatastoreContext((DatastoreContext)message);
            } else if(message instanceof RegisterRoleChangeListener){
                roleChangeNotifier.get().forward(message, context());
            } else if (message instanceof FollowerInitialSyncUpStatus) {
                shardMBean.setFollowerInitialSyncStatus(((FollowerInitialSyncUpStatus) message).isInitialSyncDone());
                context().parent().tell(message, self());
            } else if(GET_SHARD_MBEAN_MESSAGE.equals(message)){
                sender().tell(getShardMBean(), self());
            } else {
                super.onReceiveCommand(message);
            }
        } finally {
            context.done();
        }
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return roleChangeNotifier;
    }

    @Override
    protected LeaderStateChanged newLeaderStateChanged(String memberId, String leaderId, short leaderPayloadVersion) {
        return new ShardLeaderStateChanged(memberId, leaderId,
                isLeader() ? Optional.<DataTree>of(store.getDataTree()) : Optional.<DataTree>absent(),
                leaderPayloadVersion);
    }

    private void onDatastoreContext(DatastoreContext context) {
        datastoreContext = context;

        commitCoordinator.setQueueCapacity(datastoreContext.getShardTransactionCommitQueueCapacity());

        setTransactionCommitTimeout();

        if(datastoreContext.isPersistent() && !persistence().isRecoveryApplicable()) {
            setPersistence(true);
        } else if(!datastoreContext.isPersistent() && persistence().isRecoveryApplicable()) {
            setPersistence(false);
        }

        updateConfigParams(datastoreContext.getShardRaftConfig());
    }

    private void handleTransactionCommitTimeoutCheck() {
        CohortEntry cohortEntry = commitCoordinator.getCurrentCohortEntry();
        if(cohortEntry != null) {
            if(cohortEntry.isExpired(transactionCommitTimeout)) {
                LOG.warn("{}: Current transaction {} has timed out after {} ms - aborting",
                        persistenceId(), cohortEntry.getTransactionID(), transactionCommitTimeout);

                doAbortTransaction(cohortEntry.getTransactionID(), null);
            }
        }

        commitCoordinator.cleanupExpiredCohortEntries();
    }

    private static boolean isEmptyCommit(final DataTreeCandidate candidate) {
        return ModificationType.UNMODIFIED.equals(candidate.getRootNode().getModificationType());
    }

    void continueCommit(final CohortEntry cohortEntry) throws Exception {
        final DataTreeCandidate candidate = cohortEntry.getCohort().getCandidate();

        // If we do not have any followers and we are not using persistence
        // or if cohortEntry has no modifications
        // we can apply modification to the state immediately
        if ((!hasFollowers() && !persistence().isRecoveryApplicable()) || isEmptyCommit(candidate)) {
            applyModificationToState(cohortEntry.getReplySender(), cohortEntry.getTransactionID(), candidate);
        } else {
            Shard.this.persistData(cohortEntry.getReplySender(), cohortEntry.getTransactionID(),
                DataTreeCandidatePayload.create(candidate));
        }
    }

    private void handleCommitTransaction(final CommitTransaction commit) {
        if(!commitCoordinator.handleCommit(commit.getTransactionID(), getSender(), this)) {
            shardMBean.incrementFailedTransactionsCount();
        }
    }

    private void finishCommit(@Nonnull final ActorRef sender, @Nonnull final String transactionID, @Nonnull final CohortEntry cohortEntry) {
        LOG.debug("{}: Finishing commit for transaction {}", persistenceId(), cohortEntry.getTransactionID());

        try {
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            cohortEntry.getCohort().commit().get();

            sender.tell(CommitTransactionReply.INSTANCE.toSerializable(), getSelf());

            shardMBean.incrementCommittedTransactionCount();
            shardMBean.setLastCommittedTransactionTime(System.currentTimeMillis());

        } catch (Exception e) {
            sender.tell(new akka.actor.Status.Failure(e), getSelf());

            LOG.error("{}, An exception occurred while committing transaction {}", persistenceId(),
                    transactionID, e);
            shardMBean.incrementFailedTransactionsCount();
        } finally {
            commitCoordinator.currentTransactionComplete(transactionID, true);
        }
    }

    private void finishCommit(@Nonnull final ActorRef sender, final @Nonnull String transactionID) {
        // With persistence enabled, this method is called via applyState by the leader strategy
        // after the commit has been replicated to a majority of the followers.

        CohortEntry cohortEntry = commitCoordinator.getCohortEntryIfCurrent(transactionID);
        if (cohortEntry == null) {
            // The transaction is no longer the current commit. This can happen if the transaction
            // was aborted prior, most likely due to timeout in the front-end. We need to finish
            // committing the transaction though since it was successfully persisted and replicated
            // however we can't use the original cohort b/c it was already preCommitted and may
            // conflict with the current commit or may have been aborted so we commit with a new
            // transaction.
            cohortEntry = commitCoordinator.getAndRemoveCohortEntry(transactionID);
            if(cohortEntry != null) {
                try {
                    store.applyForeignCandidate(transactionID, cohortEntry.getCohort().getCandidate());
                } catch (DataValidationFailedException e) {
                    shardMBean.incrementFailedTransactionsCount();
                    LOG.error("{}: Failed to re-apply transaction {}", persistenceId(), transactionID, e);
                }

                sender.tell(CommitTransactionReply.INSTANCE.toSerializable(), getSelf());
            } else {
                // This really shouldn't happen - it likely means that persistence or replication
                // took so long to complete such that the cohort entry was expired from the cache.
                IllegalStateException ex = new IllegalStateException(
                        String.format("%s: Could not finish committing transaction %s - no CohortEntry found",
                                persistenceId(), transactionID));
                LOG.error(ex.getMessage());
                sender.tell(new akka.actor.Status.Failure(ex), getSelf());
            }
        } else {
            finishCommit(sender, transactionID, cohortEntry);
        }
    }

    private void handleCanCommitTransaction(final CanCommitTransaction canCommit) {
        LOG.debug("{}: Can committing transaction {}", persistenceId(), canCommit.getTransactionID());
        commitCoordinator.handleCanCommit(canCommit.getTransactionID(), getSender(), this);
    }

    private void noLeaderError(Object message) {
        // TODO: rather than throwing an immediate exception, we could schedule a timer to try again to make
        // it more resilient in case we're in the process of electing a new leader.
        getSender().tell(new akka.actor.Status.Failure(new NoShardLeaderException(String.format(
            "Could not find the leader for shard %s. This typically happens" +
            " when the system is coming up or recovering and a leader is being elected. Try again" +
            " later.", persistenceId()))), getSelf());
    }

    private void handleBatchedModifications(BatchedModifications batched) {
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
        if(isLeader()) {
            try {
                commitCoordinator.handleBatchedModifications(batched, getSender(), this);
            } catch (Exception e) {
                LOG.error("{}: Error handling BatchedModifications for Tx {}", persistenceId(),
                        batched.getTransactionID(), e);
                getSender().tell(new akka.actor.Status.Failure(e), getSelf());
            }
        } else {
            ActorSelection leader = getLeader();
            if(leader != null) {
                // TODO: what if this is not the first batch and leadership changed in between batched messages?
                // We could check if the commitCoordinator already has a cached entry and forward all the previous
                // batched modifications.
                LOG.debug("{}: Forwarding BatchedModifications to leader {}", persistenceId(), leader);
                leader.forward(batched, getContext());
            } else {
                noLeaderError(batched);
            }
        }
    }

    private void handleReadyLocalTransaction(final ReadyLocalTransaction message) {
        if (isLeader()) {
            try {
                commitCoordinator.handleReadyLocalTransaction(message, getSender(), this);
            } catch (Exception e) {
                LOG.error("{}: Error handling ReadyLocalTransaction for Tx {}", persistenceId(),
                        message.getTransactionID(), e);
                getSender().tell(new akka.actor.Status.Failure(e), getSelf());
            }
        } else {
            ActorSelection leader = getLeader();
            if (leader != null) {
                LOG.debug("{}: Forwarding ReadyLocalTransaction to leader {}", persistenceId(), leader);
                message.setRemoteVersion(getCurrentBehavior().getLeaderPayloadVersion());
                leader.forward(message, getContext());
            } else {
                noLeaderError(message);
            }
        }
    }

    private void handleAbortTransaction(final AbortTransaction abort) {
        doAbortTransaction(abort.getTransactionID(), getSender());
    }

    void doAbortTransaction(final String transactionID, final ActorRef sender) {
        final CohortEntry cohortEntry = commitCoordinator.getCohortEntryIfCurrent(transactionID);
        if(cohortEntry != null) {
            LOG.debug("{}: Aborting transaction {}", persistenceId(), transactionID);

            // We don't remove the cached cohort entry here (ie pass false) in case the Tx was
            // aborted during replication in which case we may still commit locally if replication
            // succeeds.
            commitCoordinator.currentTransactionComplete(transactionID, false);

            final ListenableFuture<Void> future = cohortEntry.getCohort().abort();
            final ActorRef self = getSelf();

            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void v) {
                    shardMBean.incrementAbortTransactionsCount();

                    if(sender != null) {
                        sender.tell(AbortTransactionReply.INSTANCE.toSerializable(), self);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("{}: An exception happened during abort", persistenceId(), t);

                    if(sender != null) {
                        sender.tell(new akka.actor.Status.Failure(t), self);
                    }
                }
            });
        }
    }

    private void handleCreateTransaction(final Object message) {
        if (isLeader()) {
            createTransaction(CreateTransaction.fromSerializable(message));
        } else if (getLeader() != null) {
            getLeader().forward(message, getContext());
        } else {
            getSender().tell(new akka.actor.Status.Failure(new NoShardLeaderException(String.format(
                "Could not find leader for shard %s so transaction cannot be created. This typically happens" +
                " when the system is coming up or recovering and a leader is being elected. Try again" +
                " later.", persistenceId()))), getSelf());
        }
    }

    private void closeTransactionChain(final CloseTransactionChain closeTransactionChain) {
        store.closeTransactionChain(closeTransactionChain.getTransactionChainId());
    }

    private ActorRef createTypedTransactionActor(int transactionType,
            ShardTransactionIdentifier transactionId, String transactionChainId,
            short clientVersion ) {

        return transactionActorFactory.newShardTransaction(TransactionType.fromInt(transactionType),
                transactionId, transactionChainId, clientVersion);
    }

    private void createTransaction(CreateTransaction createTransaction) {
        try {
            ActorRef transactionActor = createTransaction(createTransaction.getTransactionType(),
                createTransaction.getTransactionId(), createTransaction.getTransactionChainId(),
                createTransaction.getVersion());

            getSender().tell(new CreateTransactionReply(Serialization.serializedActorPath(transactionActor),
                    createTransaction.getTransactionId()).toSerializable(), getSelf());
        } catch (Exception e) {
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }
    }

    private ActorRef createTransaction(int transactionType, String remoteTransactionId,
            String transactionChainId, short clientVersion) {


        ShardTransactionIdentifier transactionId = new ShardTransactionIdentifier(remoteTransactionId);

        if(LOG.isDebugEnabled()) {
            LOG.debug("{}: Creating transaction : {} ", persistenceId(), transactionId);
        }

        ActorRef transactionActor = createTypedTransactionActor(transactionType, transactionId,
                transactionChainId, clientVersion);

        return transactionActor;
    }

    private void commitWithNewTransaction(final Modification modification) {
        ReadWriteShardDataTreeTransaction tx = store.newReadWriteTransaction(modification.toString(), null);
        modification.apply(tx.getSnapshot());
        try {
            snapshotCohort.syncCommitTransaction(tx);
            shardMBean.incrementCommittedTransactionCount();
            shardMBean.setLastCommittedTransactionTime(System.currentTimeMillis());
        } catch (Exception e) {
            shardMBean.incrementFailedTransactionsCount();
            LOG.error("{}: Failed to commit", persistenceId(), e);
        }
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
        return new ShardRecoveryCoordinator(store, persistenceId(), LOG);
    }

    @Override
    protected void onRecoveryComplete() {
        store.recoveryDone();
        //notify shard manager
        getContext().parent().tell(new ActorInitialized(), getSelf());

        // Being paranoid here - this method should only be called once but just in case...
        if(txCommitTimeoutCheckSchedule == null) {
            // Schedule a message to be periodically sent to check if the current in-progress
            // transaction should be expired and aborted.
            FiniteDuration period = Duration.create(transactionCommitTimeout / 3, TimeUnit.MILLISECONDS);
            txCommitTimeoutCheckSchedule = getContext().system().scheduler().schedule(
                    period, period, getSelf(),
                    TX_COMMIT_TIMEOUT_CHECK_MESSAGE, getContext().dispatcher(), ActorRef.noSender());
        }
    }

    @Override
    protected void applyState(final ActorRef clientActor, final String identifier, final Object data) {
        if (data instanceof DataTreeCandidatePayload) {
            if (clientActor == null) {
                // No clientActor indicates a replica coming from the leader
                try {
                    store.applyForeignCandidate(identifier, ((DataTreeCandidatePayload)data).getCandidate());
                } catch (DataValidationFailedException | IOException e) {
                    LOG.error("{}: Error applying replica {}", persistenceId(), identifier, e);
                }
            } else {
                // Replication consensus reached, proceed to commit
                finishCommit(clientActor, identifier);
            }
        } else if (data instanceof ModificationPayload) {
            try {
                applyModificationToState(clientActor, identifier, ((ModificationPayload) data).getModification());
            } catch (ClassNotFoundException | IOException e) {
                LOG.error("{}: Error extracting ModificationPayload", persistenceId(), e);
            }
        } else if (data instanceof CompositeModificationPayload) {
            Object modification = ((CompositeModificationPayload) data).getModification();

            applyModificationToState(clientActor, identifier, modification);
        } else if(data instanceof CompositeModificationByteStringPayload ){
            Object modification = ((CompositeModificationByteStringPayload) data).getModification();

            applyModificationToState(clientActor, identifier, modification);
        } else {
            LOG.error("{}: Unknown state received {} Class loader = {} CompositeNodeMod.ClassLoader = {}",
                    persistenceId(), data, data.getClass().getClassLoader(),
                    CompositeModificationPayload.class.getClassLoader());
        }
    }

    private void applyModificationToState(ActorRef clientActor, String identifier, Object modification) {
        if(modification == null) {
            LOG.error(
                    "{}: modification is null - this is very unexpected, clientActor = {}, identifier = {}",
                    persistenceId(), identifier, clientActor != null ? clientActor.path().toString() : null);
        } else if(clientActor == null) {
            // There's no clientActor to which to send a commit reply so we must be applying
            // replicated state from the leader.
            commitWithNewTransaction(MutableCompositeModification.fromSerializable(modification));
        } else {
            // This must be the OK to commit after replication consensus.
            finishCommit(clientActor, identifier);
        }
    }

    @Override
    protected void onStateChanged() {
        boolean isLeader = isLeader();
        changeSupport.onLeadershipChange(isLeader);
        treeChangeSupport.onLeadershipChange(isLeader);

        // If this actor is no longer the leader close all the transaction chains
        if (!isLeader) {
            if(LOG.isDebugEnabled()) {
                LOG.debug(
                    "{}: onStateChanged: Closing all transaction chains because shard {} is no longer the leader",
                    persistenceId(), getId());
            }

            store.closeAllTransactionChains();
        }
    }

    @Override
    protected void onLeaderChanged(String oldLeader, String newLeader) {
        shardMBean.incrementLeadershipChangeCount();
    }

    @Override
    public String persistenceId() {
        return this.name;
    }

    @VisibleForTesting
    ShardCommitCoordinator getCommitCoordinator() {
        return commitCoordinator;
    }


    private static class ShardCreator implements Creator<Shard> {

        private static final long serialVersionUID = 1L;

        final ShardIdentifier name;
        final Map<String, String> peerAddresses;
        final DatastoreContext datastoreContext;
        final SchemaContext schemaContext;

        ShardCreator(final ShardIdentifier name, final Map<String, String> peerAddresses,
                final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
            this.name = name;
            this.peerAddresses = peerAddresses;
            this.datastoreContext = datastoreContext;
            this.schemaContext = schemaContext;
        }

        @Override
        public Shard create() throws Exception {
            return new Shard(name, peerAddresses, datastoreContext, schemaContext);
        }
    }

    @VisibleForTesting
    public ShardDataTree getDataStore() {
        return store;
    }

    @VisibleForTesting
    ShardStats getShardMBean() {
        return shardMBean;
    }
}
