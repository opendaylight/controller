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
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
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
final class FrontendState {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendState.class);

    private final Map<LocalHistoryIdentifier, FrontendLocalHistory> localHistories = new HashMap<>();
    private final ClientIdentifier clientId;
    private final String persistenceId;

    private Long lastSeenHistory = null;

    FrontendState(final String persistenceId, final ClientIdentifier clientId) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.clientId = Preconditions.checkNotNull(clientId);
    }

    long getGeneration() {
        return clientId.getGeneration();
    }

    LocalHistorySuccess handleLocalHistoryRequest(final LocalHistoryRequest<?> request, final long sequence) throws RequestException {
        if (request instanceof CreateLocalHistoryRequest) {
            return handleCreateHistory((CreateLocalHistoryRequest) request, sequence);
        } else if (request instanceof DestroyLocalHistoryRequest) {
            return handleDestroyHistory((DestroyLocalHistoryRequest) request, sequence);
        } else if (request instanceof PurgeLocalHistoryRequest) {
            return handlePurgeHistory((PurgeLocalHistoryRequest)request, sequence);
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request, final long sequence) throws RequestException {
        final LocalHistoryIdentifier lhId = request.getTarget().getHistoryId();
        if (lastSeenHistory != null && Long.compareUnsigned(lastSeenHistory, lhId.getHistoryId()) < 0) {
            // This is a history we have not seen created
            LOG.debug("{}: last known history is {}, rejecting request {}", persistenceId, lastSeenHistory, request);
            throw new UnknownLocalHistoryException(lastSeenHistory);
        }

        final FrontendLocalHistory history = localHistories.get(lhId);
        if (history == null) {
            LOG.debug("{}: rejecting unknown history request {}", persistenceId, request);
            throw new DeadHistoryException(lastSeenHistory);
        }

        return history.handleTransactionRequest(request, sequence);
    }

    private LocalHistorySuccess handleCreateHistory(final CreateLocalHistoryRequest request, final long sequence) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final FrontendLocalHistory existing = localHistories.get(id);
        if (existing != null) {
            // History already exists: report success
            return new LocalHistorySuccess(id);
        }

        // We have not found the history. Before we create it we need to check history ID sequencing so that we do not
        // end up resurrecting a purged history.
        if (lastSeenHistory != null && Long.compareUnsigned(lastSeenHistory, id.getHistoryId()) >= 0) {
            LOG.debug("{}: last history is {}, rejecting request {}", persistenceId, lastSeenHistory, request);
            throw new DeadHistoryException(lastSeenHistory);
        }

        // Update last history we have seen
        lastSeenHistory = id.getHistoryId();


        localHistories.put(id, new FrontendLocalHistory(persistenceId, id));
        return new LocalHistorySuccess(id);
    }

    private LocalHistorySuccess handleDestroyHistory(final DestroyLocalHistoryRequest request, final long sequence) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final FrontendLocalHistory existing = localHistories.get(id);
        if (existing == null) {
            // History does not exist: report success
            return new LocalHistorySuccess(id);
        }

        return existing.destroy();
    }

    private LocalHistorySuccess handlePurgeHistory(final PurgeLocalHistoryRequest request, final long sequence) throws RequestException {
        final LocalHistoryIdentifier id = request.getTarget();
        final FrontendLocalHistory existing = localHistories.get(id);
        if (existing != null) {
            if (!existing.isDestroyed()) {
                LOG.warn("{}: purging undestroyed history {}", persistenceId, id);
                existing.destroy();
            }

            localHistories.remove(id);
        }

        return new LocalHistorySuccess(id);
    }

    void retire() {
        // FIXME: flush all state
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendState.class).add("clientId", clientId).toString();
    }
}
