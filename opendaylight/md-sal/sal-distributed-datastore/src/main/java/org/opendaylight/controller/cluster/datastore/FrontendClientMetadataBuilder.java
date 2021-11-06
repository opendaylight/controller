/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.controller.cluster.datastore.utils.UnsignedLongSet;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is NOT thread-safe.
 */
abstract class FrontendClientMetadataBuilder implements Builder<FrontendClientMetadata>,
        Identifiable<ClientIdentifier> {
    static final class Disabled extends FrontendClientMetadataBuilder {
        Disabled(final String shardName, final ClientIdentifier identifier) {
            super(shardName, identifier);
        }

        @Override
        public FrontendClientMetadata build() {
            return new FrontendClientMetadata(getIdentifier(), ImmutableRangeSet.of(), ImmutableList.of());
        }

        @Override
        void onHistoryCreated(final LocalHistoryIdentifier historyId) {
            // No-op
        }

        @Override
        void onHistoryClosed(final LocalHistoryIdentifier historyId) {
            // No-op
        }

        @Override
        void onHistoryPurged(final LocalHistoryIdentifier historyId) {
            // No-op
        }

        @Override
        void onTransactionAborted(final TransactionIdentifier txId) {
            // No-op
        }

        @Override
        void onTransactionCommitted(final TransactionIdentifier txId) {
            // No-op
        }

        @Override
        void onTransactionPurged(final TransactionIdentifier txId) {
            // No-op
        }

        @Override
        LeaderFrontendState toLeaderState(final Shard shard) {
            return new LeaderFrontendState.Disabled(shard.persistenceId(), getIdentifier(), shard.getDataStore());
        }
    }

    static final class Enabled extends FrontendClientMetadataBuilder {
        private final Map<LocalHistoryIdentifier, FrontendHistoryMetadataBuilder> currentHistories = new HashMap<>();
        private final LocalHistoryIdentifier standaloneId;
        private final UnsignedLongSet purgedHistories;

        Enabled(final String shardName, final ClientIdentifier identifier) {
            super(shardName, identifier);

            purgedHistories = UnsignedLongSet.of();

            // History for stand-alone transactions is always present
            standaloneId = standaloneHistoryId();
            currentHistories.put(standaloneId, new FrontendHistoryMetadataBuilder(standaloneId));
        }

        Enabled(final String shardName, final FrontendClientMetadata meta) {
            super(shardName, meta.getIdentifier());

            purgedHistories = UnsignedLongSet.of(meta.getPurgedHistories());
            for (FrontendHistoryMetadata h : meta.getCurrentHistories()) {
                final FrontendHistoryMetadataBuilder b = new FrontendHistoryMetadataBuilder(getIdentifier(), h);
                currentHistories.put(b.getIdentifier(), b);
            }

            // Sanity check and recovery
            standaloneId = standaloneHistoryId();
            if (!currentHistories.containsKey(standaloneId)) {
                LOG.warn("{}: Client {} recovered histories {} do not contain stand-alone history, attempting recovery",
                    shardName, getIdentifier(), currentHistories);
                currentHistories.put(standaloneId, new FrontendHistoryMetadataBuilder(standaloneId));
            }
        }

        @Override
        public FrontendClientMetadata build() {
            return new FrontendClientMetadata(getIdentifier(), purgedHistories.toRangeSet(),
                Collections2.transform(currentHistories.values(), FrontendHistoryMetadataBuilder::build));
        }

        @Override
        void onHistoryCreated(final LocalHistoryIdentifier historyId) {
            final FrontendHistoryMetadataBuilder newMeta = new FrontendHistoryMetadataBuilder(historyId);
            final FrontendHistoryMetadataBuilder oldMeta = currentHistories.putIfAbsent(historyId, newMeta);
            if (oldMeta != null) {
                // This should not be happening, warn about it
                LOG.warn("{}: Reused local history {}", shardName(), historyId);
            } else {
                LOG.debug("{}: Created local history {}", shardName(), historyId);
            }
        }

        @Override
        void onHistoryClosed(final LocalHistoryIdentifier historyId) {
            final FrontendHistoryMetadataBuilder builder = currentHistories.get(historyId);
            if (builder != null) {
                builder.onHistoryClosed();
                LOG.debug("{}: Closed history {}", shardName(), historyId);
            } else {
                LOG.warn("{}: Closed unknown history {}, ignoring", shardName(), historyId);
            }
        }

        @Override
        void onHistoryPurged(final LocalHistoryIdentifier historyId) {
            final FrontendHistoryMetadataBuilder history = currentHistories.remove(historyId);
            final long historyBits = historyId.getHistoryId();
            if (history == null) {
                if (!purgedHistories.contains(historyBits)) {
                    purgedHistories.add(historyBits);
                    LOG.warn("{}: Purging unknown history {}", shardName(), historyId);
                } else {
                    LOG.warn("{}: Duplicate purge of history {}", shardName(), historyId);
                }
            } else {
                purgedHistories.add(historyBits);
                LOG.debug("{}: Purged history {}", shardName(), historyId);
            }
        }

        @Override
        void onTransactionAborted(final TransactionIdentifier txId) {
            final FrontendHistoryMetadataBuilder history = getHistory(txId);
            if (history != null) {
                history.onTransactionAborted(txId);
                LOG.debug("{}: Aborted transaction {}", shardName(), txId);
            } else {
                LOG.warn("{}: Unknown history for aborted transaction {}, ignoring", shardName(), txId);
            }
        }

        @Override
        void onTransactionCommitted(final TransactionIdentifier txId) {
            final FrontendHistoryMetadataBuilder history = getHistory(txId);
            if (history != null) {
                history.onTransactionCommitted(txId);
                LOG.debug("{}: Committed transaction {}", shardName(), txId);
            } else {
                LOG.warn("{}: Unknown history for commited transaction {}, ignoring", shardName(), txId);
            }
        }

        @Override
        void onTransactionPurged(final TransactionIdentifier txId) {
            final FrontendHistoryMetadataBuilder history = getHistory(txId);
            if (history != null) {
                history.onTransactionPurged(txId);
                LOG.debug("{}: Purged transaction {}", shardName(), txId);
            } else {
                LOG.warn("{}: Unknown history for purged transaction {}, ignoring", shardName(), txId);
            }
        }

        @Override
        LeaderFrontendState toLeaderState(final Shard shard) {
            // Note: we have to make sure to *copy* all current state and not leak any views, otherwise leader/follower
            //       interactions would get intertwined leading to inconsistencies.
            final Map<LocalHistoryIdentifier, LocalFrontendHistory> histories = new HashMap<>();
            for (FrontendHistoryMetadataBuilder e : currentHistories.values()) {
                if (e.getIdentifier().getHistoryId() != 0) {
                    final AbstractFrontendHistory state = e.toLeaderState(shard);
                    verify(state instanceof LocalFrontendHistory, "Unexpected state %s", state);
                    histories.put(e.getIdentifier(), (LocalFrontendHistory) state);
                }
            }

            final AbstractFrontendHistory singleHistory;
            final FrontendHistoryMetadataBuilder singleHistoryMeta = currentHistories.get(
                new LocalHistoryIdentifier(getIdentifier(), 0));
            if (singleHistoryMeta == null) {
                final ShardDataTree tree = shard.getDataStore();
                singleHistory = StandaloneFrontendHistory.create(shard.persistenceId(), getIdentifier(), tree);
            } else {
                singleHistory = singleHistoryMeta.toLeaderState(shard);
            }

            return new LeaderFrontendState.Enabled(shard.persistenceId(), getIdentifier(), shard.getDataStore(),
                purgedHistories.copy(), singleHistory, histories);
        }

        @Override
        ToStringHelper addToStringAttributes(final ToStringHelper helper) {
            return super.addToStringAttributes(helper).add("current", currentHistories).add("purged", purgedHistories);
        }

        private FrontendHistoryMetadataBuilder getHistory(final TransactionIdentifier txId) {
            LocalHistoryIdentifier historyId = txId.getHistoryId();
            if (historyId.getHistoryId() == 0 && historyId.getCookie() != 0) {
                // We are pre-creating the history for free-standing transactions with a zero cookie, hence our lookup
                // needs to account for that.
                LOG.debug("{}: looking up {} instead of {}", shardName(), standaloneId, historyId);
                historyId = standaloneId;
            }

            return currentHistories.get(historyId);
        }

        private LocalHistoryIdentifier standaloneHistoryId() {
            return new LocalHistoryIdentifier(getIdentifier(), 0);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(FrontendClientMetadataBuilder.class);

    private final @NonNull ClientIdentifier identifier;
    private final @NonNull String shardName;

    FrontendClientMetadataBuilder(final String shardName, final ClientIdentifier identifier) {
        this.shardName = requireNonNull(shardName);
        this.identifier = requireNonNull(identifier);
    }

    static FrontendClientMetadataBuilder of(final String shardName, final FrontendClientMetadata meta) {
        final Collection<FrontendHistoryMetadata> current = meta.getCurrentHistories();
        final RangeSet<UnsignedLong> purged = meta.getPurgedHistories();

        // Completely empty histories imply disabled state, as otherwise we'd have a record of the single history --
        // either purged or active
        return current.isEmpty() && purged.isEmpty() ? new Disabled(shardName, meta.getIdentifier())
                : new Enabled(shardName, meta);
    }

    @Override
    public final ClientIdentifier getIdentifier() {
        return identifier;
    }

    final String shardName() {
        return shardName;
    }

    abstract void onHistoryCreated(LocalHistoryIdentifier historyId);

    abstract void onHistoryClosed(LocalHistoryIdentifier historyId);

    abstract void onHistoryPurged(LocalHistoryIdentifier historyId);

    abstract void onTransactionAborted(TransactionIdentifier txId);

    abstract void onTransactionCommitted(TransactionIdentifier txId);

    abstract void onTransactionPurged(TransactionIdentifier txId);

    /**
     * Transform frontend metadata for a particular client into its {@link LeaderFrontendState} counterpart.
     *
     * @param shard parent shard
     * @return Leader frontend state
     */
    abstract @NonNull LeaderFrontendState toLeaderState(@NonNull Shard shard);

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("identifier", identifier);
    }
}
