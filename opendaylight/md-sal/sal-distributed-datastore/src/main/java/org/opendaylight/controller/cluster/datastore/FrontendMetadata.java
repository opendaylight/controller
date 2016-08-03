/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.Collections2;
import java.util.HashMap;
import java.util.Map;
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
        clients.clear();

        for (FrontendClientMetadata m : snapshot.getClients()) {
            clients.put(m.getIdentifier().getFrontendId(), new FrontendClientMetadataBuilder(m));
        }
    }

    @Override
    FrontendShardDataTreeSnapshotMetadata toStapshot() {
        return new FrontendShardDataTreeSnapshotMetadata(Collections2.transform(clients.values(),
            FrontendClientMetadataBuilder::build));
    }

    private FrontendClientMetadataBuilder ensureClient(final ClientIdentifier id) {
        final FrontendClientMetadataBuilder existing = clients.get(id.getFrontendId());
        if (existing != null && id.equals(existing.getIdentifier())) {
            return existing;
        }

        final FrontendClientMetadataBuilder client = new FrontendClientMetadataBuilder(id);
        final FrontendClientMetadataBuilder previous = clients.put(id.getFrontendId(), client);
        if (previous != null) {
            LOG.debug("Replaced client {} with {}", previous, client);
        } else {
            LOG.debug("Added client {}", client);
        }
        return client;
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
    void onTransactionCommitted(final TransactionIdentifier txId) {
        ensureClient(txId.getHistoryId().getClientId()).onTransactionCommitted(txId);
    }
}
