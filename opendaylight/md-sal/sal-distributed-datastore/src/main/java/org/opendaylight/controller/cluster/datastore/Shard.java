/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Status;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotOffer;
import org.apache.pekko.serialization.JavaSerializer;
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
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.SliceableMessage;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.controller.cluster.common.actor.CommonConfig;
import org.opendaylight.controller.cluster.common.actor.Dispatchers;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.common.actor.MessageTracker;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.datastore.actors.JsonExportActor;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClients;
import org.opendaylight.controller.cluster.datastore.messages.GetKnownClientsReply;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.messages.MakeLeaderLocal;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
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
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadership;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties.ExportOnRecovery;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.TreeType;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Shard represents a portion of the logical data tree.
 *
 * <p>Our Shard uses InMemoryDataTree as its internal representation and delegates all requests it
 */
// FIXME: non-final for testing?
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

    private static final Logger LOG = LoggerFactory.getLogger(Shard.class);

    // FIXME: shard names should be encapsulated in their own class and this should be exposed as a constant.
    public static final String DEFAULT_NAME = "default";

    private static final List<ABIVersion> SUPPORTED_ABIVERSIONS;

    // Make sure to keep this in sync with the journal configuration in factory-pekko.conf
    public static final String NON_PERSISTENT_JOURNAL_ID = "pekko.persistence.non-persistent.journal";

    static {
        final ABIVersion[] values = ABIVersion.values();
        final ABIVersion[] real = Arrays.copyOfRange(values, 1, values.length - 1);
        SUPPORTED_ABIVERSIONS = ImmutableList.copyOf(real).reverse();
    }

    // FIXME: make this a dynamic property based on mailbox size and maximum number of clients
    private static final int CLIENT_MAX_MESSAGES = 1000;

    // The state of this Shard
    private final ShardDataTree store;

    private final String shardName;

    private final DefaultShardStatsMXBean shardMBean;

    private final ShardDataTreeListenerInfoMXBeanImpl listenerInfoMXBean;

    private DatastoreContext datastoreContext;

    private long transactionCommitTimeout;

    private Cancellable txCommitTimeoutCheckSchedule;

    private final ActorRef roleChangeNotifier;

    private final MessageTracker appendEntriesReplyTracker;

    private final ShardSnapshotCohort snapshotCohort;

    private final DataTreeChangeListenerSupport treeChangeSupport = new DataTreeChangeListenerSupport(this);

    private ShardSnapshot restoreFromSnapshot;

    @VisibleForTesting
    final FrontendMetadata frontendMetadata;

    private Map<FrontendIdentifier, LeaderFrontendState> knownFrontends = ImmutableMap.of();
    private boolean paused;

    private final MessageSlicer responseMessageSlicer;
    private final Dispatchers dispatchers;

    private final MessageAssembler requestMessageAssembler;

    private final ExportOnRecovery exportOnRecovery;

    private final ActorRef exportActor;

    Shard(final AbstractBuilder<?, ?> builder) {
        super(builder.getBaseDir(), builder.getId().toString(), builder.getPeerAddresses(),
                Optional.of(builder.getDatastoreContext().getShardRaftConfig()), DataStoreVersions.CURRENT_VERSION);

        shardName = builder.getId().getShardName();
        datastoreContext = builder.getDatastoreContext();
        restoreFromSnapshot = builder.getRestoreFromSnapshot();

        final var name = memberId();
        frontendMetadata = new FrontendMetadata(name);
        exportOnRecovery = datastoreContext.getExportOnRecovery();

        exportActor = switch (exportOnRecovery) {
            case Json -> getContext().actorOf(JsonExportActor.props(builder.getSchemaContext(),
                datastoreContext.getRecoveryExportBaseDir()));
            case Off -> null;
        };

        setPersistence(datastoreContext.isPersistent());

        LOG.info("{}: Shard created, persistent : {}", memberId(), datastoreContext.isPersistent());

        ShardDataTreeChangeListenerPublisherActorProxy treeChangeListenerPublisher =
                new ShardDataTreeChangeListenerPublisherActorProxy(getContext(), name + "-DTCL-publisher", name);
        if (builder.getDataTree() != null) {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getDataTree(),
                    treeChangeListenerPublisher, name,
                    frontendMetadata);
        } else {
            store = new ShardDataTree(this, builder.getSchemaContext(), builder.getTreeType(),
                    builder.getDatastoreContext().getStoreRoot(), treeChangeListenerPublisher, name,
                    frontendMetadata);
        }

        shardMBean = DefaultShardStatsMXBean.create(name, datastoreContext.getDataStoreMXBeanType(), this);

        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }

        setTransactionCommitTimeout();

        // create a notifier actor for each cluster member
        roleChangeNotifier = getContext().actorOf(RoleChangeNotifier.getProps(name), name + "-notifier");

        appendEntriesReplyTracker = new MessageTracker(AppendEntriesReply.class,
                getRaftActorContext().getConfigParams().getIsolatedCheckIntervalInMillis());

        dispatchers = new Dispatchers(getContext().system().dispatchers());

        snapshotCohort = ShardSnapshotCohort.create(getContext(), builder.getId().getMemberName(), store, name,
            datastoreContext);

        responseMessageSlicer = MessageSlicer.builder().logContext(name)
                .messageSliceSize(datastoreContext.getMaximumMessageSliceSize())
                .fileBackedStreamFactory(getRaftActorContext().getFileBackedOutputStreamFactory())
                .expireStateAfterInactivity(2, TimeUnit.MINUTES).build();

        requestMessageAssembler = MessageAssembler.builder().logContext(name)
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

    @Override
    public final void postStop() throws Exception {
        LOG.info("{}: Stopping Shard", memberId());

        super.postStop();

        if (txCommitTimeoutCheckSchedule != null) {
            txCommitTimeoutCheckSchedule.cancel();
        }

        shardMBean.unregisterMBean();
        listenerInfoMXBean.unregister();
    }

    @Override
    protected final void handleRecover(final Object message) {
        LOG.debug("{}: onReceiveRecover: Received message {} from {}", memberId(), message.getClass(), getSender());

        super.handleRecover(message);

        switch (exportOnRecovery) {
            case Json:
                if (message instanceof SnapshotOffer) {
                    exportActor.tell(
                        new JsonExportActor.ExportSnapshot(store.readCurrentData().orElseThrow(), memberId()),
                        ActorRef.noSender());
                } else if (message instanceof ReplicatedLogEntry replicatedLogEntry) {
                    exportActor.tell(new JsonExportActor.ExportJournal(replicatedLogEntry), ActorRef.noSender());
                } else if (message instanceof RecoveryCompleted) {
                    exportActor.tell(new JsonExportActor.FinishExport(memberId()), ActorRef.noSender());
                    exportActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
                }
                break;
            case Off:
            default:
                break;
        }

        if (LOG.isTraceEnabled()) {
            appendEntriesReplyTracker.begin();
        }
    }

    @Override
    // non-final for TestShard
    protected void handleNonRaftCommand(final Object message) {
        try (var context = appendEntriesReplyTracker.received(message)) {
            final var maybeError = context.error();
            if (maybeError.isPresent()) {
                LOG.trace("{} : AppendEntriesReply failed to arrive at the expected interval {}", memberId(),
                    maybeError.orElseThrow());
            }

            store.resetTransactionBatch();

            if (message instanceof RequestEnvelope request) {
                handleRequestEnvelope(request);
            } else if (MessageAssembler.isHandledMessage(message)) {
                handleRequestAssemblerMessage(message);
            } else if (message instanceof ConnectClientRequest request) {
                handleConnectClient(request);
            } else if (message instanceof DataTreeChangedReply) {
                // Ignore reply
            } else if (message instanceof RegisterDataTreeChangeListener request) {
                treeChangeSupport.onMessage(request, isLeader(), hasLeader());
            } else if (message instanceof UpdateSchemaContext request) {
                updateSchemaContext(request);
            } else if (message instanceof PeerAddressResolved resolved) {
                setPeerAddress(resolved.getPeerId(), resolved.getPeerAddress());
            } else if (TX_COMMIT_TIMEOUT_CHECK_MESSAGE.equals(message)) {
                commitTimeoutCheck();
            } else if (message instanceof DatastoreContext request) {
                onDatastoreContext(request);
            } else if (message instanceof RegisterRoleChangeListener) {
                roleChangeNotifier.forward(message, context());
            } else if (message instanceof FollowerInitialSyncUpStatus request) {
                shardMBean.setFollowerInitialSyncStatus(request.isInitialSyncDone());
                context().parent().tell(message, self());
            } else if (GET_SHARD_MBEAN_MESSAGE.equals(message)) {
                getSender().tell(getShardMBean(), self());
            } else if (message instanceof GetShardDataTree) {
                getSender().tell(store.getDataTree(), self());
            } else if (message instanceof ServerRemoved) {
                context().parent().forward(message, context());
            } else if (message instanceof DataTreeCohortActorRegistry.CohortRegistryCommand request) {
                store.processCohortRegistryCommand(getSender(), request);
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
                        responseMessageSlicer.slice(SliceOptions.builder()
                            .identifier(success.getTarget())
                            .message(envelope.newSuccessEnvelope(success, executionTimeNanos))
                            .sendTo(envelope.getMessage().getReplyTo()).replyTo(self())
                            .onFailureCallback(
                                t -> LOG.warn("{}: Error slicing response {}", memberId(), success, t))
                            .build()));
                } else {
                    envelope.sendSuccess(success, executionTimeNanos);
                }
            }
        } catch (RequestException e) {
            LOG.debug("{}: request {} failed", memberId(), envelope, e);
            envelope.sendFailure(e, ticker().read() - now);
        } catch (Exception e) {
            LOG.debug("{}: request {} caused failure", memberId(), envelope, e);
            envelope.sendFailure(new RuntimeRequestException("Request failed to process", e), ticker().read() - now);
        }
    }

    private void commitTimeoutCheck() {
        store.checkForExpiredTransactions(transactionCommitTimeout, this::updateAccess);
        requestMessageAssembler.checkExpiredAssembledMessageState();
    }

    private OptionalLong updateAccess(final CommitCohort cohort) {
        final var frontend = cohort.transactionId().getHistoryId().getClientId().getFrontendId();
        final var state = knownFrontends.get(frontend);
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

    private void onMakeLeaderLocal() {
        LOG.debug("{}: onMakeLeaderLocal received", memberId());
        if (isLeader()) {
            getSender().tell(new Status.Success(null), self());
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
                new LeadershipTransferFailedException(
                    "We cannot initiate leadership transfer to local node. "
                        + "Currently there is no leader for " + memberId())),
                self());
            return;
        }

        leader.tell(new RequestLeadership(memberId(), getSender()), self());
    }

    // Acquire our frontend tracking handle and verify generation matches
    private @Nullable LeaderFrontendState findFrontend(final ClientIdentifier clientId) throws RequestException {
        final var existing = knownFrontends.get(clientId.getFrontendId());
        if (existing != null) {
            final int cmp = Long.compareUnsigned(existing.getIdentifier().getGeneration(), clientId.getGeneration());
            if (cmp == 0) {
                existing.touch();
                return existing;
            }
            if (cmp > 0) {
                LOG.debug("{}: rejecting request from outdated client {}", memberId(), clientId);
                throw new RetiredGenerationException(clientId.getGeneration(),
                    existing.getIdentifier().getGeneration());
            }

            LOG.info("{}: retiring state {}, outdated by request from client {}", memberId(), existing, clientId);
            existing.retire();
            knownFrontends.remove(clientId.getFrontendId());
        } else {
            LOG.debug("{}: client {} is not yet known", memberId(), clientId);
        }

        return null;
    }

    private LeaderFrontendState getFrontend(final ClientIdentifier clientId) throws RequestException {
        final var ret = findFrontend(clientId);
        if (ret != null) {
            return ret;
        }

        // TODO: a dedicated exception would be better, but this is technically true, too
        throw new OutOfSequenceEnvelopeException(0);
    }

    private static @NonNull ABIVersion selectVersion(final ConnectClientRequest message) {
        final var clientRange = Range.closed(message.getMinVersion(), message.getMaxVersion());
        for (var v : SUPPORTED_ABIVERSIONS) {
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
        final var clientId = message.getTarget();
        try {
            final var existing = findFrontend(clientId);
            if (existing != null) {
                existing.touch();
            }

            if (!isLeader() || !isLeaderActive()) {
                LOG.info("{}: not currently leader, rejecting request {}. isLeader: {}, isLeaderActive: {},"
                    + "isLeadershipTransferInProgress: {}.",
                    memberId(), message, isLeader(), isLeaderActive(), isLeadershipTransferInProgress());
                throw new NotLeaderException(self());
            }

            final ABIVersion selectedVersion = selectVersion(message);
            final LeaderFrontendState frontend;
            if (existing == null) {
                frontend = new LeaderFrontendState.Enabled(memberId(), clientId, store);
                knownFrontends.put(clientId.getFrontendId(), frontend);
                LOG.debug("{}: created state {} for client {}", memberId(), frontend, clientId);
            } else {
                frontend = existing;
            }

            frontend.reconnect();
            message.getReplyTo().tell(new ConnectClientSuccess(message.getTarget(), message.getSequence(), self(),
                List.of(), store.getDataTree(), CLIENT_MAX_MESSAGES).toVersion(selectedVersion), ActorRef.noSender());
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
                memberId(), envelope, isLeader(), isLeaderActive(), isLeadershipTransferInProgress(), paused);
            throw new NotLeaderException(self());
        }

        final var request = envelope.getMessage();
        return switch (request) {
            case TransactionRequest<?> req -> getFrontend(req.getTarget().getHistoryId().getClientId())
                .handleTransactionRequest(req, envelope, now);
            case LocalHistoryRequest<?> req -> getFrontend(req.getTarget().getClientId())
                .handleLocalHistoryRequest(req, envelope, now);
            default -> {
                LOG.warn("{}: rejecting unsupported request {}", memberId(), request);
                throw new UnsupportedRequestException(request);
            }
        };
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
        getSender().tell(new GetKnownClientsReply(clients), self());
    }

    private boolean hasLeader() {
        return getLeaderId() != null;
    }

    final int getPendingTxCommitQueueSize() {
        return store.getQueueSize();
    }

    @Override
    protected final ActorRef roleChangeNotifier() {
        return roleChangeNotifier;
    }

    final String getShardName() {
        return shardName;
    }

    @Override
    protected final LeaderStateChanged newLeaderStateChanged(final String memberId, final String leaderId,
            final short leaderPayloadVersion) {
        return isLeader() ? new ShardLeaderStateChanged(memberId, leaderId, store.getDataTree(), leaderPayloadVersion)
                : new ShardLeaderStateChanged(memberId, leaderId, leaderPayloadVersion);
    }

    private void onDatastoreContext(final DatastoreContext context) {
        datastoreContext = verifyNotNull(context);

        setTransactionCommitTimeout();

        setPersistence(datastoreContext.isPersistent());

        updateConfigParams(datastoreContext.getShardRaftConfig());
    }

    // applyState() will be invoked once consensus is reached on the payload
    // non-final for mocking
    void persistPayload(final Identifier id, final Payload payload, final boolean batchHint) {
        final boolean canSkipPayload = !hasFollowers() && !persistence().isRecoveryApplicable();
        if (canSkipPayload) {
            applyState(self(), id, payload);
        } else {
            // We are faking the sender
            persistData(self(), id, payload, batchHint);
        }
    }

    protected boolean isIsolatedLeader() {
        return getRaftState() == RaftState.IsolatedLeader;
    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
        updateSchemaContext(message.modelContext());
    }

    @VisibleForTesting
    void updateSchemaContext(final @NonNull EffectiveModelContext schemaContext) {
        store.updateSchemaContext(schemaContext);
    }

    private boolean isMetricsCaptureEnabled() {
        CommonConfig config = new CommonConfig(getContext().system().settings().config());
        return config.isMetricCaptureEnabled();
    }

    @Override
    protected final RaftActorSnapshotCohort getRaftActorSnapshotCohort() {
        return snapshotCohort;
    }

    @Override
    protected final RaftActorRecoveryCohort getRaftActorRecoveryCohort() {
        return restoreFromSnapshot == null ? ShardRecoveryCoordinator.create(store, memberId())
            : ShardRecoveryCoordinator.forSnapshot(store, memberId(), restoreFromSnapshot.getSnapshot());
    }

    @Override
    // non-final for testing
    protected void onRecoveryComplete() {
        restoreFromSnapshot = null;

        //notify shard manager
        getContext().parent().tell(new ActorInitialized(self()), ActorRef.noSender());

        // Being paranoid here - this method should only be called once but just in case...
        if (txCommitTimeoutCheckSchedule == null) {
            // Schedule a message to be periodically sent to check if the current in-progress transaction should be
            // expired and aborted.
            // Note:
            final var period = Duration.ofMillis(transactionCommitTimeout / 3);
            txCommitTimeoutCheckSchedule = getContext().system().scheduler()
                // withFixedDelay to avoid bursts
                .scheduleWithFixedDelay(period, period, self(), TX_COMMIT_TIMEOUT_CHECK_MESSAGE,
                    getContext().dispatcher(), ActorRef.noSender());
        }
    }

    @Override
    protected final void applyState(final ActorRef clientActor, final Identifier identifier, final Object data) {
        if (data instanceof Payload payload) {
            if (payload instanceof DisableTrackingPayload disableTracking) {
                LOG.debug("{}: ignoring legacy {}", memberId(), disableTracking);
                return;
            }

            try {
                store.applyReplicatedPayload(identifier, payload);
            } catch (DataValidationFailedException | IOException e) {
                LOG.error("{}: Error applying replica {}", memberId(), identifier, e);
            }
        } else {
            LOG.error("{}: Unknown state for {} received {}", memberId(), identifier, data);
        }
    }

    @Override
    protected final void onStateChanged() {
        boolean isLeader = isLeader();
        boolean hasLeader = hasLeader();
        treeChangeSupport.onLeadershipChange(isLeader, hasLeader);

        // If this actor is no longer the leader close all the transaction chains
        if (!isLeader) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "{}: onStateChanged: Closing all transaction chains because shard is no longer the leader",
                    memberId());
            }

            paused = false;
            store.purgeLeaderState();
        }
    }

    @Override
    protected final void onLeaderChanged(final String oldLeader, final String newLeader) {
        shardMBean.incrementLeadershipChangeCount();
        paused = false;

        if (!isLeader()) {
            if (!knownFrontends.isEmpty()) {
                LOG.debug("{}: removing frontend state for {}", memberId(), knownFrontends.keySet());
                knownFrontends = ImmutableMap.of();
            }

            requestMessageAssembler.close();
        } else {
            // We have become the leader, we need to reconstruct frontend state
            knownFrontends = verifyNotNull(frontendMetadata.toLeaderState(this));
            LOG.debug("{}: became leader with frontend state for {}", memberId(), knownFrontends.keySet());
        }
    }

    @Override
    protected final void pauseLeader(final Runnable operation) {
        LOG.debug("{}: In pauseLeader, operation: {}", memberId(), operation);
        paused = true;

        // Tell-based protocol can replay transaction state, so it is safe to blow it up when we are paused.
        knownFrontends.values().forEach(LeaderFrontendState::retire);
        knownFrontends = ImmutableMap.of();

        store.setRunOnPendingTransactionsComplete(operation);
    }

    @Override
    protected final void unpauseLeader() {
        LOG.debug("{}: In unpauseLeader", memberId());
        paused = false;

        store.setRunOnPendingTransactionsComplete(null);

        // Restore tell-based protocol state as if we were becoming the leader
        knownFrontends = verifyNotNull(frontendMetadata.toLeaderState(this));
    }

    @Override
    protected final OnDemandRaftState.AbstractBuilder<?, ?> newOnDemandRaftStateBuilder() {
        return OnDemandShardState.newBuilder()
            .treeChangeListenerActors(treeChangeSupport.getListenerActors())
            .commitCohortActors(store.getCohortActors());
    }

    @Override
    public final String journalPluginId() {
        // This method may be invoked from super constructor (wonderful), hence we also need to handle the case of
        // the field being uninitialized because our constructor is not finished.
        if (datastoreContext != null && !datastoreContext.isPersistent()) {
            return NON_PERSISTENT_JOURNAL_ID;
        }
        return super.journalPluginId();
    }

    // non-final for mocking
    DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    @VisibleForTesting
    final ShardDataTree getDataStore() {
        return store;
    }

    @VisibleForTesting
    // non-final for mocking
    ShardStatsMXBean getShardMBean() {
        return shardMBean;
    }

    @VisibleForTesting
    // non-final for mocking
    ShardStats shardStats() {
        return shardMBean.shardStats();
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract static class AbstractBuilder<T extends AbstractBuilder<T, S>, S extends Shard> {
        private final Class<? extends S> shardClass;
        private ShardIdentifier id;
        private Path baseDir;
        private Map<String, String> peerAddresses = Collections.emptyMap();
        private DatastoreContext datastoreContext;
        private Supplier<@NonNull EffectiveModelContext> schemaContextProvider;
        private DatastoreSnapshot.ShardSnapshot restoreFromSnapshot;
        private DataTree dataTree;

        private volatile boolean sealed;

        AbstractBuilder(final Class<? extends S> shardClass) {
            this.shardClass = shardClass;
        }

        final void checkSealed() {
            checkState(!sealed, "Builder is already sealed - further modifications are not allowed");
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public T baseDir(final Path newBaseDir) {
            checkSealed();
            baseDir = requireNonNull(newBaseDir);
            return self();
        }

        public T id(final ShardIdentifier newId) {
            checkSealed();
            id = newId;
            return self();
        }

        public T peerAddresses(final Map<String, String> newPeerAddresses) {
            checkSealed();
            peerAddresses = newPeerAddresses;
            return self();
        }

        public T datastoreContext(final DatastoreContext newDatastoreContext) {
            checkSealed();
            datastoreContext = newDatastoreContext;
            return self();
        }

        public T schemaContextProvider(final Supplier<@NonNull EffectiveModelContext> newSchemaContextProvider) {
            checkSealed();
            schemaContextProvider = requireNonNull(newSchemaContextProvider);
            return self();
        }

        public T restoreFromSnapshot(final DatastoreSnapshot.ShardSnapshot newRestoreFromSnapshot) {
            checkSealed();
            restoreFromSnapshot = newRestoreFromSnapshot;
            return self();
        }

        public T dataTree(final DataTree newDataTree) {
            checkSealed();
            dataTree = newDataTree;
            return self();
        }

        public ShardIdentifier getId() {
            return id;
        }

        public Path getBaseDir() {
            return baseDir;
        }

        public Map<String, String> getPeerAddresses() {
            return peerAddresses;
        }

        public DatastoreContext getDatastoreContext() {
            return datastoreContext;
        }

        public EffectiveModelContext getSchemaContext() {
            return verifyNotNull(schemaContextProvider.get());
        }

        public DatastoreSnapshot.ShardSnapshot getRestoreFromSnapshot() {
            return restoreFromSnapshot;
        }

        public DataTree getDataTree() {
            return dataTree;
        }

        public TreeType getTreeType() {
            return switch (datastoreContext.getLogicalStoreType()) {
                case CONFIGURATION -> TreeType.CONFIGURATION;
                case OPERATIONAL -> TreeType.OPERATIONAL;
            };
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
