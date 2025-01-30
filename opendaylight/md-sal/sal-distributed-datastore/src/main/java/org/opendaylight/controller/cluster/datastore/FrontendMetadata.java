/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
import org.opendaylight.controller.cluster.raft.spi.ImmutableUnsignedLongSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by a shard follower. This class is responsible for maintaining metadata state so that this
 * can be used to seed {@link LeaderFrontendState} with proper state so that the frontend/backend conversation can
 * continue where it left off. This class is NOT thread-safe.
 */
final class FrontendMetadata extends ShardDataTreeMetadata<FrontendShardDataTreeSnapshotMetadata> {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendMetadata.class);

    private final Map<FrontendIdentifier, FrontendClientMetadataBuilder> clients = new HashMap<>();
    private final String shardName;

    FrontendMetadata(final String shardName) {
        this.shardName = requireNonNull(shardName);
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

        for (var clientMeta : snapshot.getClients()) {
            LOG.debug("{}: applying metadata {}", shardName, clientMeta);
            final var builder = new FrontendClientMetadataBuilder(shardName, clientMeta);
            final var frontendId = clientMeta.clientId().getFrontendId();

            LOG.debug("{}: client {} updated to {}", shardName, frontendId, builder);
            clients.put(frontendId, builder);
        }
    }

    @Override
    FrontendShardDataTreeSnapshotMetadata toSnapshot() {
        return new FrontendShardDataTreeSnapshotMetadata(Collections2.transform(clients.values(),
            FrontendClientMetadataBuilder::build));
    }

    private FrontendClientMetadataBuilder ensureClient(final ClientIdentifier id) {
        final var existing = clients.get(id.getFrontendId());
        if (existing != null && id.equals(existing.clientId())) {
            return existing;
        }

        final var client = new FrontendClientMetadataBuilder(shardName, id);
        final var previous = clients.put(id.getFrontendId(), client);
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

    @Override
    void onTransactionsSkipped(final LocalHistoryIdentifier historyId, final ImmutableUnsignedLongSet txIds) {
        ensureClient(historyId.getClientId()).onTransactionsSkipped(historyId, txIds);
    }

    /**
     * Transform frontend metadata into an active leader state map.
     *
     * @return Leader frontend state
     */
    @NonNull Map<FrontendIdentifier, LeaderFrontendState> toLeaderState(final @NonNull Shard shard) {
        return new HashMap<>(Maps.transformValues(clients, meta -> meta.toLeaderState(shard)));
    }

    ImmutableSet<ClientIdentifier> getClients() {
        return clients.values().stream()
            .map(FrontendClientMetadataBuilder::clientId)
            .collect(ImmutableSet.toImmutableSet());
    }
}
