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
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.persistence.RecoveryFailure;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.ShardCommitCoordinator.CohortEntry;
import org.opendaylight.controller.cluster.datastore.compat.BackwardsCompatibleThreePhaseCommitCohort;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardMBeanFactory;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationByteStringPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * A Shard represents a portion of the logical data tree <br/>
 * <p>
 * Our Shard uses InMemoryDataStore as it's internal representation and delegates all requests it
 * </p>
 */
public class Shard extends RaftActor {

    private static final Object COMMIT_TRANSACTION_REPLY = new CommitTransactionReply().toSerializable();

    private static final Object TX_COMMIT_TIMEOUT_CHECK_MESSAGE = "txCommitTimeoutCheck";

    public static final String DEFAULT_NAME = "default";

    // The state of this Shard
    private final InMemoryDOMDataStore store;

    private final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    /// The name of this shard
    private final ShardIdentifier name;

    private final ShardStats shardMBean;

    private final List<ActorSelection> dataChangeListeners =  Lists.newArrayList();

    private final List<DelayedListenerRegistration> delayedListenerRegistrations =
                                                                       Lists.newArrayList();

    private final DatastoreContext datastoreContext;

    private final DataPersistenceProvider dataPersistenceProvider;

    private SchemaContext schemaContext;

    private ActorRef createSnapshotTransaction;

    private int createSnapshotTransactionCounter;

    private final ShardCommitCoordinator commitCoordinator;

    private final long transactionCommitTimeout;

    private Cancellable txCommitTimeoutCheckSchedule;

    private Optional<ActorRef> roleChangeNotifier;

    /**
     * Coordinates persistence recovery on startup.
     */
    private ShardRecoveryCoordinator recoveryCoordinator;
    private List<Object> currentLogRecoveryBatch;

    private final Map<String, DOMStoreTransactionChain> transactionChains = new HashMap<>();

    protected Shard(final ShardIdentifier name, final Map<ShardIdentifier, String> peerAddresses,
            final DatastoreContext datastoreContext, final SchemaContext schemaContext) {
        super(name.toString(), mapPeerAddresses(peerAddresses),
                Optional.of(datastoreContext.getShardRaftConfig()));

        this.name = name;
        this.datastoreContext = datastoreContext;
        this.schemaContext = schemaContext;
        this.dataPersistenceProvider = (datastoreContext.isPersistent()) ? new PersistentDataProvider() : new NonPersistentRaftDataProvider();

        LOG.info("Shard created : {} persistent : {}", name, datastoreContext.isPersistent());

        store = InMemoryDOMDataStoreFactory.create(name.toString(), false, null,
                datastoreContext.getDataStoreProperties());

        if(schemaContext != null) {
            store.onGlobalContextUpdated(schemaContext);
        }

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name.toString(),
                datastoreContext.getDataStoreMXBeanType());
        shardMBean.setNotificationManager(store.getDataChangeListenerNotificationManager());

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }

        commitCoordinator = new ShardCommitCoordinator(TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES),
                datastoreContext.getShardTransactionCommitQueueCapacity());

        transactionCommitTimeout = TimeUnit.MILLISECONDS.convert(
                datastoreContext.getShardTransactionCommitTimeoutInSeconds(), TimeUnit.SECONDS);

        // create a notifier actor for each cluster member
        roleChangeNotifier = createRoleChangeNotifier(name.toString());
    }

    private static Map<String, String> mapPeerAddresses(
        final Map<ShardIdentifier, String> peerAddresses) {
        Map<String, String> map = new HashMap<>();

        for (Map.Entry<ShardIdentifier, String> entry : peerAddresses
            .entrySet()) {
            map.put(entry.getKey().toString(), entry.getValue());
        }

        return map;
    }

    public static Props props(final ShardIdentifier name,
        final Map<ShardIdentifier, String> peerAddresses,
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
        return Optional.<ActorRef>of(shardRoleChangeNotifier);
    }

    @Override
    public void postStop() {
        super.postStop();

        if(txCommitTimeoutCheckSchedule != null) {
            txCommitTimeoutCheckSchedule.cancel();
        }
    }

    @Override
    public void onReceiveRecover(final Object message) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("onReceiveRecover: Received message {} from {}",
                message.getClass().toString(),
                getSender());
        }

        if (message instanceof RecoveryFailure){
            LOG.error(((RecoveryFailure) message).cause(), "Recovery failed because of this cause");

            // Even though recovery failed, we still need to finish our recovery, eg send the
            // ActorInitialized message and start the txCommitTimeoutCheckSchedule.
            onRecoveryComplete();
        } else {
            super.onReceiveRecover(message);
        }
    }

    @Override
    public void onReceiveCommand(final Object message) throws Exception {
        if(LOG.isDebugEnabled()) {
            LOG.debug("onReceiveCommand: Received message {} from {}", message, getSender());
        }

        if(message.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
            handleReadDataReply(message);
        } else if (message.getClass().equals(CreateTransaction.SERIALIZABLE_CLASS)) {
            handleCreateTransaction(message);
        } else if(message instanceof ForwardedReadyTransaction) {
            handleForwardedReadyTransaction((ForwardedReadyTransaction)message);
        } else if(message.getClass().equals(CanCommitTransaction.SERIALIZABLE_CLASS)) {
            handleCanCommitTransaction(CanCommitTransaction.fromSerializable(message));
        } else if(message.getClass().equals(CommitTransaction.SERIALIZABLE_CLASS)) {
            handleCommitTransaction(CommitTransaction.fromSerializable(message));
        } else if(message.getClass().equals(AbortTransaction.SERIALIZABLE_CLASS)) {
            handleAbortTransaction(AbortTransaction.fromSerializable(message));
        } else if (message.getClass().equals(CloseTransactionChain.SERIALIZABLE_CLASS)){
            closeTransactionChain(CloseTransactionChain.fromSerializable(message));
        } else if (message instanceof RegisterChangeListener) {
            registerChangeListener((RegisterChangeListener) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        } else if (message instanceof PeerAddressResolved) {
            PeerAddressResolved resolved = (PeerAddressResolved) message;
            setPeerAddress(resolved.getPeerId().toString(),
                resolved.getPeerAddress());
        } else if(message.equals(TX_COMMIT_TIMEOUT_CHECK_MESSAGE)) {
            handleTransactionCommitTimeoutCheck();
        } else {
            super.onReceiveCommand(message);
        }
    }

    @Override
    protected Optional<ActorRef> getRoleChangeNotifier() {
        return roleChangeNotifier;
    }

    private void handleTransactionCommitTimeoutCheck() {
        CohortEntry cohortEntry = commitCoordinator.getCurrentCohortEntry();
        if(cohortEntry != null) {
            long elapsed = System.currentTimeMillis() - cohortEntry.getLastAccessTime();
            if(elapsed > transactionCommitTimeout) {
                LOG.warning("Current transaction {} has timed out after {} ms - aborting",
                        cohortEntry.getTransactionID(), transactionCommitTimeout);

                doAbortTransaction(cohortEntry.getTransactionID(), null);
            }
        }
    }

    private void handleCommitTransaction(final CommitTransaction commit) {
        final String transactionID = commit.getTransactionID();

        LOG.debug("Committing transaction {}", transactionID);

        // Get the current in-progress cohort entry in the commitCoordinator if it corresponds to
        // this transaction.
        final CohortEntry cohortEntry = commitCoordinator.getCohortEntryIfCurrent(transactionID);
        if(cohortEntry == null) {
            // We're not the current Tx - the Tx was likely expired b/c it took too long in
            // between the canCommit and commit messages.
            IllegalStateException ex = new IllegalStateException(
                    String.format("Cannot commit transaction %s - it is not the current transaction",
                            transactionID));
            LOG.error(ex.getMessage());
            shardMBean.incrementFailedTransactionsCount();
            getSender().tell(new akka.actor.Status.Failure(ex), getSelf());
            return;
        }

        // We perform the preCommit phase here atomically with the commit phase. This is an
        // optimization to eliminate the overhead of an extra preCommit message. We lose front-end
        // coordination of preCommit across shards in case of failure but preCommit should not
        // normally fail since we ensure only one concurrent 3-phase commit.

        try {
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            cohortEntry.getCohort().preCommit().get();

            // If we do not have any followers and we are not using persistence we can
            // apply modification to the state immediately
            if(!hasFollowers() && !persistence().isRecoveryApplicable()){
                applyModificationToState(getSender(), transactionID, cohortEntry.getModification());
            } else {
                Shard.this.persistData(getSender(), transactionID,
                        new CompositeModificationByteStringPayload(cohortEntry.getModification().toSerializable()));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e, "An exception occurred while preCommitting transaction {}",
                    cohortEntry.getTransactionID());
            shardMBean.incrementFailedTransactionsCount();
            getSender().tell(new akka.actor.Status.Failure(e), getSelf());
        }

        cohortEntry.updateLastAccessTime();
    }

    private void finishCommit(@Nonnull final ActorRef sender, final @Nonnull String transactionID) {
        // With persistence enabled, this method is called via applyState by the leader strategy
        // after the commit has been replicated to a majority of the followers.

        CohortEntry cohortEntry = commitCoordinator.getCohortEntryIfCurrent(transactionID);
        if(cohortEntry == null) {
            // The transaction is no longer the current commit. This can happen if the transaction
            // was aborted prior, most likely due to timeout in the front-end. We need to finish
            // committing the transaction though since it was successfully persisted and replicated
            // however we can't use the original cohort b/c it was already preCommitted and may
            // conflict with the current commit or may have been aborted so we commit with a new
            // transaction.
            cohortEntry = commitCoordinator.getAndRemoveCohortEntry(transactionID);
            if(cohortEntry != null) {
                commitWithNewTransaction(cohortEntry.getModification());
                sender.tell(COMMIT_TRANSACTION_REPLY, getSelf());
            } else {
                // This really shouldn't happen - it likely means that persistence or replication
                // took so long to complete such that the cohort entry was expired from the cache.
                IllegalStateException ex = new IllegalStateException(
                        String.format("Could not finish committing transaction %s - no CohortEntry found",
                                transactionID));
                LOG.error(ex.getMessage());
                sender.tell(new akka.actor.Status.Failure(ex), getSelf());
            }

            return;
        }

        LOG.debug("Finishing commit for transaction {}", cohortEntry.getTransactionID());

        try {
            // We block on the future here so we don't have to worry about possibly accessing our
            // state on a different thread outside of our dispatcher. Also, the data store
            // currently uses a same thread executor anyway.
            cohortEntry.getCohort().commit().get();

            sender.tell(COMMIT_TRANSACTION_REPLY, getSelf());

            shardMBean.incrementCommittedTransactionCount();
            shardMBean.setLastCommittedTransactionTime(System.currentTimeMillis());

        } catch (InterruptedException | ExecutionException e) {
            sender.tell(new akka.actor.Status.Failure(e), getSelf());

            LOG.error(e, "An exception occurred while committing transaction {}", transactionID);
            shardMBean.incrementFailedTransactionsCount();
        }

        commitCoordinator.currentTransactionComplete(transactionID, true);
    }

    private void handleCanCommitTransaction(final CanCommitTransaction canCommit) {
        LOG.debug("Can committing transaction {}", canCommit.getTransactionID());
        commitCoordinator.handleCanCommit(canCommit, getSender(), self());
    }

    private void handleForwardedReadyTransaction(ForwardedReadyTransaction ready) {
        LOG.debug("Readying transaction {}, client version {}", ready.getTransactionID(),
                ready.getTxnClientVersion());

        // This message is forwarded by the ShardTransaction on ready. We cache the cohort in the
        // commitCoordinator in preparation for the subsequent three phase commit initiated by
        // the front-end.
        commitCoordinator.transactionReady(ready.getTransactionID(), ready.getCohort(),
                ready.getModification());

        // Return our actor path as we'll handle the three phase commit, except if the Tx client
        // version < 1 (Helium-1 version). This means the Tx was initiated by a base Helium version
        // node. In that case, the subsequent 3-phase commit messages won't contain the
        // transactionId so to maintain backwards compatibility, we create a separate cohort actor
        // to provide the compatible behavior.
        ActorRef replyActorPath = self();
        if(ready.getTxnClientVersion() < CreateTransaction.HELIUM_1_VERSION) {
            LOG.debug("Creating BackwardsCompatibleThreePhaseCommitCohort");
            replyActorPath = getContext().actorOf(BackwardsCompatibleThreePhaseCommitCohort.props(
                    ready.getTransactionID()));
        }

        ReadyTransactionReply readyTransactionReply = new ReadyTransactionReply(
                Serialization.serializedActorPath(replyActorPath));
        getSender().tell(ready.isReturnSerialized() ? readyTransactionReply.toSerializable() :
                readyTransactionReply, getSelf());
    }

    private void handleAbortTransaction(final AbortTransaction abort) {
        doAbortTransaction(abort.getTransactionID(), getSender());
    }

    private void doAbortTransaction(final String transactionID, final ActorRef sender) {
        final CohortEntry cohortEntry = commitCoordinator.getCohortEntryIfCurrent(transactionID);
        if(cohortEntry != null) {
            LOG.debug("Aborting transaction {}", transactionID);

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
                        sender.tell(new AbortTransactionReply().toSerializable(), self);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error(t, "An exception happened during abort");

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
            getSender().tell(new akka.actor.Status.Failure(new NoShardLeaderException(
                "Could not find shard leader so transaction cannot be created. This typically happens" +
                " when the system is coming up or recovering and a leader is being elected. Try again" +
                " later.")), getSelf());
        }
    }

    private void handleReadDataReply(final Object message) {
        // This must be for install snapshot. Don't want to open this up and trigger
        // deSerialization

        self().tell(new CaptureSnapshotReply(ReadDataReply.getNormalizedNodeByteString(message)),
                self());

        createSnapshotTransaction = null;

        // Send a PoisonPill instead of sending close transaction because we do not really need
        // a response
        getSender().tell(PoisonPill.getInstance(), self());
    }

    private void closeTransactionChain(final CloseTransactionChain closeTransactionChain) {
        DOMStoreTransactionChain chain =
            transactionChains.remove(closeTransactionChain.getTransactionChainId());

        if(chain != null) {
            chain.close();
        }
    }

    private ActorRef createTypedTransactionActor(int transactionType,
            ShardTransactionIdentifier transactionId, String transactionChainId, int clientVersion ) {

        DOMStoreTransactionFactory factory = store;

        if(!transactionChainId.isEmpty()) {
            factory = transactionChains.get(transactionChainId);
            if(factory == null){
                DOMStoreTransactionChain transactionChain = store.createTransactionChain();
                transactionChains.put(transactionChainId, transactionChain);
                factory = transactionChain;
            }
        }

        if(this.schemaContext == null) {
            throw new IllegalStateException("SchemaContext is not set");
        }

        if (transactionType == TransactionProxy.TransactionType.READ_ONLY.ordinal()) {

            shardMBean.incrementReadOnlyTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newReadOnlyTransaction(), getSelf(),
                        schemaContext,datastoreContext, shardMBean,
                        transactionId.getRemoteTransactionId(), clientVersion),
                        transactionId.toString());

        } else if (transactionType == TransactionProxy.TransactionType.READ_WRITE.ordinal()) {

            shardMBean.incrementReadWriteTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newReadWriteTransaction(), getSelf(),
                        schemaContext, datastoreContext, shardMBean,
                        transactionId.getRemoteTransactionId(), clientVersion),
                        transactionId.toString());


        } else if (transactionType == TransactionProxy.TransactionType.WRITE_ONLY.ordinal()) {

            shardMBean.incrementWriteOnlyTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newWriteOnlyTransaction(), getSelf(),
                        schemaContext, datastoreContext, shardMBean,
                        transactionId.getRemoteTransactionId(), clientVersion),
                        transactionId.toString());
        } else {
            throw new IllegalArgumentException(
                "Shard="+name + ":CreateTransaction message has unidentified transaction type="
                    + transactionType);
        }
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
            String transactionChainId, int clientVersion) {

        ShardTransactionIdentifier transactionId =
            ShardTransactionIdentifier.builder()
                .remoteTransactionId(remoteTransactionId)
                .build();

        if(LOG.isDebugEnabled()) {
            LOG.debug("Creating transaction : {} ", transactionId);
        }

        ActorRef transactionActor = createTypedTransactionActor(transactionType, transactionId,
                transactionChainId, clientVersion);

        return transactionActor;
    }

    private void syncCommitTransaction(final DOMStoreWriteTransaction transaction)
        throws ExecutionException, InterruptedException {
        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        commitCohort.preCommit().get();
        commitCohort.commit().get();
    }

    private void commitWithNewTransaction(final Modification modification) {
        DOMStoreWriteTransaction tx = store.newWriteOnlyTransaction();
        modification.apply(tx);
        try {
            syncCommitTransaction(tx);
            shardMBean.incrementCommittedTransactionCount();
            shardMBean.setLastCommittedTransactionTime(System.currentTimeMillis());
        } catch (InterruptedException | ExecutionException e) {
            shardMBean.incrementFailedTransactionsCount();
            LOG.error(e, "Failed to commit");
        }
    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
        this.schemaContext = message.getSchemaContext();
        updateSchemaContext(message.getSchemaContext());
        store.onGlobalContextUpdated(message.getSchemaContext());
    }

    @VisibleForTesting
    void updateSchemaContext(final SchemaContext schemaContext) {
        store.onGlobalContextUpdated(schemaContext);
    }

    private void registerChangeListener(final RegisterChangeListener registerChangeListener) {

        LOG.debug("registerDataChangeListener for {}", registerChangeListener.getPath());

        ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                                     NormalizedNode<?, ?>>> registration;
        if(isLeader()) {
            registration = doChangeListenerRegistration(registerChangeListener);
        } else {
            LOG.debug("Shard is not the leader - delaying registration");

            DelayedListenerRegistration delayedReg =
                    new DelayedListenerRegistration(registerChangeListener);
            delayedListenerRegistrations.add(delayedReg);
            registration = delayedReg;
        }

        ActorRef listenerRegistration = getContext().actorOf(
                DataChangeListenerRegistration.props(registration));

        LOG.debug("registerDataChangeListener sending reply, listenerRegistrationPath = {} ",
                    listenerRegistration.path());

        getSender().tell(new RegisterChangeListenerReply(listenerRegistration.path()), getSelf());
    }

    private ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                               NormalizedNode<?, ?>>> doChangeListenerRegistration(
            final RegisterChangeListener registerChangeListener) {

        ActorSelection dataChangeListenerPath = getContext().system().actorSelection(
                registerChangeListener.getDataChangeListenerPath());

        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath.tell(new EnableNotification(true), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        dataChangeListeners.add(dataChangeListenerPath);

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener =
                new DataChangeListenerProxy(schemaContext, dataChangeListenerPath);

        LOG.debug("Registering for path {}", registerChangeListener.getPath());

        return store.registerChangeListener(registerChangeListener.getPath(), listener,
                registerChangeListener.getScope());
    }

    private boolean isMetricsCaptureEnabled(){
        CommonConfig config = new CommonConfig(getContext().system().settings().config());
        return config.isMetricCaptureEnabled();
    }

    @Override
    protected
    void startLogRecoveryBatch(final int maxBatchSize) {
        currentLogRecoveryBatch = Lists.newArrayListWithCapacity(maxBatchSize);

        if(LOG.isDebugEnabled()) {
            LOG.debug("{} : starting log recovery batch with max size {}", persistenceId(), maxBatchSize);
        }
    }

    @Override
    protected void appendRecoveredLogEntry(final Payload data) {
        if (data instanceof CompositeModificationPayload) {
            currentLogRecoveryBatch.add(((CompositeModificationPayload) data).getModification());
        } else if (data instanceof CompositeModificationByteStringPayload) {
            currentLogRecoveryBatch.add(((CompositeModificationByteStringPayload) data).getModification());
        } else {
            LOG.error("Unknown state received {} during recovery", data);
        }
    }

    @Override
    protected void applyRecoverySnapshot(final ByteString snapshot) {
        if(recoveryCoordinator == null) {
            recoveryCoordinator = new ShardRecoveryCoordinator(persistenceId(), schemaContext);
        }

        recoveryCoordinator.submit(snapshot, store.newWriteOnlyTransaction());

        if(LOG.isDebugEnabled()) {
            LOG.debug("{} : submitted recovery sbapshot", persistenceId());
        }
    }

    @Override
    protected void applyCurrentLogRecoveryBatch() {
        if(recoveryCoordinator == null) {
            recoveryCoordinator = new ShardRecoveryCoordinator(persistenceId(), schemaContext);
        }

        recoveryCoordinator.submit(currentLogRecoveryBatch, store.newWriteOnlyTransaction());

        if(LOG.isDebugEnabled()) {
            LOG.debug("{} : submitted log recovery batch with size {}", persistenceId(),
                    currentLogRecoveryBatch.size());
        }
    }

    @Override
    protected void onRecoveryComplete() {
        if(recoveryCoordinator != null) {
            Collection<DOMStoreWriteTransaction> txList = recoveryCoordinator.getTransactions();

            if(LOG.isDebugEnabled()) {
                LOG.debug("{} : recovery complete - committing {} Tx's", persistenceId(), txList.size());
            }

            for(DOMStoreWriteTransaction tx: txList) {
                try {
                    syncCommitTransaction(tx);
                    shardMBean.incrementCommittedTransactionCount();
                } catch (InterruptedException | ExecutionException e) {
                    shardMBean.incrementFailedTransactionsCount();
                    LOG.error(e, "Failed to commit");
                }
            }
        }

        recoveryCoordinator = null;
        currentLogRecoveryBatch = null;
        updateJournalStats();

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

        if (data instanceof CompositeModificationPayload) {
            Object modification = ((CompositeModificationPayload) data).getModification();

            applyModificationToState(clientActor, identifier, modification);
        } else if(data instanceof CompositeModificationByteStringPayload ){
            Object modification = ((CompositeModificationByteStringPayload) data).getModification();

            applyModificationToState(clientActor, identifier, modification);

        } else {
            LOG.error("Unknown state received {} Class loader = {} CompositeNodeMod.ClassLoader = {}",
                    data, data.getClass().getClassLoader(),
                    CompositeModificationPayload.class.getClassLoader());
        }

        updateJournalStats();

    }

    private void applyModificationToState(ActorRef clientActor, String identifier, Object modification) {
        if(modification == null) {
            LOG.error(
                    "modification is null - this is very unexpected, clientActor = {}, identifier = {}",
                    identifier, clientActor != null ? clientActor.path().toString() : null);
        } else if(clientActor == null) {
            // There's no clientActor to which to send a commit reply so we must be applying
            // replicated state from the leader.
            commitWithNewTransaction(MutableCompositeModification.fromSerializable(
                    modification, schemaContext));
        } else {
            // This must be the OK to commit after replication consensus.
            finishCommit(clientActor, identifier);
        }
    }

    private void updateJournalStats() {
        ReplicatedLogEntry lastLogEntry = getLastLogEntry();

        if (lastLogEntry != null) {
            shardMBean.setLastLogIndex(lastLogEntry.getIndex());
            shardMBean.setLastLogTerm(lastLogEntry.getTerm());
        }

        shardMBean.setCommitIndex(getCommitIndex());
        shardMBean.setLastApplied(getLastApplied());
        shardMBean.setInMemoryJournalDataSize(getRaftActorContext().getReplicatedLog().dataSize());
    }

    @Override
    protected void createSnapshot() {
        if (createSnapshotTransaction == null) {

            // Create a transaction. We are really going to treat the transaction as a worker
            // so that this actor does not get block building the snapshot
            createSnapshotTransaction = createTransaction(
                TransactionProxy.TransactionType.READ_ONLY.ordinal(),
                "createSnapshot" + ++createSnapshotTransactionCounter, "",
                CreateTransaction.CURRENT_VERSION);

            createSnapshotTransaction.tell(
                new ReadData(YangInstanceIdentifier.builder().build()).toSerializable(), self());

        }
    }

    @VisibleForTesting
    @Override
    protected void applySnapshot(final ByteString snapshot) {
        // Since this will be done only on Recovery or when this actor is a Follower
        // we can safely commit everything in here. We not need to worry about event notifications
        // as they would have already been disabled on the follower

        LOG.info("Applying snapshot");
        try {
            DOMStoreWriteTransaction transaction = store.newWriteOnlyTransaction();
            NormalizedNodeMessages.Node serializedNode = NormalizedNodeMessages.Node.parseFrom(snapshot);
            NormalizedNode<?, ?> node = new NormalizedNodeToNodeCodec(schemaContext)
                .decode(serializedNode);

            // delete everything first
            transaction.delete(YangInstanceIdentifier.builder().build());

            // Add everything from the remote node back
            transaction.write(YangInstanceIdentifier.builder().build(), node);
            syncCommitTransaction(transaction);
        } catch (InvalidProtocolBufferException | InterruptedException | ExecutionException e) {
            LOG.error(e, "An exception occurred when applying snapshot");
        } finally {
            LOG.info("Done applying snapshot");
        }
    }

    @Override
    protected void onStateChanged() {
        boolean isLeader = isLeader();
        for (ActorSelection dataChangeListener : dataChangeListeners) {
            dataChangeListener.tell(new EnableNotification(isLeader), getSelf());
        }

        if(isLeader) {
            for(DelayedListenerRegistration reg: delayedListenerRegistrations) {
                if(!reg.isClosed()) {
                    reg.setDelegate(doChangeListenerRegistration(reg.getRegisterChangeListener()));
                }
            }

            delayedListenerRegistrations.clear();
        }

        shardMBean.setRaftState(getRaftState().name());
        shardMBean.setCurrentTerm(getCurrentTerm());

        // If this actor is no longer the leader close all the transaction chains
        if(!isLeader){
            for(Map.Entry<String, DOMStoreTransactionChain> entry : transactionChains.entrySet()){
                if(LOG.isDebugEnabled()) {
                    LOG.debug(
                        "onStateChanged: Closing transaction chain {} because shard {} is no longer the leader",
                        entry.getKey(), getId());
                }
                entry.getValue().close();
            }

            transactionChains.clear();
        }
    }

    @Override
    protected DataPersistenceProvider persistence() {
        return dataPersistenceProvider;
    }

    @Override protected void onLeaderChanged(final String oldLeader, final String newLeader) {
        shardMBean.setLeader(newLeader);
    }

    @Override public String persistenceId() {
        return this.name.toString();
    }

    @VisibleForTesting
    DataPersistenceProvider getDataPersistenceProvider() {
        return dataPersistenceProvider;
    }

    private static class ShardCreator implements Creator<Shard> {

        private static final long serialVersionUID = 1L;

        final ShardIdentifier name;
        final Map<ShardIdentifier, String> peerAddresses;
        final DatastoreContext datastoreContext;
        final SchemaContext schemaContext;

        ShardCreator(final ShardIdentifier name, final Map<ShardIdentifier, String> peerAddresses,
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
    InMemoryDOMDataStore getDataStore() {
        return store;
    }

    @VisibleForTesting
    ShardStats getShardMBean() {
        return shardMBean;
    }

    private static class DelayedListenerRegistration implements
        ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> {

        private volatile boolean closed;

        private final RegisterChangeListener registerChangeListener;

        private volatile ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                                             NormalizedNode<?, ?>>> delegate;

        DelayedListenerRegistration(final RegisterChangeListener registerChangeListener) {
            this.registerChangeListener = registerChangeListener;
        }

        void setDelegate( final ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                            NormalizedNode<?, ?>>> registration) {
            this.delegate = registration;
        }

        boolean isClosed() {
            return closed;
        }

        RegisterChangeListener getRegisterChangeListener() {
            return registerChangeListener;
        }

        @Override
        public AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> getInstance() {
            return delegate != null ? delegate.getInstance() : null;
        }

        @Override
        public void close() {
            closed = true;
            if(delegate != null) {
                delegate.close();
            }
        }
    }
}
