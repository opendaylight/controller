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
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardTransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardMBeanFactory;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.ForwardedCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.CompositeModificationPayload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * A Shard represents a portion of the logical data tree <br/>
 * <p>
 * Our Shard uses InMemoryDataStore as it's internal representation and delegates all requests it
 * </p>
 */
public class Shard extends RaftActor {

    public static final String DEFAULT_NAME = "default";

    // The state of this Shard
    private final InMemoryDOMDataStore store;

    private final Map<Object, DOMStoreThreePhaseCommitCohort>
        modificationToCohort = new HashMap<>();

    private final LoggingAdapter LOG =
        Logging.getLogger(getContext().system(), this);

    // By default persistent will be true and can be turned off using the system
    // property shard.persistent
    private final boolean persistent;

    /// The name of this shard
    private final ShardIdentifier name;

    private final ShardStats shardMBean;

    private final List<ActorSelection> dataChangeListeners = new ArrayList<>();

    private final DatastoreContext datastoreContext;

    private SchemaContext schemaContext;

    private ActorRef createSnapshotTransaction;

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

        String setting = System.getProperty("shard.persistent");

        this.persistent = !"false".equals(setting);

        LOG.info("Shard created : {} persistent : {}", name, persistent);

        store = InMemoryDOMDataStoreFactory.create(name.toString(), null,
                datastoreContext.getDataStoreProperties());

        if(schemaContext != null) {
            store.onGlobalContextUpdated(schemaContext);
        }

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name.toString(),
                datastoreContext.getDataStoreMXBeanType());
        shardMBean.setDataStore(store);

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }
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

    @Override public void onReceiveRecover(final Object message) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("onReceiveRecover: Received message {} from {}",
                message.getClass().toString(),
                getSender());
        }

        if (message instanceof RecoveryFailure){
            LOG.error(((RecoveryFailure) message).cause(), "Recovery failed because of this cause");
        } else {
            super.onReceiveRecover(message);
        }
    }

    @Override public void onReceiveCommand(final Object message) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("onReceiveCommand: Received message {} from {}",
                message.getClass().toString(),
                getSender());
        }

        if(message.getClass().equals(ReadDataReply.SERIALIZABLE_CLASS)) {
            // This must be for install snapshot. Don't want to open this up and trigger
            // deSerialization
            self()
                .tell(new CaptureSnapshotReply(ReadDataReply.getNormalizedNodeByteString(message)),
                    self());

            createSnapshotTransaction = null;
            // Send a PoisonPill instead of sending close transaction because we do not really need
            // a response
            getSender().tell(PoisonPill.getInstance(), self());

        } else if (message.getClass().equals(CloseTransactionChain.SERIALIZABLE_CLASS)){
            closeTransactionChain(CloseTransactionChain.fromSerializable(message));
        } else if (message instanceof RegisterChangeListener) {
            registerChangeListener((RegisterChangeListener) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        } else if (message instanceof ForwardedCommitTransaction) {
            handleForwardedCommit((ForwardedCommitTransaction) message);
        } else if (message.getClass()
            .equals(CreateTransaction.SERIALIZABLE_CLASS)) {
            if (isLeader()) {
                createTransaction(CreateTransaction.fromSerializable(message));
            } else if (getLeader() != null) {
                getLeader().forward(message, getContext());
            } else {
                getSender().tell(new akka.actor.Status.Failure(new IllegalStateException(
                    "Could not find leader so transaction cannot be created")), getSelf());
            }
        } else if (message instanceof PeerAddressResolved) {
            PeerAddressResolved resolved = (PeerAddressResolved) message;
            setPeerAddress(resolved.getPeerId().toString(),
                resolved.getPeerAddress());
        } else {
            super.onReceiveCommand(message);
        }
    }

    private void closeTransactionChain(final CloseTransactionChain closeTransactionChain) {
        DOMStoreTransactionChain chain =
            transactionChains.remove(closeTransactionChain.getTransactionChainId());

        if(chain != null) {
            chain.close();
        }
    }

    private ActorRef createTypedTransactionActor(
        final int transactionType,
        final ShardTransactionIdentifier transactionId,
        final String transactionChainId ) {

        DOMStoreTransactionFactory factory = store;

        if(!transactionChainId.isEmpty()) {
            factory = transactionChains.get(transactionChainId);
            if(factory == null){
                DOMStoreTransactionChain transactionChain = store.createTransactionChain();
                transactionChains.put(transactionChainId, transactionChain);
                factory = transactionChain;
            }
        }

        if(this.schemaContext == null){
            throw new NullPointerException("schemaContext should not be null");
        }

        if (transactionType
            == TransactionProxy.TransactionType.READ_ONLY.ordinal()) {

            shardMBean.incrementReadOnlyTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newReadOnlyTransaction(), getSelf(),
                        schemaContext,datastoreContext, shardMBean), transactionId.toString());

        } else if (transactionType
            == TransactionProxy.TransactionType.READ_WRITE.ordinal()) {

            shardMBean.incrementReadWriteTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newReadWriteTransaction(), getSelf(),
                        schemaContext, datastoreContext, shardMBean), transactionId.toString());


        } else if (transactionType
            == TransactionProxy.TransactionType.WRITE_ONLY.ordinal()) {

            shardMBean.incrementWriteOnlyTransactionCount();

            return getContext().actorOf(
                ShardTransaction.props(factory.newWriteOnlyTransaction(), getSelf(),
                        schemaContext, datastoreContext, shardMBean), transactionId.toString());
        } else {
            throw new IllegalArgumentException(
                "Shard="+name + ":CreateTransaction message has unidentified transaction type="
                    + transactionType);
        }
    }

    private void createTransaction(final CreateTransaction createTransaction) {
        createTransaction(createTransaction.getTransactionType(),
            createTransaction.getTransactionId(), createTransaction.getTransactionChainId());
    }

    private ActorRef createTransaction(final int transactionType, final String remoteTransactionId, final String transactionChainId) {

        ShardTransactionIdentifier transactionId =
            ShardTransactionIdentifier.builder()
                .remoteTransactionId(remoteTransactionId)
                .build();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Creating transaction : {} ", transactionId);
        }
        ActorRef transactionActor =
            createTypedTransactionActor(transactionType, transactionId, transactionChainId);

        getSender()
            .tell(new CreateTransactionReply(
                    Serialization.serializedActorPath(transactionActor),
                    remoteTransactionId).toSerializable(),
                getSelf());

        return transactionActor;
    }

    private void syncCommitTransaction(final DOMStoreWriteTransaction transaction)
        throws ExecutionException, InterruptedException {
        DOMStoreThreePhaseCommitCohort commitCohort = transaction.ready();
        commitCohort.preCommit().get();
        commitCohort.commit().get();
    }


    private void commit(final ActorRef sender, final Object serialized) {
        Modification modification = MutableCompositeModification
            .fromSerializable(serialized, schemaContext);
        DOMStoreThreePhaseCommitCohort cohort =
            modificationToCohort.remove(serialized);
        if (cohort == null) {
            // If there's no cached cohort then we must be applying replicated state.
            commitWithNewTransaction(serialized);
            return;
        }

        if(sender == null) {
            LOG.error("Commit failed. Sender cannot be null");
            return;
        }

        ListenableFuture<Void> future = cohort.commit();

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void v) {
                sender.tell(new CommitTransactionReply().toSerializable(), getSelf());
                shardMBean.incrementCommittedTransactionCount();
                shardMBean.setLastCommittedTransactionTime(System.currentTimeMillis());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error(t, "An exception happened during commit");
                shardMBean.incrementFailedTransactionsCount();
                sender.tell(new akka.actor.Status.Failure(t), getSelf());
            }
        });

    }

    private void commitWithNewTransaction(final Object modification) {
        DOMStoreWriteTransaction tx = store.newWriteOnlyTransaction();
        MutableCompositeModification.fromSerializable(modification, schemaContext).apply(tx);
        try {
            syncCommitTransaction(tx);
            shardMBean.incrementCommittedTransactionCount();
        } catch (InterruptedException | ExecutionException e) {
            shardMBean.incrementFailedTransactionsCount();
            LOG.error(e, "Failed to commit");
        }
    }

    private void handleForwardedCommit(final ForwardedCommitTransaction message) {
        Object serializedModification =
            message.getModification().toSerializable();

        modificationToCohort
            .put(serializedModification, message.getCohort());

        if (persistent) {
            this.persistData(getSender(), "identifier",
                new CompositeModificationPayload(serializedModification));
        } else {
            this.commit(getSender(), serializedModification);
        }
    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
        this.schemaContext = message.getSchemaContext();
        updateSchemaContext(message.getSchemaContext());
        store.onGlobalContextUpdated(message.getSchemaContext());
    }

    @VisibleForTesting void updateSchemaContext(final SchemaContext schemaContext) {
        store.onGlobalContextUpdated(schemaContext);
    }

    private void registerChangeListener(
        final RegisterChangeListener registerChangeListener) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("registerDataChangeListener for {}", registerChangeListener
                .getPath());
        }


        ActorSelection dataChangeListenerPath = getContext()
            .system().actorSelection(
                registerChangeListener.getDataChangeListenerPath());


        // Notify the listener if notifications should be enabled or not
        // If this shard is the leader then it will enable notifications else
        // it will not
        dataChangeListenerPath
            .tell(new EnableNotification(isLeader()), getSelf());

        // Now store a reference to the data change listener so it can be notified
        // at a later point if notifications should be enabled or disabled
        dataChangeListeners.add(dataChangeListenerPath);

        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>
            listener = new DataChangeListenerProxy(schemaContext, dataChangeListenerPath);

        ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>>
            registration = store.registerChangeListener(registerChangeListener.getPath(),
                listener, registerChangeListener.getScope());
        ActorRef listenerRegistration =
            getContext().actorOf(
                DataChangeListenerRegistration.props(registration));

        if(LOG.isDebugEnabled()) {
            LOG.debug(
                "registerDataChangeListener sending reply, listenerRegistrationPath = {} "
                , listenerRegistration.path().toString());
        }

        getSender()
            .tell(new RegisterChangeListenerReply(listenerRegistration.path()),
                getSelf());
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
    }

    @Override
    protected void applyState(final ActorRef clientActor, final String identifier, final Object data) {

        if (data instanceof CompositeModificationPayload) {
            Object modification = ((CompositeModificationPayload) data).getModification();

            if (modification != null) {
                commit(clientActor, modification);
            } else {
                LOG.error(
                    "modification is null - this is very unexpected, clientActor = {}, identifier = {}",
                    identifier, clientActor != null ? clientActor.path().toString() : null);
            }

        } else {
            LOG.error("Unknown state received {} Class loader = {} CompositeNodeMod.ClassLoader = {}",
                    data, data.getClass().getClassLoader(),
                    CompositeModificationPayload.class.getClassLoader());
        }

        updateJournalStats();

    }

    private void updateJournalStats() {
        ReplicatedLogEntry lastLogEntry = getLastLogEntry();

        if (lastLogEntry != null) {
            shardMBean.setLastLogIndex(lastLogEntry.getIndex());
            shardMBean.setLastLogTerm(lastLogEntry.getTerm());
        }

        shardMBean.setCommitIndex(getCommitIndex());
        shardMBean.setLastApplied(getLastApplied());
    }

    @Override
    protected void createSnapshot() {
        if (createSnapshotTransaction == null) {

            // Create a transaction. We are really going to treat the transaction as a worker
            // so that this actor does not get block building the snapshot
            createSnapshotTransaction = createTransaction(
                TransactionProxy.TransactionType.READ_ONLY.ordinal(),
                "createSnapshot", "");

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
                .decode(YangInstanceIdentifier.builder().build(), serializedNode);

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

    @Override protected void onStateChanged() {
        for (ActorSelection dataChangeListener : dataChangeListeners) {
            dataChangeListener
                .tell(new EnableNotification(isLeader()), getSelf());
        }

        shardMBean.setRaftState(getRaftState().name());
        shardMBean.setCurrentTerm(getCurrentTerm());

        // If this actor is no longer the leader close all the transaction chains
        if(!isLeader()){
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

    @Override protected void onLeaderChanged(final String oldLeader, final String newLeader) {
        shardMBean.setLeader(newLeader);
    }

    @Override public String persistenceId() {
        return this.name.toString();
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
    NormalizedNode<?,?> readStore(final YangInstanceIdentifier id)
            throws ExecutionException, InterruptedException {
        DOMStoreReadTransaction transaction = store.newReadOnlyTransaction();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> future =
            transaction.read(id);

        Optional<NormalizedNode<?, ?>> optional = future.get();
        NormalizedNode<?, ?> node = optional.isPresent()? optional.get() : null;

        transaction.close();

        return node;
    }

    @VisibleForTesting
    void writeToStore(final YangInstanceIdentifier id, final NormalizedNode<?,?> node)
        throws ExecutionException, InterruptedException {
        DOMStoreWriteTransaction transaction = store.newWriteOnlyTransaction();

        transaction.write(id, node);

        syncCommitTransaction(transaction);
    }

    @VisibleForTesting
    ShardStats getShardMBean() {
        return shardMBean;
    }
}
