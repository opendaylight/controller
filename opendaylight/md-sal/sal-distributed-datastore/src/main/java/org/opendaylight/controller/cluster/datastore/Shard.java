/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.ExtendedActorSystem;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.Status.Failure;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientSuccess;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.commands.OutOfSequenceEnvelopeException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.common.actor.MessageTracker;
import org.opendaylight.controller.cluster.common.actor.MessageTracker.Error;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardDataTreeListenerInfoMXBeanImpl;
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
import org.opendaylight.controller.cluster.datastore.messages.ForwardedReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClients;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClientsReply;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot.ShardSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.DisableTrackingPayload;
import org.opendaylight.controller.cluster.messaging.MessageAssembler;
import org.opendaylight.controller.cluster.messaging.MessageSlicer;
import org.opendaylight.controller.cluster.messaging.SliceOptions;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.raft.LeadershipTransferFailedException;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadership;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import scala.concurrent.duration.FiniteDuration;

/**
 * A Shard represents a portion of the logical data tree.
 *
 * <p>
 * Our Shard uses InMemoryDataTree as it's internal representation and delegates all requests it
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

    static final Object RESUME_NEXT_PENDING_TRANSACTION = new Object() {
        @Override
        public String toString() {
            return "resumeNextPendingTransaction";
        }
    };

    // FIXME: shard names should be encapsulated in their own class and this should be exposed as a constant.
    public static final String DEFAULT_NAME = "default";

    private static final Collection<ABIVersion> SUPPORTED_ABIVERSIONS;

    static {
        final ABIVersion[] values = ABIVersion.values();
        final ABIVersion[] real = Arrays.copyOfRange(values, 1, values.length - 1);
        SUPPORTED_ABIVERSIONS = ImmutableList.copyOf(real).reverse();
    }

    // FIXME: make this a dynamic property based on mailbox size and maximum number of clients
    private static final int CLIENT_MAX_MESSAGES = 1000;

    // The state of this Shard
    private final ShardDataTree store;

    /// The name of this shard
    private final String name;

    private final String shardName;

    private final ShardStats shardMBean;

    private final ActorRef shardManagerRef;

    private final ShardDataTreeListenerInfoMXBeanImpl listenerInfoMXBean;

    private DatastoreContext datastoreContext;

    private final ShardCommitCoordinator commitCoordinator;

    private long transactionCommitTimeout;

    private Cancellable txCommitTimeoutCheckSchedule;

    private final Optional<ActorRef> roleChangeNotifier;

    private final MessageTracker appendEntriesReplyTracker;

    private final ShardTransactionActorFactory transactionActorFactory;

    private final ShardSnapshotCohort snapshotCohort;

    private final DataTreeChangeListenerSupport treeChangeSupport = new DataTreeChangeListenerSupport(this);

    private ShardSnapshot restoreFromSnapshot;

    private final ShardTransactionMessageRetrySupport messageRetrySupport;

    @VisibleForTesting
    final FrontendMetadata frontendMetadata;

    private Map<FrontendIdentifier, LeaderFrontendState> knownFrontends = ImmutableMap.of();
    private boolean paused;

    private final MessageSlicer responseMessageSlicer;
    private final Dispatchers dispatchers;

    private final MessageAssembler requestMessageAssembler;

    protected Shard(final AbstractBuilder<?, ?> builder) {
        super(builder.getId().toString(), builder.getPeerAddresses(),
                Optional.of(builder.getDatastoreContext().getShardRaftConfig()),
                DataStoreVersions.CURRENT_VERSION, builder.isBackoffSupervised());

        this.name = builder.getId().toString();
        this.shardName = builder.getId().getShardName();
        this.datastoreContext = builder.getDatastoreContext();
        this.restoreFromSnapshot = builder.getRestoreFromSnapshot();
        this.shardManagerRef = builder.getShardManagerRef();
        this.frontendMetadata = new FrontendMetadata(name);

        setPersistence(datastoreContext.isPersistent());

        LOG.info("Shard created : {}, persistent : {}", name, datastoreContext.isPersistent());

        ShardDataTreeChangeListenerPublisherActorProxy treeChangeListenerPublisher =
                new ShardDataTreeChangeListenerPublisherActorProxy(getContext(), name + "-DTCL-publisher", name);
        if (builder.getDataTree() != null) {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getDataTree(),
                    treeChangeListenerPublisher, name, frontendMetadata);
        } else {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getTreeType(),
                    builder.getDatastoreContext().getStoreRoot(), treeChangeListenerPublisher, name, frontendMetadata);
        }

        shardMBean = ShardMBeanFactory.getShardStatsMBean(name, datastoreContext.getDataStoreMXBeanType(), this);

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }

        commitCoordinator = new ShardCommitCoordinator(store, LOG, this.name);

        setTransactionCommitTimeout();

        // create a notifier actor for each cluster member
        roleChangeNotifier = createRoleChangeNotifier(name);

        appendEntriesReplyTracker = new MessageTracker(AppendEntriesReply.class,
                getRaftActorContext().getConfigParams().getIsolatedCheckIntervalInMillis());

        dispatchers = new Dispatchers(context().system().dispatchers());
        transactionActorFactory = new ShardTransactionActorFactory(store, datastoreContext,
            dispatchers.getDispatcherPath(Dispatchers.DispatcherType.Transaction),
                self(), getContext(), shardMBean, builder.getId().getShardName());

        snapshotCohort = ShardSnapshotCohort.create(getContext(), builder.getId().getMemberName(), store, LOG,
            this.name);

        messageRetrySupport = new ShardTransactionMessageRetrySupport(this);

        responseMessageSlicer = MessageSlicer.builder().logContext(this.name)
                .messageSliceSize(datastoreContext.getMaximumMessageSliceSize())
                .fileBackedStreamFactory(getRaftActorContext().getFileBackedOutputStreamFactory())
                .expireStateAfterInactivity(2, TimeUnit.MINUTES).build();

        requestMessageAssembler = MessageAssembler.builder().logContext(this.name)
                .fileBackedStreamFactory(getRaftActorContext().getFileBackedOutputStreamFactory())
                .assembledMessageCallback((message, sender) -> self().tell(message, sender))
                .expireStateAfterInactivity(datastoreContext.getRequestTimeout(), TimeUnit.NANOSECONDS).build();

        listenerInfoMXBean = new ShardDataTreeListenerInfoMXBeanImpl(name, datastoreContext.getDataStoreMXBeanType(),
                self());
        listenerInfoMXBean.register();
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
    public void postStop() throws Exception {
        LOG.info("Stopping Shard {}", persistenceId());

        super.postStop();

        messageRetrySupport.close();

        if (txCommitTimeoutCheckSchedule != null) {
            txCommitTimeoutCheckSchedule.cancel();
        }

        commitCoordinator.abortPendingTransactions("Transaction aborted due to shutdown.", this);

        shardMBean.unregisterMBean();
        listenerInfoMXBean.unregister();
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
        try (MessageTracker.Context context = appendEntriesReplyTracker.received(message)) {
            final Optional<Error> maybeError = context.error();
            if (maybeError.isPresent()) {
                LOG.trace("{} : AppendEntriesReply failed to arrive at the expected interval {}", persistenceId(),
                    maybeError.get());
            }

            store.resetTransactionBatch();

            if (message instanceof RequestEnvelope) {
                handleRequestEnvelope((RequestEnvelope)message);
            } else if (MessageAssembler.isHandledMessage(message)) {
                handleRequestAssemblerMessage(message);
            } else if (message instanceof ConnectClientRequest) {
                handleConnectClient((ConnectClientRequest)message);
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
            } else if (message instanceof RegisterDataTreeChangeListener) {
                treeChangeSupport.onMessage((RegisterDataTreeChangeListener) message, isLeader(), hasLeader());
            } else if (message instanceof UpdateSchemaContext) {
                updateSchemaContext((UpdateSchemaContext) message);
            } else if (message instanceof PeerAddressResolved) {
                PeerAddressResolved resolved = (PeerAddressResolved) message;
                setPeerAddress(resolved.getPeerId(), resolved.getPeerAddress());
            } else if (TX_COMMIT_TIMEOUT_CHECK_MESSAGE.equals(message)) {
                commitTimeoutCheck();
            } else if (message instanceof DatastoreContext) {
                onDatastoreContext((DatastoreContext)message);
            } else if (message instanceof RegisterRoleChangeListener) {
                roleChangeNotifier.get().forward(message, context());
            } else if (message instanceof FollowerInitialSyncUpStatus) {
                shardMBean.setFollowerInitialSyncStatus(((FollowerInitialSyncUpStatus) message).isInitialSyncDone());
                tellShardManager(message);
            } else if (GET_SHARD_MBEAN_MESSAGE.equals(message)) {
                sender().tell(getShardMBean(), self());
            } else if (message instanceof GetShardDataTree) {
                sender().tell(store.getDataTree(), self());
            } else if (message instanceof ServerRemoved) {
                forwardToShardManager(message);
            } else if (ShardTransactionMessageRetrySupport.TIMER_MESSAGE_CLASS.isInstance(message)) {
                messageRetrySupport.onTimerMessage(message);
            } else if (message instanceof DataTreeCohortActorRegistry.CohortRegistryCommand) {
                store.processCohortRegistryCommand(getSender(),
                        (DataTreeCohortActorRegistry.CohortRegistryCommand) message);
            } else if (message instanceof MakeLeaderLocal) {
                onMakeLeaderLocal();
            } else if (RESUME_NEXT_PENDING_TRANSACTION.equals(message)) {
                store.resumeNextPendingTransaction();
            } else if (GetKnownClients.INSTANCE.equals(message)) {
                handleGetKnownClients();
            } else if (!responseMessageSlicer.handleMessage(message)) {
                super.handleNonRaftCommand(message);
            }
        }
    }

    private void tellShardManager(final Object message) {
        if (shardManagerRef != null) {
            if (shardManagerRef.equals(context().parent())) {
                shardManagerRef.tell(message, self());
            } else {
                shardManagerRef.tell(message, context().parent());
            }
        } else {
            Preconditions.checkState(!isBackoffSupervised());
            context().parent().tell(message, self());
        }
    }

    private void forwardToShardManager(final Object message) {
        if (shardManagerRef != null) {
            shardManagerRef.forward(message, context());
        } else {
            Preconditions.checkState(!isBackoffSupervised());
            context().parent().forward(message, context());
        }
    }

    private void handleRequestAssemblerMessage(final Object message) {
        dispatchers.getDispatcher(DispatcherType.Serialization).execute(() -> {
            JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) context().system());
            requestMessageAssembler.handleMessage(message, self());
        });
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleRequestEnvelope(final RequestEnvelope envelope) {
        final long now = ticker().read();
        try {
            final RequestSuccess<?, ?> success = handleRequest(envelope, now);
            if (success != null) {
                final long executionTimeNanos = ticker().read() - now;
                if (success instanceof SliceableMessage) {
                    dispatchers.getDispatcher(DispatcherType.Serialization).execute(() ->
                        responseMessageSlicer.slice(SliceOptions.builder().identifier(success.getTarget())
                            .message(envelope.newSuccessEnvelope(success, executionTimeNanos))
                            .sendTo(envelope.getMessage().getReplyTo()).replyTo(self())
                            .onFailureCallback(t -> LOG.warn("Error slicing response {}", success, t)).build()));
                } else {
                    envelope.sendSuccess(success, executionTimeNanos);
                }
            }
        } catch (RequestException e) {
            LOG.debug("{}: request {} failed", persistenceId(), envelope, e);
            envelope.sendFailure(e, ticker().read() - now);
        } catch (Exception e) {
            LOG.debug("{}: request {} caused failure", persistenceId(), envelope, e);
            envelope.sendFailure(new RuntimeRequestException("Request failed to process", e),
                ticker().read() - now);
        }
    }

    private void commitTimeoutCheck() {
        store.checkForExpiredTransactions(transactionCommitTimeout, this::updateAccess);
        commitCoordinator.checkForExpiredTransactions(transactionCommitTimeout, this);
        requestMessageAssembler.checkExpiredAssembledMessageState();
    }

    private OptionalLong updateAccess(final SimpleShardDataTreeCohort cohort) {
        final FrontendIdentifier frontend = cohort.getIdentifier().getHistoryId().getClientId().getFrontendId();
        final LeaderFrontendState state = knownFrontends.get(frontend);
        if (state == null) {
            // Not tell-based protocol, do nothing
            return OptionalLong.empty();
        }

        if (isIsolatedLeader()) {
            // We are isolated and no new request can come through until we emerge from it. We are still updating
            // liveness of frontend when we see it attempting to communicate. Use the last access timer.
            return OptionalLong.of(state.getLastSeenTicks());
        }

        // If this frontend has freshly connected, give it some time to catch up before killing its transactions.
        return OptionalLong.of(state.getLastConnectTicks());
    }

    private void disableTracking(final DisableTrackingPayload payload) {
        final ClientIdentifier clientId = payload.getIdentifier();
        LOG.debug("{}: disabling tracking of {}", persistenceId(), clientId);
        frontendMetadata.disableTracking(clientId);

        if (isLeader()) {
            final FrontendIdentifier frontendId = clientId.getFrontendId();
            final LeaderFrontendState frontend = knownFrontends.get(frontendId);
            if (frontend != null) {
                if (clientId.equals(frontend.getIdentifier())) {
                    if (!(frontend instanceof LeaderFrontendState.Disabled)) {
                        verify(knownFrontends.replace(frontendId, frontend,
                            new LeaderFrontendState.Disabled(persistenceId(), clientId, store)));
                        LOG.debug("{}: leader state for {} disabled", persistenceId(), clientId);
                    } else {
                        LOG.debug("{}: leader state {} is already disabled", persistenceId(), frontend);
                    }
                } else {
                    LOG.debug("{}: leader state {} does not match {}", persistenceId(), frontend, clientId);
                }
            } else {
                LOG.debug("{}: leader state for {} not found", persistenceId(), clientId);
                knownFrontends.put(frontendId, new LeaderFrontendState.Disabled(persistenceId(), clientId,
                    getDataStore()));
            }
        }
    }

    private void onMakeLeaderLocal() {
        LOG.debug("{}: onMakeLeaderLocal received", persistenceId());
        if (isLeader()) {
            getSender().tell(new Status.Success(null), getSelf());
            return;
        }

        final ActorSelection leader = getLeader();

        if (leader == null) {
            // Leader is not present. The cluster is most likely trying to
            // elect a leader and we should let that run its normal course

            // TODO we can wait for the election to complete and retry the
            // request. We can also let the caller retry by sending a flag
            // in the response indicating the request is "reTryable".
            getSender().tell(new Failure(
                    new LeadershipTransferFailedException("We cannot initiate leadership transfer to local node. "
                            + "Currently there is no leader for " + persistenceId())),
                    getSelf());
            return;
        }

        leader.tell(new RequestLeadership(getId(), getSender()), getSelf());
    }

    // Acquire our frontend tracking handle and verify generation matches
    private @Nullable LeaderFrontendState findFrontend(final ClientIdentifier clientId) throws RequestException {
        final LeaderFrontendState existing = knownFrontends.get(clientId.getFrontendId());
        if (existing != null) {
            final int cmp = Long.compareUnsigned(existing.getIdentifier().getGeneration(), clientId.getGeneration());
            if (cmp == 0) {
                existing.touch();
                return existing;
            }
            if (cmp > 0) {
                LOG.debug("{}: rejecting request from outdated client {}", persistenceId(), clientId);
                throw new RetiredGenerationException(clientId.getGeneration(),
                    existing.getIdentifier().getGeneration());
            }

            LOG.info("{}: retiring state {}, outdated by request from client {}", persistenceId(), existing, clientId);
            existing.retire();
            knownFrontends.remove(clientId.getFrontendId());
        } else {
            LOG.debug("{}: client {} is not yet known", persistenceId(), clientId);
        }

        return null;
    }

    private LeaderFrontendState getFrontend(final ClientIdentifier clientId) throws RequestException {
        final LeaderFrontendState ret = findFrontend(clientId);
        if (ret != null) {
            return ret;
        }

        // TODO: a dedicated exception would be better, but this is technically true, too
        throw new OutOfSequenceEnvelopeException(0);
    }

    private static @NonNull ABIVersion selectVersion(final ConnectClientRequest message) {
        final Range<ABIVersion> clientRange = Range.closed(message.getMinVersion(), message.getMaxVersion());
        for (ABIVersion v : SUPPORTED_ABIVERSIONS) {
            if (clientRange.contains(v)) {
                return v;
            }
        }

        throw new IllegalArgumentException(String.format(
            "No common version between backend versions %s and client versions %s", SUPPORTED_ABIVERSIONS,
            clientRange));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleConnectClient(final ConnectClientRequest message) {
        try {
            final ClientIdentifier clientId = message.getTarget();
            final LeaderFrontendState existing = findFrontend(clientId);
            if (existing != null) {
                existing.touch();
            }

            if (!isLeader() || !isLeaderActive()) {
                LOG.info("{}: not currently leader, rejecting request {}. isLeader: {}, isLeaderActive: {},"
                                + "isLeadershipTransferInProgress: {}.",
                        persistenceId(), message, isLeader(), isLeaderActive(), isLeadershipTransferInProgress());
                throw new NotLeaderException(getSelf());
            }

            final ABIVersion selectedVersion = selectVersion(message);
            final LeaderFrontendState frontend;
            if (existing == null) {
                frontend = new LeaderFrontendState.Enabled(persistenceId(), clientId, store);
                knownFrontends.put(clientId.getFrontendId(), frontend);
                LOG.debug("{}: created state {} for client {}", persistenceId(), frontend, clientId);
            } else {
                frontend = existing;
            }

            frontend.reconnect();
            message.getReplyTo().tell(new ConnectClientSuccess(message.getTarget(), message.getSequence(), getSelf(),
                ImmutableList.of(), store.getDataTree(), CLIENT_MAX_MESSAGES).toVersion(selectedVersion),
                ActorRef.noSender());
        } catch (RequestException | RuntimeException e) {
            message.getReplyTo().tell(new Failure(e), ActorRef.noSender());
        }
    }

    private @Nullable RequestSuccess<?, ?> handleRequest(final RequestEnvelope envelope, final long now)
            throws RequestException {
        // We are not the leader, hence we want to fail-fast.
        if (!isLeader() || paused || !isLeaderActive()) {
            LOG.debug("{}: not currently active leader, rejecting request {}. isLeader: {}, isLeaderActive: {},"
                            + "isLeadershipTransferInProgress: {}, paused: {}",
                    persistenceId(), envelope, isLeader(), isLeaderActive(), isLeadershipTransferInProgress(), paused);
            throw new NotLeaderException(getSelf());
        }

        final Request<?, ?> request = envelope.getMessage();
        if (request instanceof TransactionRequest) {
            final TransactionRequest<?> txReq = (TransactionRequest<?>)request;
            final ClientIdentifier clientId = txReq.getTarget().getHistoryId().getClientId();
            return getFrontend(clientId).handleTransactionRequest(txReq, envelope, now);
        } else if (request instanceof LocalHistoryRequest) {
            final LocalHistoryRequest<?> lhReq = (LocalHistoryRequest<?>)request;
            final ClientIdentifier clientId = lhReq.getTarget().getClientId();
            return getFrontend(clientId).handleLocalHistoryRequest(lhReq, envelope, now);
        } else {
            LOG.warn("{}: rejecting unsupported request {}", persistenceId(), request);
            throw new UnsupportedRequestException(request);
        }
    }

    private void handleGetKnownClients() {
        final ImmutableSet<ClientIdentifier> clients;
        if (isLeader()) {
            clients = knownFrontends.values().stream()
                    .map(LeaderFrontendState::getIdentifier)
                    .collect(ImmutableSet.toImmutableSet());
        } else {
            clients = frontendMetadata.getClients();
        }
        sender().tell(new GetKnownClientsReply(clients), self());
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

    String getShardName() {
        return shardName;
    }

    @Override
    protected LeaderStateChanged newLeaderStateChanged(final String memberId, final String leaderId,
            final short leaderPayloadVersion) {
        return isLeader() ? new ShardLeaderStateChanged(memberId, leaderId, store.getDataTree(), leaderPayloadVersion)
                : new ShardLeaderStateChanged(memberId, leaderId, leaderPayloadVersion);
    }

    protected void onDatastoreContext(final DatastoreContext context) {
        datastoreContext = context;

        setTransactionCommitTimeout();

        setPersistence(datastoreContext.isPersistent());

        updateConfigParams(datastoreContext.getShardRaftConfig());
    }

    // applyState() will be invoked once consensus is reached on the payload
    void persistPayload(final Identifier id, final Payload payload, final boolean batchHint) {
        boolean canSkipPayload = !hasFollowers() && !persistence().isRecoveryApplicable();
        if (canSkipPayload) {
            applyState(self(), id, payload);
        } else {
            // We are faking the sender
            persistData(self(), id, payload, batchHint);
        }
    }

    private void handleCommitTransaction(final CommitTransaction commit) {
        final TransactionIdentifier txId = commit.getTransactionId();
        if (isLeader()) {
            askProtocolEncountered(txId);
            commitCoordinator.handleCommit(txId, getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (leader == null) {
                messageRetrySupport.addMessageToRetry(commit, getSender(), "Could not commit transaction " + txId);
            } else {
                LOG.debug("{}: Forwarding CommitTransaction to leader {}", persistenceId(), leader);
                leader.forward(commit, getContext());
            }
        }
    }

    private void handleCanCommitTransaction(final CanCommitTransaction canCommit) {
        final TransactionIdentifier txId = canCommit.getTransactionId();
        LOG.debug("{}: Can committing transaction {}", persistenceId(), txId);

        if (isLeader()) {
            askProtocolEncountered(txId);
            commitCoordinator.handleCanCommit(txId, getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (leader == null) {
                messageRetrySupport.addMessageToRetry(canCommit, getSender(),
                        "Could not canCommit transaction " + txId);
            } else {
                LOG.debug("{}: Forwarding CanCommitTransaction to leader {}", persistenceId(), leader);
                leader.forward(canCommit, getContext());
            }
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    protected void handleBatchedModificationsLocal(final BatchedModifications batched, final ActorRef sender) {
        askProtocolEncountered(batched.getTransactionId());

        try {
            commitCoordinator.handleBatchedModifications(batched, sender, this);
        } catch (Exception e) {
            LOG.error("{}: Error handling BatchedModifications for Tx {}", persistenceId(),
                    batched.getTransactionId(), e);
            sender.tell(new Failure(e), getSelf());
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
                        "Could not process BatchedModifications " + batched.getTransactionId());
            } else {
                // If this is not the first batch and leadership changed in between batched messages,
                // we need to reconstruct previous BatchedModifications from the transaction
                // DataTreeModification, honoring the max batched modification count, and forward all the
                // previous BatchedModifications to the new leader.
                Collection<BatchedModifications> newModifications = commitCoordinator
                        .createForwardedBatchedModifications(batched,
                                datastoreContext.getShardBatchedModificationCount());

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
            sender.tell(new Failure(new NoShardLeaderException(String.format(
                    "Shard %s was the leader but has lost contact with all of its followers. Either all"
                    + " other follower nodes are down or this node is isolated by a network partition.",
                    persistenceId()))), getSelf());
            return true;
        }

        return false;
    }

    protected boolean isIsolatedLeader() {
        return getRaftState() == RaftState.IsolatedLeader;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void handleReadyLocalTransaction(final ReadyLocalTransaction message) {
        LOG.debug("{}: handleReadyLocalTransaction for {}", persistenceId(), message.getTransactionId());

        boolean isLeaderActive = isLeaderActive();
        if (isLeader() && isLeaderActive) {
            try {
                commitCoordinator.handleReadyLocalTransaction(message, getSender(), this);
            } catch (Exception e) {
                LOG.error("{}: Error handling ReadyLocalTransaction for Tx {}", persistenceId(),
                        message.getTransactionId(), e);
                getSender().tell(new Failure(e), getSelf());
            }
        } else {
            ActorSelection leader = getLeader();
            if (!isLeaderActive || leader == null) {
                messageRetrySupport.addMessageToRetry(message, getSender(),
                        "Could not process ready local transaction " + message.getTransactionId());
            } else {
                LOG.debug("{}: Forwarding ReadyLocalTransaction to leader {}", persistenceId(), leader);
                message.setRemoteVersion(getCurrentBehavior().getLeaderPayloadVersion());
                leader.forward(message, getContext());
            }
        }
    }

    private void handleForwardedReadyTransaction(final ForwardedReadyTransaction forwardedReady) {
        LOG.debug("{}: handleForwardedReadyTransaction for {}", persistenceId(), forwardedReady.getTransactionId());

        boolean isLeaderActive = isLeaderActive();
        if (isLeader() && isLeaderActive) {
            askProtocolEncountered(forwardedReady.getTransactionId());
            commitCoordinator.handleForwardedReadyTransaction(forwardedReady, getSender(), this);
        } else {
            ActorSelection leader = getLeader();
            if (!isLeaderActive || leader == null) {
                messageRetrySupport.addMessageToRetry(forwardedReady, getSender(),
                        "Could not process forwarded ready transaction " + forwardedReady.getTransactionId());
            } else {
                LOG.debug("{}: Forwarding ForwardedReadyTransaction to leader {}", persistenceId(), leader);

                ReadyLocalTransaction readyLocal = new ReadyLocalTransaction(forwardedReady.getTransactionId(),
                        forwardedReady.getTransaction().getSnapshot(), forwardedReady.isDoImmediateCommit(),
                        forwardedReady.getParticipatingShardNames());
                readyLocal.setRemoteVersion(getCurrentBehavior().getLeaderPayloadVersion());
                leader.forward(readyLocal, getContext());
            }
        }
    }

    private void handleAbortTransaction(final AbortTransaction abort) {
        final TransactionIdentifier transactionId = abort.getTransactionId();
        askProtocolEncountered(transactionId);
        doAbortTransaction(transactionId, getSender());
    }

    void doAbortTransaction(final Identifier transactionID, final ActorRef sender) {
        commitCoordinator.handleAbort(transactionID, sender, this);
    }

    private void handleCreateTransaction(final Object message) {
        if (isLeader()) {
            createTransaction(CreateTransaction.fromSerializable(message));
        } else if (getLeader() != null) {
            getLeader().forward(message, getContext());
        } else {
            getSender().tell(new Failure(new NoShardLeaderException(
                    "Could not create a shard transaction", persistenceId())), getSelf());
        }
    }

    private void closeTransactionChain(final CloseTransactionChain closeTransactionChain) {
        if (isLeader()) {
            final LocalHistoryIdentifier id = closeTransactionChain.getIdentifier();
            askProtocolEncountered(id.getClientId());
            store.closeTransactionChain(id);
        } else if (getLeader() != null) {
            getLeader().forward(closeTransactionChain, getContext());
        } else {
            LOG.warn("{}: Could not close transaction {}", persistenceId(), closeTransactionChain.getIdentifier());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void createTransaction(final CreateTransaction createTransaction) {
        askProtocolEncountered(createTransaction.getTransactionId());

        try {
            if (TransactionType.fromInt(createTransaction.getTransactionType()) != TransactionType.READ_ONLY
                    && failIfIsolatedLeader(getSender())) {
                return;
            }

            ActorRef transactionActor = createTransaction(createTransaction.getTransactionType(),
                createTransaction.getTransactionId());

            getSender().tell(new CreateTransactionReply(Serialization.serializedActorPath(transactionActor),
                    createTransaction.getTransactionId(), createTransaction.getVersion()).toSerializable(), getSelf());
        } catch (Exception e) {
            getSender().tell(new Failure(e), getSelf());
        }
    }

    private ActorRef createTransaction(final int transactionType, final TransactionIdentifier transactionId) {
        LOG.debug("{}: Creating transaction : {} ", persistenceId(), transactionId);
        return transactionActorFactory.newShardTransaction(TransactionType.fromInt(transactionType),
            transactionId);
    }

    // Called on leader only
    private void askProtocolEncountered(final TransactionIdentifier transactionId) {
        askProtocolEncountered(transactionId.getHistoryId().getClientId());
    }

    // Called on leader only
    private void askProtocolEncountered(final ClientIdentifier clientId) {
        final FrontendIdentifier frontend = clientId.getFrontendId();
        final LeaderFrontendState state = knownFrontends.get(frontend);
        if (!(state instanceof LeaderFrontendState.Disabled)) {
            LOG.debug("{}: encountered ask-based client {}, disabling transaction tracking", persistenceId(), clientId);
            if (knownFrontends.isEmpty()) {
                knownFrontends = new HashMap<>();
            }
            knownFrontends.put(frontend, new LeaderFrontendState.Disabled(persistenceId(), clientId, getDataStore()));

            persistPayload(clientId, DisableTrackingPayload.create(clientId,
                datastoreContext.getInitialPayloadSerializedBufferCapacity()), false);
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
    protected RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        if (restoreFromSnapshot == null) {
            return ShardRecoveryCoordinator.create(store, persistenceId(), LOG);
        }

        return ShardRecoveryCoordinator.forSnapshot(store, persistenceId(), LOG, restoreFromSnapshot.getSnapshot());
    }

    @Override
    protected void onRecoveryComplete() {
        restoreFromSnapshot = null;

        //notify shard manager
        tellShardManager(new ActorInitialized());

        // Being paranoid here - this method should only be called once but just in case...
        if (txCommitTimeoutCheckSchedule == null) {
            // Schedule a message to be periodically sent to check if the current in-progress
            // transaction should be expired and aborted.
            FiniteDuration period = FiniteDuration.create(transactionCommitTimeout / 3, TimeUnit.MILLISECONDS);
            txCommitTimeoutCheckSchedule = getContext().system().scheduler().schedule(
                    period, period, getSelf(),
                    TX_COMMIT_TIMEOUT_CHECK_MESSAGE, getContext().dispatcher(), ActorRef.noSender());
        }
    }

    @Override
    protected void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        if (data instanceof Payload) {
            if (data instanceof DisableTrackingPayload) {
                disableTracking((DisableTrackingPayload) data);
                return;
            }

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
        treeChangeSupport.onLeadershipChange(isLeader, hasLeader);

        // If this actor is no longer the leader close all the transaction chains
        if (!isLeader) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "{}: onStateChanged: Closing all transaction chains because shard {} is no longer the leader",
                    persistenceId(), getId());
            }

            paused = false;
            store.purgeLeaderState();
        }

        if (hasLeader && !isIsolatedLeader()) {
            messageRetrySupport.retryMessages();
        }
    }

    @Override
    protected void onLeaderChanged(final String oldLeader, final String newLeader) {
        shardMBean.incrementLeadershipChangeCount();
        paused = false;

        if (!isLeader()) {
            if (!knownFrontends.isEmpty()) {
                LOG.debug("{}: removing frontend state for {}", persistenceId(), knownFrontends.keySet());
                knownFrontends = ImmutableMap.of();
            }

            requestMessageAssembler.close();

            if (!hasLeader()) {
                // No leader anywhere, nothing else to do
                return;
            }

            // Another leader was elected. If we were the previous leader and had pending transactions, convert
            // them to transaction messages and send to the new leader.
            ActorSelection leader = getLeader();
            if (leader != null) {
                Collection<?> messagesToForward = convertPendingTransactionsToMessages();

                if (!messagesToForward.isEmpty()) {
                    LOG.debug("{}: Forwarding {} pending transaction messages to leader {}", persistenceId(),
                            messagesToForward.size(), leader);

                    for (Object message : messagesToForward) {
                        LOG.debug("{}: Forwarding pending transaction message {}", persistenceId(), message);

                        leader.tell(message, self());
                    }
                }
            } else {
                commitCoordinator.abortPendingTransactions("The transacton was aborted due to inflight leadership "
                        + "change and the leader address isn't available.", this);
            }
        } else {
            // We have become the leader, we need to reconstruct frontend state
            knownFrontends = Verify.verifyNotNull(frontendMetadata.toLeaderState(this));
            LOG.debug("{}: became leader with frontend state for {}", persistenceId(), knownFrontends.keySet());
        }

        if (!isIsolatedLeader()) {
            messageRetrySupport.retryMessages();
        }
    }

    /**
     * Clears all pending transactions and converts them to messages to be forwarded to a new leader.
     *
     * @return the converted messages
     */
    public Collection<?> convertPendingTransactionsToMessages() {
        return commitCoordinator.convertPendingTransactionsToMessages(
                datastoreContext.getShardBatchedModificationCount());
    }

    @Override
    protected void pauseLeader(final Runnable operation) {
        LOG.debug("{}: In pauseLeader, operation: {}", persistenceId(), operation);
        paused = true;

        // Tell-based protocol can replay transaction state, so it is safe to blow it up when we are paused.
        if (datastoreContext.isUseTellBasedProtocol()) {
            knownFrontends.values().forEach(LeaderFrontendState::retire);
            knownFrontends = ImmutableMap.of();
        }

        store.setRunOnPendingTransactionsComplete(operation);
    }

    @Override
    protected void unpauseLeader() {
        LOG.debug("{}: In unpauseLeader", persistenceId());
        paused = false;

        store.setRunOnPendingTransactionsComplete(null);

        // Restore tell-based protocol state as if we were becoming the leader
        knownFrontends = Verify.verifyNotNull(frontendMetadata.toLeaderState(this));
    }

    @Override
    protected OnDemandRaftState.AbstractBuilder<?, ?> newOnDemandRaftStateBuilder() {
        return OnDemandShardState.newBuilder().treeChangeListenerActors(treeChangeSupport.getListenerActors())
                .commitCohortActors(store.getCohortActors());
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

    public abstract static class AbstractBuilder<T extends AbstractBuilder<T, S>, S extends Shard> {
        private final Class<? extends S> shardClass;
        private ShardIdentifier id;
        private Map<String, String> peerAddresses = Collections.emptyMap();
        private DatastoreContext datastoreContext;
        private SchemaContextProvider schemaContextProvider;
        private DatastoreSnapshot.ShardSnapshot restoreFromSnapshot;
        private DataTree dataTree;

        private ActorRef shardManagerRef;
        private boolean backoffSupervised;
        private volatile boolean sealed;

        protected AbstractBuilder(final Class<? extends S> shardClass) {
            this.shardClass = shardClass;
        }

        protected void checkSealed() {
            checkState(!sealed, "Builder isalready sealed - further modifications are not allowed");
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public T id(final ShardIdentifier newId) {
            checkSealed();
            this.id = newId;
            return self();
        }

        public T peerAddresses(final Map<String, String> newPeerAddresses) {
            checkSealed();
            this.peerAddresses = newPeerAddresses;
            return self();
        }

        public T datastoreContext(final DatastoreContext newDatastoreContext) {
            checkSealed();
            this.datastoreContext = newDatastoreContext;
            return self();
        }

        public T schemaContextProvider(final SchemaContextProvider newSchemaContextProvider) {
            checkSealed();
            this.schemaContextProvider = requireNonNull(newSchemaContextProvider);
            return self();
        }

        public T restoreFromSnapshot(final DatastoreSnapshot.ShardSnapshot newRestoreFromSnapshot) {
            checkSealed();
            this.restoreFromSnapshot = newRestoreFromSnapshot;
            return self();
        }

        public T dataTree(final DataTree newDataTree) {
            checkSealed();
            this.dataTree = newDataTree;
            return self();
        }

        public T shardManagerRef(final ActorRef newShardManagerRef) {
            checkSealed();
            shardManagerRef = newShardManagerRef;
            return self();
        }

        public T backoffSupervised(final boolean newBackoffSupervised) {
            checkSealed();
            backoffSupervised = newBackoffSupervised;
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
            return Verify.verifyNotNull(schemaContextProvider.getSchemaContext());
        }

        public DatastoreSnapshot.ShardSnapshot getRestoreFromSnapshot() {
            return restoreFromSnapshot;
        }

        public DataTree getDataTree() {
            return dataTree;
        }

        public TreeType getTreeType() {
            switch (datastoreContext.getLogicalStoreType()) {
                case CONFIGURATION:
                    return TreeType.CONFIGURATION;
                case OPERATIONAL:
                    return TreeType.OPERATIONAL;
                default:
                    throw new IllegalStateException("Unhandled logical store type "
                            + datastoreContext.getLogicalStoreType());
            }
        }

        public ActorRef getShardManagerRef() {
            return shardManagerRef;
        }

        public boolean isBackoffSupervised() {
            return backoffSupervised;
        }

        protected void verify() {
            requireNonNull(id, "id should not be null");
            requireNonNull(peerAddresses, "peerAddresses should not be null");
            requireNonNull(datastoreContext, "dataStoreContext should not be null");
            requireNonNull(schemaContextProvider, "schemaContextProvider should not be null");
        }

        public Props props() {
            sealed = true;
            verify();
            return Props.create(shardClass, this);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, Shard> {
        Builder() {
            this(Shard.class);
        }

        Builder(final Class<? extends Shard> shardClass) {
            super(shardClass);
        }
    }

    Ticker ticker() {
        return Ticker.systemTicker();
    }

    void scheduleNextPendingTransaction() {
        self().tell(RESUME_NEXT_PENDING_TRANSACTION, ActorRef.noSender());
    }
}
