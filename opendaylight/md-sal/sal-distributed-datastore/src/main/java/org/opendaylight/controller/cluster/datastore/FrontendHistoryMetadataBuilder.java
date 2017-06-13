/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;

final class FrontendHistoryMetadataBuilder implements Builder<FrontendHistoryMetadata>,
        Identifiable<LocalHistoryIdentifier> {

    private final Map<UnsignedLong, Boolean> closedTransactions;
    private final RangeSet<UnsignedLong> purgedTransactions;
    private final LocalHistoryIdentifier identifier;

    private boolean closed;

    FrontendHistoryMetadataBuilder(final LocalHistoryIdentifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.purgedTransactions = TreeRangeSet.create();
        this.closedTransactions = new HashMap<>(2);
    }

    FrontendHistoryMetadataBuilder(final ClientIdentifier clientId, final FrontendHistoryMetadata meta) {
        identifier = new LocalHistoryIdentifier(clientId, meta.getHistoryId(), meta.getCookie());
        closedTransactions = new HashMap<>(meta.getClosedTransactions());
        purgedTransactions = TreeRangeSet.create(meta.getPurgedTransactions());
        closed = meta.isClosed();
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public FrontendHistoryMetadata build() {
        return new FrontendHistoryMetadata(identifier.getHistoryId(), identifier.getCookie(), closed,
            closedTransactions, purgedTransactions);
    }

    void onHistoryClosed() {
        Preconditions.checkState(identifier.getHistoryId() != 0);
        closed = true;
    }

    void onTransactionAborted(final TransactionIdentifier txId) {
        closedTransactions.put(UnsignedLong.fromLongBits(txId.getTransactionId()), Boolean.FALSE);
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        closedTransactions.put(UnsignedLong.fromLongBits(txId.getTransactionId()), Boolean.TRUE);
    }

    void onTransactionPurged(final TransactionIdentifier txId) {
        final UnsignedLong id = UnsignedLong.fromLongBits(txId.getTransactionId());
        closedTransactions.remove(id);
        purgedTransactions.add(Range.closedOpen(id, UnsignedLong.ONE.plus(id)));
    }

    /**
     * Transform frontend metadata for a particular client history into its {@link LocalFrontendHistory} counterpart.
     *
     * @param shard parent shard
     * @return Leader history state
     */
    @Nonnull AbstractFrontendHistory toLeaderState(@Nonnull final Shard shard) {
        if (identifier.getHistoryId() == 0) {
            return StandaloneFrontendHistory.recreate(shard.persistenceId(), identifier.getClientId(),
                shard.getDataStore(), closedTransactions, purgedTransactions);
        }

        return LocalFrontendHistory.recreate(shard.persistenceId(), shard.getDataStore(),
            shard.getDataStore().recreateTransactionChain(identifier, closed), closedTransactions, purgedTransactions);
    }
}
