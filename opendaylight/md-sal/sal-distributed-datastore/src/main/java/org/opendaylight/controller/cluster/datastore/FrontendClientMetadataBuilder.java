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
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.raft.spi.ImmutableUnsignedLongSet;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is NOT thread-safe.
 */
final class FrontendClientMetadataBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendClientMetadataBuilder.class);

    private final HashMap<LocalHistoryIdentifier, FrontendHistoryMetadataBuilder> currentHistories = new HashMap<>();
    private final @NonNull MutableUnsignedLongSet purgedHistories;
    private final @NonNull LocalHistoryIdentifier standaloneId;
    private final @NonNull ClientIdentifier clientId;
    private final @NonNull String shardName;

    FrontendClientMetadataBuilder(final String shardName, final ClientIdentifier clientId) {
        this.shardName = requireNonNull(shardName);
        this.clientId = requireNonNull(clientId);

        purgedHistories = MutableUnsignedLongSet.of();

        // History for stand-alone transactions is always present
        standaloneId = StandaloneFrontendHistory.identifierForClient(clientId);
        currentHistories.put(standaloneId, new FrontendHistoryMetadataBuilder(standaloneId));
    }

    FrontendClientMetadataBuilder(final String shardName, final FrontendClientMetadata meta) {
        this.shardName = requireNonNull(shardName);
        clientId = meta.clientId();

        purgedHistories = meta.purgedHistories().mutableCopy();
        for (var historyMeta : meta.currentHistories()) {
            final var builder = new FrontendHistoryMetadataBuilder(clientId, historyMeta);
            currentHistories.put(builder.getIdentifier(), builder);
        }

        // Sanity check and recovery
        standaloneId = StandaloneFrontendHistory.identifierForClient(clientId);
        currentHistories.computeIfAbsent(standaloneId, missingId -> {
            LOG.warn("{}: Client {} recovered histories {} do not contain stand-alone history, attempting recovery",
                shardName, clientId, currentHistories);
            return new FrontendHistoryMetadataBuilder(missingId);
        });
    }

    @NonNull ClientIdentifier clientId() {
        return clientId;
    }

    FrontendClientMetadata build() {
        return new FrontendClientMetadata(clientId, purgedHistories.immutableCopy(),
            currentHistories.values().stream()
                .map(FrontendHistoryMetadataBuilder::build)
                .collect(ImmutableList.toImmutableList()));
    }

    /**
     * Transform frontend metadata for a particular client into its {@link LeaderFrontendState} counterpart.
     *
     * @param shard parent shard
     * @return Leader frontend state
     */
    @NonNull LeaderFrontendState toLeaderState(final @NonNull Shard shard) {
        // Note: we have to make sure to *copy* all current state and not leak any views, otherwise leader/follower
        //       interactions would get intertwined leading to inconsistencies.
        final var histories = new HashMap<LocalHistoryIdentifier, LocalFrontendHistory>();
        for (var historyMetaBuilder : currentHistories.values()) {
            final var historyId = historyMetaBuilder.getIdentifier();
            if (historyId.getHistoryId() != 0) {
                final var state = historyMetaBuilder.toLeaderState(shard);
                if (!(state instanceof LocalFrontendHistory localState)) {
                    throw new VerifyException("Unexpected state " + state);
                }
                histories.put(historyId, localState);
            }
        }

        final AbstractFrontendHistory singleHistory;
        final var singleHistoryMeta = currentHistories.get(new LocalHistoryIdentifier(clientId, 0));
        if (singleHistoryMeta == null) {
            singleHistory = StandaloneFrontendHistory.create(shard.memberId(), clientId, shard.getDataStore());
        } else {
            singleHistory = singleHistoryMeta.toLeaderState(shard);
        }

        return new LeaderFrontendState.Enabled(shard.memberId(), clientId, shard.getDataStore(),
            purgedHistories.mutableCopy(), singleHistory, histories);
    }

    void onHistoryCreated(final LocalHistoryIdentifier historyId) {
        final var newMeta = new FrontendHistoryMetadataBuilder(historyId);
        final var oldMeta = currentHistories.putIfAbsent(historyId, newMeta);
        if (oldMeta != null) {
            // This should not be happening, warn about it
            LOG.warn("{}: Reused local history {}", shardName, historyId);
        } else {
            LOG.debug("{}: Created local history {}", shardName, historyId);
        }
    }

    void onHistoryClosed(final LocalHistoryIdentifier historyId) {
        final var builder = currentHistories.get(historyId);
        if (builder != null) {
            builder.onHistoryClosed();
            LOG.debug("{}: Closed history {}", shardName, historyId);
        } else {
            LOG.warn("{}: Closed unknown history {}, ignoring", shardName, historyId);
        }
    }

    void onHistoryPurged(final LocalHistoryIdentifier historyId) {
        final var history = currentHistories.remove(historyId);
        final long historyBits = historyId.getHistoryId();
        if (history == null) {
            if (!purgedHistories.contains(historyBits)) {
                purgedHistories.add(historyBits);
                LOG.warn("{}: Purging unknown history {}", shardName, historyId);
            } else {
                LOG.warn("{}: Duplicate purge of history {}", shardName, historyId);
            }
        } else {
            purgedHistories.add(historyBits);
            LOG.debug("{}: Purged history {}", shardName, historyId);
        }
    }

    void onTransactionAborted(final TransactionIdentifier txId) {
        final var history = getHistory(txId);
        if (history != null) {
            history.onTransactionAborted(txId);
            LOG.debug("{}: Aborted transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for aborted transaction {}, ignoring", shardName, txId);
        }
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        final var history = getHistory(txId);
        if (history != null) {
            history.onTransactionCommitted(txId);
            LOG.debug("{}: Committed transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for commited transaction {}, ignoring", shardName, txId);
        }
    }

    void onTransactionPurged(final TransactionIdentifier txId) {
        final var history = getHistory(txId);
        if (history != null) {
            history.onTransactionPurged(txId);
            LOG.debug("{}: Purged transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for purged transaction {}, ignoring", shardName, txId);
        }
    }

    void onTransactionsSkipped(final LocalHistoryIdentifier historyId, final ImmutableUnsignedLongSet txIds) {
        final var history = getHistory(historyId);
        if (history != null) {
            history.onTransactionsSkipped(txIds);
            LOG.debug("{}: History {} skipped transactions {}", shardName, historyId, txIds);
        } else {
            LOG.warn("{}: Unknown history {} for skipped transactions, ignoring", shardName, historyId);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("clientId", clientId)
            .add("current", currentHistories)
            .add("purged", purgedHistories)
            .toString();
    }

    private FrontendHistoryMetadataBuilder getHistory(final TransactionIdentifier txId) {
        return getHistory(txId.getHistoryId());
    }

    private FrontendHistoryMetadataBuilder getHistory(final LocalHistoryIdentifier historyId) {
        final LocalHistoryIdentifier local;
        if (historyId.getHistoryId() == 0 && historyId.getCookie() != 0) {
            // We are pre-creating the history for free-standing transactions with a zero cookie, hence our lookup
            // needs to account for that.
            LOG.debug("{}: looking up {} instead of {}", shardName, standaloneId, historyId);
            local = standaloneId;
        } else {
            local = historyId;
        }

        return currentHistories.get(local);
    }
}
