/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
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
    private static final Logger LOG = LoggerFactory.getLogger(FrontendMetadata.class);

    private final Map<FrontendIdentifier, FrontendClientMetadataBuilder> clients = new HashMap<>();
    private final String shardName;

    FrontendMetadata(final String shardName) {
        this.shardName = Preconditions.checkNotNull(shardName);
    }

    @Override
    Class<FrontendShardDataTreeSnapshotMetadata> getSupportedType() {
        return FrontendShardDataTreeSnapshotMetadata.class;
    }

    @Override
    void reset() {
        LOG.debug("{}: clearing clients {}", shardName, clients);
        clients.clear();
    }

    @Override
    void doApplySnapshot(final FrontendShardDataTreeSnapshotMetadata snapshot) {
        LOG.debug("{}: applying snapshot {} over clients {}", shardName, snapshot, clients);
        clients.clear();

        for (FrontendClientMetadata m : snapshot.getClients()) {
            LOG.debug("{}: applying metadata {}", shardName, m);
            final FrontendClientMetadataBuilder b = new FrontendClientMetadataBuilder(shardName, m);
            final FrontendIdentifier client = m.getIdentifier().getFrontendId();

            LOG.debug("{}: client {} updated to {}", shardName, client, b);
            clients.put(client, b);
        }
    }

    @Override
    FrontendShardDataTreeSnapshotMetadata toSnapshot() {
        return new FrontendShardDataTreeSnapshotMetadata(Collections2.transform(clients.values(),
            FrontendClientMetadataBuilder::build));
    }

    private FrontendClientMetadataBuilder ensureClient(final ClientIdentifier id) {
        final FrontendClientMetadataBuilder existing = clients.get(id.getFrontendId());
        if (existing != null && id.equals(existing.getIdentifier())) {
            return existing;
        }

        final FrontendClientMetadataBuilder client = new FrontendClientMetadataBuilder(shardName, id);
        final FrontendClientMetadataBuilder previous = clients.put(id.getFrontendId(), client);
        if (previous != null) {
            LOG.debug("{}: Replaced client {} with {}", shardName, previous, client);
        } else {
            LOG.debug("{}: Added client {}", shardName, client);
        }
        return client;
    }

    @Override
    void onHistoryCreated(final LocalHistoryIdentifier historyId) {
        ensureClient(historyId.getClientId()).onHistoryCreated(historyId);
    }

    @Override
    void onHistoryClosed(final LocalHistoryIdentifier historyId) {
        ensureClient(historyId.getClientId()).onHistoryClosed(historyId);
    }

    @Override
    void onHistoryPurged(final LocalHistoryIdentifier historyId) {
        ensureClient(historyId.getClientId()).onHistoryPurged(historyId);
    }

    @Override
    void onTransactionAborted(final TransactionIdentifier txId) {
        ensureClient(txId.getHistoryId().getClientId()).onTransactionAborted(txId);
    }

    @Override
    void onTransactionCommitted(final TransactionIdentifier txId) {
        ensureClient(txId.getHistoryId().getClientId()).onTransactionCommitted(txId);
    }

    @Override
    void onTransactionPurged(final TransactionIdentifier txId) {
        ensureClient(txId.getHistoryId().getClientId()).onTransactionPurged(txId);
    }

    /**
     * Transform frontend metadata into an active leader state map.
     *
     * @return Leader frontend state
     */
    @Nonnull Map<FrontendIdentifier, LeaderFrontendState> toLeaderState(@Nonnull final Shard shard) {
        return new HashMap<>(Maps.transformValues(clients, meta -> meta.toLeaderState(shard)));
    }
}
