/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by a shard follower. This class is responsible for maintaining metadata state
 * so that this can be used to seed {@link LeaderFrontendState} with proper state so that the frontend/backend
 * conversation can continue where it left off.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendMetadata extends ShardDataTreeMetadata<FrontendShardDataTreeSnapshotMetadata> {
    private static final class Client implements Identifiable<ClientIdentifier> {
        private final Map<LocalHistoryIdentifier, History> currentHistories = new HashMap<>();
        private final RangeSet<UnsignedLong> purgedHistories = TreeRangeSet.create();
        private final ClientIdentifier identifier;

        Client(ClientIdentifier id) {
            this.identifier = Preconditions.checkNotNull(id);
        }

        @Override
        public ClientIdentifier getIdentifier() {
            return identifier;
        }

        void onTransactionCommitted(TransactionIdentifier txId) {
            ensureHistory(txId.getHistoryId()).onTransactionCommitted(txId);
        }

        private History ensureHistory(LocalHistoryIdentifier historyId) {
            final History existing = currentHistories.get(historyId);
            if (existing != null) {
                return existing;
            }

            final History ret = new History();
            currentHistories.put(historyId, ret);
            return ret;
        }
    }

    private static final class History {
        private long nextTransaction;
        private RequestException failed;
        private boolean closed;

        void onTransactionCommitted(TransactionIdentifier txId) {
            nextTransaction = txId.getTransactionId() + 1;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(FrontendMetadata.class);

    private final Map<FrontendIdentifier, Client> clients = new HashMap<>();

    @Override
    Class<FrontendShardDataTreeSnapshotMetadata> getSupportedType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }

    @Override
    void reset() {
        clients.clear();
    }

    @Override
    void doApplySnapshot(final FrontendShardDataTreeSnapshotMetadata snapshot) {
        // TODO Auto-generated method stub

    }

    @Override
    FrontendShardDataTreeSnapshotMetadata toStapshot() {
        // TODO Auto-generated method stub
        return null;
    }

    private Client ensureClient(ClientIdentifier id) {
        final Client existing = clients.get(id.getFrontendId());
        if (existing != null && id.equals(existing.getIdentifier())) {
            return existing;
        }

        final Client client = new Client(id);
        final Client previous = clients.put(id.getFrontendId(), client);
        if (previous != null) {
            LOG.debug("Replaced client {} with {}", previous, client);
        } else {
            LOG.debug("Added client {}", client);
        }
        return client;
    }

    @Override
    void onTransactionCommitted(final TransactionIdentifier txId) {
        ensureClient(txId.getHistoryId().getClientId()).onTransactionCommitted(txId);
    }
}
