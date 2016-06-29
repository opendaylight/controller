/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.PurgeLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.UnknownHistoryException;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by the shard leader. this class is responsible for tracking generations and sequencing
 * in the frontend/backend conversation.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class LeaderFrontendState {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderFrontendState.class);

    // Histories which have not been purged
    private final Map<LocalHistoryIdentifier, LocalFrontendHistory> localHistories = new HashMap<>();

    // RangeSet performs automatic merging, hence we keep minimal state tracking information
    private final RangeSet<UnsignedLong> purgedHistories = TreeRangeSet.create();

    // Used for all standalone transactions
    private final AbstractFrontendHistory standaloneHistory;
    private final ShardDataTree tree;
    private final ClientIdentifier clientId;
    private final String persistenceId;

    private Long lastSeenHistory = null;


    // TODO: explicit failover notification
    //       Record the ActorRef for the originating actor and when we switch to being a leader send a notification
    //       to the frontend client -- that way it can immediately start sending requests

    // TODO: add statistics:
    // - number of requests processed
    // - number of histories processed
    // - per-RequestException throw counters

    LeaderFrontendState(final String persistenceId, final ClientIdentifier clientId, final ShardDataTree tree) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.clientId = Preconditions.checkNotNull(clientId);
        this.tree = Preconditions.checkNotNull(tree);
        standaloneHistory = new StandaloneFrontendHistory(persistenceId, clientId, tree);
    }

    long getGeneration() {
        return clientId.getGeneration();
    }

    LocalHistorySuccess handleLocalHistoryRequest(final LocalHistoryRequest<?> request, final RequestEnvelope envelope)
            throws RequestException {
        if (request instanceof CreateLocalHistoryRequest) {
            return handleCreateHistory((CreateLocalHistoryRequest) request);
        } else if (request instanceof DestroyLocalHistoryRequest) {
            return handleDestroyHistory((DestroyLocalHistoryRequest) request);
        } else if (request instanceof PurgeLocalHistoryRequest) {
            return handlePurgeHistory((PurgeLocalHistoryRequest)request);
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    private LocalHistorySuccess handleCreateHistory(final CreateLocalHistoryRequest request) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final AbstractFrontendHistory existing = localHistories.get(id);
        if (existing != null) {
            // History already exists: report success
            LOG.debug("{}: history {} already exists", persistenceId, id);
            return new LocalHistorySuccess(id, request.getSequence());
        }

        // We have not found the history. Before we create it we need to check history ID sequencing so that we do not
        // end up resurrecting a purged history.
        if (purgedHistories.contains(UnsignedLong.fromLongBits(id.getHistoryId()))) {
            LOG.debug("{}: rejecting purged request {}", persistenceId, request);
            throw new DeadHistoryException(lastSeenHistory);
        }

        // Update last history we have seen
        if (lastSeenHistory != null && Long.compareUnsigned(lastSeenHistory, id.getHistoryId()) < 0) {
            lastSeenHistory = id.getHistoryId();
        }

        localHistories.put(id, new LocalFrontendHistory(persistenceId, tree.ensureTransactionChain(id)));
        LOG.debug("{}: created history {}", persistenceId, id);
        return new LocalHistorySuccess(id, request.getSequence());
    }

    private LocalHistorySuccess handleDestroyHistory(final DestroyLocalHistoryRequest request) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final LocalFrontendHistory existing = localHistories.get(id);
        if (existing == null) {
            // History does not exist: report success
            LOG.debug("{}: history {} does not exist, nothing to destroy", persistenceId, id);
            return new LocalHistorySuccess(id, request.getSequence());
        }

        return existing.destroy(request.getSequence());
    }

    private LocalHistorySuccess handlePurgeHistory(final PurgeLocalHistoryRequest request) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final LocalFrontendHistory existing = localHistories.remove(id);
        if (existing != null) {
            purgedHistories.add(Range.singleton(UnsignedLong.fromLongBits(id.getHistoryId())));

            if (!existing.isDestroyed()) {
                LOG.warn("{}: purging undestroyed history {}", persistenceId, id);
                existing.destroy(request.getSequence());
            }

            // FIXME: record a PURGE tombstone in the journal

            LOG.debug("{}: purged history {}", persistenceId, id);
        } else {
            LOG.debug("{}: history {} has already been purged", persistenceId, id);
        }

        return new LocalHistorySuccess(id, request.getSequence());
    }

    TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request, final RequestEnvelope envelope)
            throws RequestException {
        final LocalHistoryIdentifier lhId = request.getTarget().getHistoryId();
        final AbstractFrontendHistory history;

        if (lhId.getHistoryId() != 0) {
            history = localHistories.get(lhId);
            if (history == null) {
                LOG.debug("{}: rejecting unknown history request {}", persistenceId, request);
                throw new UnknownHistoryException(lastSeenHistory);
            }
        } else {
            history = standaloneHistory;
        }

        return history.handleTransactionRequest(request, envelope);
    }


    void retire() {
        // FIXME: flush all state
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LeaderFrontendState.class).add("clientId", clientId)
                .add("purgedHistories", purgedHistories).toString();
    }
}
