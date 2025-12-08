/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.controller.cluster.raft.spi.ImmutableUnsignedLongSet;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;
import org.opendaylight.controller.cluster.raft.spi.UnsignedLongBitmap;
import org.opendaylight.yangtools.concepts.Identifiable;

final class FrontendHistoryMetadataBuilder implements Identifiable<LocalHistoryIdentifier> {
    private final @NonNull Map<UnsignedLong, Boolean> closedTransactions;
    private final @NonNull MutableUnsignedLongSet purgedTransactions;
    private final @NonNull LocalHistoryIdentifier identifier;

    private boolean closed;

    FrontendHistoryMetadataBuilder(final LocalHistoryIdentifier identifier) {
        this.identifier = requireNonNull(identifier);
        purgedTransactions = MutableUnsignedLongSet.of();
        closedTransactions = new HashMap<>(2);
    }

    FrontendHistoryMetadataBuilder(final ClientIdentifier clientId, final FrontendHistoryMetadata meta) {
        identifier = new LocalHistoryIdentifier(clientId, meta.getHistoryId(), meta.getCookie());
        closedTransactions = meta.getClosedTransactions().mutableCopy();
        purgedTransactions = meta.getPurgedTransactions().mutableCopy();
        closed = meta.isClosed();
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    public FrontendHistoryMetadata build() {
        return new FrontendHistoryMetadata(identifier.getHistoryId(), identifier.getCookie(), closed,
            UnsignedLongBitmap.copyOf(closedTransactions), purgedTransactions.immutableCopy());
    }

    void onHistoryClosed() {
        checkState(identifier.getHistoryId() != 0);
        closed = true;
    }

    void onTransactionAborted(final TransactionIdentifier txId) {
        closedTransactions.put(UnsignedLong.fromLongBits(txId.getTransactionId()), Boolean.FALSE);
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        closedTransactions.put(UnsignedLong.fromLongBits(txId.getTransactionId()), Boolean.TRUE);
    }

    void onTransactionPurged(final TransactionIdentifier txId) {
        final long txidBits = txId.getTransactionId();
        closedTransactions.remove(UnsignedLong.fromLongBits(txidBits));
        purgedTransactions.add(txidBits);
    }

    void onTransactionsSkipped(final ImmutableUnsignedLongSet txIds) {
        purgedTransactions.addAll(txIds);
    }

    /**
     * Transform frontend metadata for a particular client history into its {@link LocalFrontendHistory} counterpart.
     *
     * @param shard parent shard
     * @return Leader history state
     */
    @NonNull AbstractFrontendHistory toLeaderState(final @NonNull Shard shard) {
        if (identifier.getHistoryId() == 0) {
            return StandaloneFrontendHistory.recreate(shard.memberId(), identifier.getClientId(),
                shard.getDataStore(), closedTransactions, purgedTransactions);
        }

        return LocalFrontendHistory.recreate(shard.memberId(),
            shard.getDataStore().recreateChainedParent(identifier, closed), closedTransactions, purgedTransactions);
    }
}
