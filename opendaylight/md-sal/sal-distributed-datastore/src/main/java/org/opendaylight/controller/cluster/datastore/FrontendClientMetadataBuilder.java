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
import com.google.common.base.Verify;
import com.google.common.collect.Collections2;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class FrontendClientMetadataBuilder implements Builder<FrontendClientMetadata>, Identifiable<ClientIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendClientMetadataBuilder.class);

    private final Map<LocalHistoryIdentifier, FrontendHistoryMetadataBuilder> currentHistories = new HashMap<>();
    private final RangeSet<UnsignedLong> purgedHistories;
    private final ClientIdentifier identifier;
    private final String shardName;

    FrontendClientMetadataBuilder(final String shardName, final ClientIdentifier identifier) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.identifier = Preconditions.checkNotNull(identifier);
        purgedHistories = TreeRangeSet.create();

        // History for stand-alone transactions is always present
        final LocalHistoryIdentifier standaloneId = standaloneHistoryId();
        currentHistories.put(standaloneId, new FrontendHistoryMetadataBuilder(standaloneId));
    }

    FrontendClientMetadataBuilder(final String shardName, final FrontendClientMetadata meta) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.identifier = Preconditions.checkNotNull(meta.getIdentifier());
        purgedHistories = TreeRangeSet.create(meta.getPurgedHistories());

        for (FrontendHistoryMetadata h : meta.getCurrentHistories()) {
            final FrontendHistoryMetadataBuilder b = new FrontendHistoryMetadataBuilder(identifier, h);
            currentHistories.put(b.getIdentifier(), b);
        }

        // Sanity check and recovery
        final LocalHistoryIdentifier standaloneId = standaloneHistoryId();
        if (!currentHistories.containsKey(standaloneId)) {
            LOG.warn("{}: Client {} recovered histories {} do not contain stand-alone history, attempting recovery",
                shardName, identifier, currentHistories);
            currentHistories.put(standaloneId, new FrontendHistoryMetadataBuilder(standaloneId));
        }
    }

    private LocalHistoryIdentifier standaloneHistoryId() {
        return new LocalHistoryIdentifier(identifier, 0);
    }

    @Override
    public FrontendClientMetadata build() {
        return new FrontendClientMetadata(identifier, purgedHistories,
            Collections2.transform(currentHistories.values(), FrontendHistoryMetadataBuilder::build));
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    void onHistoryCreated(final LocalHistoryIdentifier historyId) {
        final FrontendHistoryMetadataBuilder newMeta = new FrontendHistoryMetadataBuilder(historyId);
        final FrontendHistoryMetadataBuilder oldMeta = currentHistories.putIfAbsent(historyId, newMeta);
        if (oldMeta != null) {
            // This should not be happening, warn about it
            LOG.warn("{}: Reused local history {}", shardName, historyId);
        } else {
            LOG.debug("{}: Created local history {}", shardName, historyId);
        }
    }

    void onHistoryClosed(final LocalHistoryIdentifier historyId) {
        final FrontendHistoryMetadataBuilder builder = currentHistories.get(historyId);
        if (builder != null) {
            builder.onHistoryClosed();
            LOG.debug("{}: Closed history {}", shardName, historyId);
        } else {
            LOG.warn("{}: Closed unknown history {}, ignoring", shardName, historyId);
        }
    }

    void onHistoryPurged(final LocalHistoryIdentifier historyId) {
        final FrontendHistoryMetadataBuilder history = currentHistories.remove(historyId);
        if (history == null) {
            LOG.warn("{}: Purging unknown history {}", shardName, historyId);
        }

        // XXX: do we need to account for cookies?
        final UnsignedLong ul = UnsignedLong.fromLongBits(historyId.getHistoryId());
        purgedHistories.add(Range.closedOpen(ul, UnsignedLong.ONE.plus(ul)));
        LOG.debug("{}: Purged history {}", historyId);
    }

    void onTransactionAborted(final TransactionIdentifier txId) {
        final FrontendHistoryMetadataBuilder history = getHistory(txId);
        if (history != null) {
            history.onTransactionAborted(txId);
            LOG.debug("{}: Aborted transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for aborted transaction {}, ignoring", shardName, txId);
        }
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        final FrontendHistoryMetadataBuilder history = getHistory(txId);
        if (history != null) {
            history.onTransactionCommitted(txId);
            LOG.debug("{}: Committed transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for commited transaction {}, ignoring", shardName, txId);
        }
    }

    void onTransactionPurged(final TransactionIdentifier txId) {
        final FrontendHistoryMetadataBuilder history = getHistory(txId);
        if (history != null) {
            history.onTransactionPurged(txId);
            LOG.debug("{}: Purged transaction {}", shardName, txId);
        } else {
            LOG.warn("{}: Unknown history for purged transaction {}, ignoring", shardName, txId);
        }
    }

    /**
     * Transform frontend metadata for a particular client into its {@link LeaderFrontendState} counterpart.
     *
     * @param shard parent shard
     * @return Leader frontend state
     */
    @Nonnull LeaderFrontendState toLeaderState(@Nonnull final Shard shard) {
        // Note: we have to make sure to *copy* all current state and not leak any views, otherwise leader/follower
        //       interactions would get intertwined leading to inconsistencies.
        final Map<LocalHistoryIdentifier, LocalFrontendHistory> histories = new HashMap<>();
        for (FrontendHistoryMetadataBuilder e : currentHistories.values()) {
            if (e.getIdentifier().getHistoryId() != 0) {
                final AbstractFrontendHistory state = e.toLeaderState(shard);
                Verify.verify(state instanceof LocalFrontendHistory);
                histories.put(e.getIdentifier(), (LocalFrontendHistory) state);
            }
        }

        final AbstractFrontendHistory singleHistory;
        final FrontendHistoryMetadataBuilder singleHistoryMeta = currentHistories.get(
            new LocalHistoryIdentifier(identifier, 0));
        if (singleHistoryMeta == null) {
            final ShardDataTree tree = shard.getDataStore();
            singleHistory = StandaloneFrontendHistory.create(shard.persistenceId(), getIdentifier(), tree);
        } else {
            singleHistory = singleHistoryMeta.toLeaderState(shard);
        }

        return new LeaderFrontendState(shard.persistenceId(), getIdentifier(), shard.getDataStore(),
            TreeRangeSet.create(purgedHistories), singleHistory, histories);
    }

    private FrontendHistoryMetadataBuilder getHistory(final TransactionIdentifier txId) {
        return currentHistories.get(txId.getHistoryId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("identifier", identifier).add("current", currentHistories)
                .add("purged", purgedHistories).toString();
    }
}
