/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfSequenceEnvelopeException;
import org.opendaylight.controller.cluster.access.commands.PurgeLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.UnknownHistoryException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.controller.cluster.datastore.ShardDataTreeCohort.State;
import org.opendaylight.controller.cluster.datastore.utils.MutableUnsignedLongSet;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by the shard leader. This class is responsible for tracking generations and sequencing
 * in the frontend/backend conversation. This class is NOT thread-safe.
 *
 * @author Robert Varga
 */
abstract class LeaderFrontendState implements Identifiable<ClientIdentifier> {
    static final class Disabled extends LeaderFrontendState {
        Disabled(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree) {
            super(persistenceId, clientId, tree);
        }

        @Override
        LocalHistorySuccess handleLocalHistoryRequest(final LocalHistoryRequest<?> request,
                final RequestEnvelope envelope, final long now) throws RequestException {
            throw new UnsupportedRequestException(request);
        }

        @Override
        TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request,
                final RequestEnvelope envelope, final long now) throws RequestException {
            throw new UnsupportedRequestException(request);
        }
    }

    static final class Enabled extends LeaderFrontendState {
        // Histories which have not been purged
        private final Map<LocalHistoryIdentifier, LocalFrontendHistory> localHistories;

        // UnsignedLongSet performs automatic merging, hence we keep minimal state tracking information
        private final MutableUnsignedLongSet purgedHistories;

        // Used for all standalone transactions
        private final AbstractFrontendHistory standaloneHistory;

        private long expectedTxSequence;
        private Long lastSeenHistory = null;

        Enabled(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree) {
            this(persistenceId, clientId, tree, MutableUnsignedLongSet.of(),
                StandaloneFrontendHistory.create(persistenceId, clientId, tree), new HashMap<>());
        }

        Enabled(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree,
                final MutableUnsignedLongSet purgedHistories, final AbstractFrontendHistory standaloneHistory,
                final Map<LocalHistoryIdentifier, LocalFrontendHistory> localHistories) {
            super(persistenceId, clientId, tree);
            this.purgedHistories = requireNonNull(purgedHistories);
            this.standaloneHistory = requireNonNull(standaloneHistory);
            this.localHistories = requireNonNull(localHistories);
        }

        @Override
        @Nullable LocalHistorySuccess handleLocalHistoryRequest(final LocalHistoryRequest<?> request,
                final RequestEnvelope envelope, final long now) throws RequestException {
            checkRequestSequence(envelope);

            try {
                if (request instanceof CreateLocalHistoryRequest) {
                    return handleCreateHistory((CreateLocalHistoryRequest) request, envelope, now);
                } else if (request instanceof DestroyLocalHistoryRequest) {
                    return handleDestroyHistory((DestroyLocalHistoryRequest) request, envelope, now);
                } else if (request instanceof PurgeLocalHistoryRequest) {
                    return handlePurgeHistory((PurgeLocalHistoryRequest)request, envelope, now);
                } else {
                    LOG.warn("{}: rejecting unsupported request {}", persistenceId(), request);
                    throw new UnsupportedRequestException(request);
                }
            } finally {
                expectNextRequest();
            }
        }

        @Override
        @Nullable TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request,
                final RequestEnvelope envelope, final long now) throws RequestException {
            checkRequestSequence(envelope);

            try {
                final LocalHistoryIdentifier lhId = request.getTarget().getHistoryId();
                final AbstractFrontendHistory history;

                if (lhId.getHistoryId() != 0) {
                    history = localHistories.get(lhId);
                    if (history == null) {
                        if (purgedHistories.contains(lhId.getHistoryId())) {
                            LOG.warn("{}: rejecting request {} to purged history", persistenceId(), request);
                            throw new DeadHistoryException(purgedHistories.toRangeSet());
                        }

                        LOG.warn("{}: rejecting unknown history request {}", persistenceId(), request);
                        throw new UnknownHistoryException(lastSeenHistory);
                    }
                } else {
                    history = standaloneHistory;
                }

                return history.handleTransactionRequest(request, envelope, now);
            } finally {
                expectNextRequest();
            }
        }

        @Override
        void reconnect() {
            expectedTxSequence = 0;
            super.reconnect();
        }

        @Override
        void retire() {
            super.retire();

            // Clear out all transaction chains
            localHistories.values().forEach(AbstractFrontendHistory::retire);
            localHistories.clear();
            standaloneHistory.retire();
        }

        @Override
        ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return super.addToStringAttributes(helper).add("purgedHistories", purgedHistories);
        }

        private LocalHistorySuccess handleCreateHistory(final CreateLocalHistoryRequest request,
                final RequestEnvelope envelope, final long now) throws RequestException {
            final LocalHistoryIdentifier historyId = request.getTarget();
            final AbstractFrontendHistory existing = localHistories.get(historyId);
            if (existing != null) {
                // History already exists: report success
                LOG.debug("{}: history {} already exists", persistenceId(), historyId);
                return new LocalHistorySuccess(historyId, request.getSequence());
            }

            // We have not found the history. Before we create it we need to check history ID sequencing so that we do
            // not end up resurrecting a purged history.
            if (purgedHistories.contains(historyId.getHistoryId())) {
                LOG.debug("{}: rejecting purged request {}", persistenceId(), request);
                throw new DeadHistoryException(purgedHistories.toRangeSet());
            }

            // Update last history we have seen
            if (lastSeenHistory == null || Long.compareUnsigned(lastSeenHistory, historyId.getHistoryId()) < 0) {
                lastSeenHistory = historyId.getHistoryId();
            }

            // We have to send the response only after persistence has completed
            final ShardDataTreeTransactionChain chain = tree().ensureTransactionChain(historyId, () -> {
                LOG.debug("{}: persisted history {}", persistenceId(), historyId);
                envelope.sendSuccess(new LocalHistorySuccess(historyId, request.getSequence()),
                    tree().readTime() - now);
            });

            localHistories.put(historyId, LocalFrontendHistory.create(persistenceId(), tree(), chain));
            LOG.debug("{}: created history {}", persistenceId(), historyId);
            return null;
        }

        private LocalHistorySuccess handleDestroyHistory(final DestroyLocalHistoryRequest request,
                final RequestEnvelope envelope, final long now) {
            final LocalHistoryIdentifier id = request.getTarget();
            final LocalFrontendHistory existing = localHistories.get(id);
            if (existing == null) {
                // History does not exist: report success
                LOG.debug("{}: history {} does not exist, nothing to destroy", persistenceId(), id);
                return new LocalHistorySuccess(id, request.getSequence());
            }

            existing.destroy(request.getSequence(), envelope, now);
            return null;
        }

        private LocalHistorySuccess handlePurgeHistory(final PurgeLocalHistoryRequest request,
                final RequestEnvelope envelope, final long now) {
            final LocalHistoryIdentifier id = request.getTarget();
            final LocalFrontendHistory existing = localHistories.remove(id);
            if (existing == null) {
                LOG.debug("{}: history {} has already been purged", persistenceId(), id);
                return new LocalHistorySuccess(id, request.getSequence());
            }

            LOG.debug("{}: purging history {}", persistenceId(), id);
            purgedHistories.add(id.getHistoryId());
            existing.purge(request.getSequence(), envelope, now);
            return null;
        }

        private void checkRequestSequence(final RequestEnvelope envelope) throws OutOfSequenceEnvelopeException {
            if (expectedTxSequence != envelope.getTxSequence()) {
                throw new OutOfSequenceEnvelopeException(expectedTxSequence);
            }
        }

        private void expectNextRequest() {
            expectedTxSequence++;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LeaderFrontendState.class);

    private final ShardDataTree tree;
    private final ClientIdentifier clientId;
    private final String persistenceId;

    private long lastConnectTicks;
    private long lastSeenTicks;

    // TODO: explicit failover notification
    //       Record the ActorRef for the originating actor and when we switch to being a leader send a notification
    //       to the frontend client -- that way it can immediately start sending requests

    // TODO: add statistics:
    // - number of requests processed
    // - number of histories processed
    // - per-RequestException throw counters

    LeaderFrontendState(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree) {
        this.persistenceId = requireNonNull(persistenceId);
        this.clientId = requireNonNull(clientId);
        this.tree = requireNonNull(tree);
        lastSeenTicks = tree.readTime();
    }

    @Override
    public final ClientIdentifier getIdentifier() {
        return clientId;
    }

    final String persistenceId() {
        return persistenceId;
    }

    final long getLastConnectTicks() {
        return lastConnectTicks;
    }

    final long getLastSeenTicks() {
        return lastSeenTicks;
    }

    final ShardDataTree tree() {
        return tree;
    }

    final void touch() {
        lastSeenTicks = tree.readTime();
    }

    abstract @Nullable LocalHistorySuccess handleLocalHistoryRequest(LocalHistoryRequest<?> request,
            RequestEnvelope envelope, long now) throws RequestException;

    abstract @Nullable TransactionSuccess<?> handleTransactionRequest(TransactionRequest<?> request,
            RequestEnvelope envelope, long now) throws RequestException;

    void reconnect() {
        lastConnectTicks = tree.readTime();
    }

    void retire() {
        // Hunt down any transactions associated with this frontend
        final Iterator<SimpleShardDataTreeCohort> it = tree.cohortIterator();
        while (it.hasNext()) {
            final SimpleShardDataTreeCohort cohort = it.next();
            if (clientId.equals(cohort.getIdentifier().getHistoryId().getClientId())) {
                if (cohort.getState() != State.COMMIT_PENDING) {
                    LOG.debug("{}: Retiring transaction {}", persistenceId, cohort.getIdentifier());
                    it.remove();
                } else {
                    LOG.debug("{}: Transaction {} already committing, not retiring it", persistenceId,
                        cohort.getIdentifier());
                }
            }
        }
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("clientId", clientId).add("nanosAgo", tree.readTime() - lastSeenTicks);
    }
}
