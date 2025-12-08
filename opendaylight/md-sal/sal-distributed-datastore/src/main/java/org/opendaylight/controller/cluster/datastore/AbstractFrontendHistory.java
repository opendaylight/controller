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
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ClosedTransactionException;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.DeadTransactionException;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.SkipTransactionsRequest;
import org.opendaylight.controller.cluster.access.commands.SkipTransactionsResponse;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.raft.spi.MutableUnsignedLongSet;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalone transactions and chained transactions.
 */
abstract class AbstractFrontendHistory implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFrontendHistory.class);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final @NonNull MutableUnsignedLongSet purgedTransactions;
    private final @NonNull TransactionParent parent;
    private final @NonNull String persistenceId;

    /**
     * Transactions closed by the previous leader. Boolean indicates whether the transaction was committed (true) or
     * aborted (false). We only ever shrink these.
     */
    private Map<UnsignedLong, Boolean> closedTransactions;

    AbstractFrontendHistory(final String persistenceId, final TransactionParent parent,
            final Map<UnsignedLong, Boolean> closedTransactions, final MutableUnsignedLongSet purgedTransactions) {
        this.persistenceId = requireNonNull(persistenceId);
        this.parent = requireNonNull(parent);
        this.closedTransactions = requireNonNull(closedTransactions);
        this.purgedTransactions = requireNonNull(purgedTransactions);
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return parent.historyId;
    }

    final String persistenceId() {
        return persistenceId;
    }

    final long readTime() {
        return parent.dataTree.readTime();
    }

    final @Nullable TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        if (request instanceof TransactionPurgeRequest purgeRequest) {
            return handleTransactionPurgeRequest(purgeRequest, envelope, now);
        }
        if (request instanceof SkipTransactionsRequest skipRequest) {
            return handleSkipTransactionsRequest(skipRequest, envelope, now);
        }

        final TransactionIdentifier id = request.getTarget();
        final long txidBits = id.getTransactionId();
        if (purgedTransactions.contains(txidBits)) {
            LOG.warn("{}: Request {} is contained purged transactions {}", persistenceId, request, purgedTransactions);
            throw new DeadTransactionException(purgedTransactions.toRangeSet());
        }

        final Boolean closed = closedTransactions.get(UnsignedLong.fromLongBits(txidBits));
        if (closed != null) {
            final boolean successful = closed;
            LOG.debug("{}: Request {} refers to a {} transaction", persistenceId, request, successful ? "successful"
                    : "failed");
            throw new ClosedTransactionException(successful);
        }

        FrontendTransaction tx = transactions.get(id);
        if (tx == null) {
            // The transaction does not exist and we are about to create it, check sequence number
            if (request.getSequence() != 0) {
                LOG.warn("{}: no transaction state present, unexpected request {}", persistenceId, request);
                throw new OutOfOrderRequestException(0);
            }

            tx = createTransaction(request, id);
            transactions.put(id, tx);
        } else if (!(request instanceof IncrementTransactionSequenceRequest)) {
            final Optional<TransactionSuccess<?>> maybeReplay = tx.replaySequence(request.getSequence());
            if (maybeReplay.isPresent()) {
                final TransactionSuccess<?> replay = maybeReplay.orElseThrow();
                LOG.debug("{}: envelope {} replaying response {}", persistenceId, envelope, replay);
                return replay;
            }
        }

        return tx.handleRequest(request, envelope, now);
    }

    private TransactionPurgeResponse handleTransactionPurgeRequest(final TransactionPurgeRequest request,
            final RequestEnvelope envelope, final long now) {
        final TransactionIdentifier id = request.getTarget();
        final long txidBits = id.getTransactionId();
        if (purgedTransactions.contains(txidBits)) {
            // Retransmitted purge request: nothing to do
            LOG.debug("{}: transaction {} already purged", persistenceId, id);
            return new TransactionPurgeResponse(id, request.getSequence());
        }

        // We perform two lookups instead of a straight remove, because once the map becomes empty we switch it
        // to an ImmutableMap, which does not allow remove().
        final UnsignedLong ul = UnsignedLong.fromLongBits(txidBits);
        if (closedTransactions.containsKey(ul)) {
            parent.dataTree.purgeTransaction(id, () -> {
                closedTransactions.remove(ul);
                if (closedTransactions.isEmpty()) {
                    closedTransactions = ImmutableMap.of();
                }

                purgedTransactions.add(txidBits);
                LOG.debug("{}: finished purging inherited transaction {}", persistenceId, id);
                envelope.sendSuccess(new TransactionPurgeResponse(id, request.getSequence()), readTime() - now);
            });
            return null;
        }

        final FrontendTransaction tx = transactions.get(id);
        if (tx == null) {
            // This should never happen because the purge callback removes the transaction and puts it into
            // purged transactions in one go. If it does, we warn about the situation and
            LOG.warn("{}: transaction {} not tracked in {}, but not present in active transactions", persistenceId,
                id, purgedTransactions);
            purgedTransactions.add(txidBits);
            return new TransactionPurgeResponse(id, request.getSequence());
        }

        parent.dataTree.purgeTransaction(id, () -> {
            purgedTransactions.add(txidBits);
            transactions.remove(id);
            LOG.debug("{}: finished purging transaction {}", persistenceId, id);
            envelope.sendSuccess(new TransactionPurgeResponse(id, request.getSequence()), readTime() - now);
        });

        return null;
    }

    private SkipTransactionsResponse handleSkipTransactionsRequest(final SkipTransactionsRequest request,
            final RequestEnvelope envelope, final long now) {
        final var first = request.getTarget();
        final var others = request.getOthers();
        final var ids = new ArrayList<UnsignedLong>(others.size() + 1);
        ids.add(UnsignedLong.fromLongBits(first.getTransactionId()));
        ids.addAll(others);

        final var it = ids.iterator();
        while (it.hasNext()) {
            final var id = it.next();
            final long bits = id.longValue();
            if (purgedTransactions.contains(bits)) {
                LOG.warn("{}: history {} tracks {} as purged", persistenceId, getIdentifier(), id);
                it.remove();
            } else if (transactions.containsKey(new TransactionIdentifier(getIdentifier(), bits))) {
                LOG.warn("{}: history {} tracks {} as open", persistenceId, getIdentifier(), id);
                it.remove();
            }
        }

        if (ids.isEmpty()) {
            LOG.debug("{}: history {} completing empty skip request", persistenceId, getIdentifier());
            return new SkipTransactionsResponse(first, now);
        }

        final var transactionIds = MutableUnsignedLongSet.of(ids.stream().mapToLong(UnsignedLong::longValue).toArray())
            .immutableCopy();
        LOG.debug("{}: history {} skipping transactions {}", persistenceId, getIdentifier(), transactionIds.ranges());

        parent.dataTree.skipTransactions(getIdentifier(), transactionIds, () -> {
            purgedTransactions.addAll(transactionIds);
            envelope.sendSuccess(new TransactionPurgeResponse(first, request.getSequence()), readTime() - now);
        });
        return null;
    }

    final void destroy(final long sequence, final RequestEnvelope envelope, final long now) {
        LOG.debug("{}: closing history {}", persistenceId, getIdentifier());
        parent.dataTree.closeTransactionChain(getIdentifier(),
            () -> envelope.sendSuccess(new LocalHistorySuccess(getIdentifier(), sequence), readTime() - now));
    }

    final void purge(final long sequence, final RequestEnvelope envelope, final long now) {
        LOG.debug("{}: purging history {}", persistenceId, getIdentifier());
        parent.dataTree.purgeTransactionChain(getIdentifier(),
            () -> envelope.sendSuccess(new LocalHistorySuccess(getIdentifier(), sequence), readTime() - now));
    }

    final void retire() {
        transactions.values().forEach(FrontendTransaction::retire);
        parent.dataTree.removeTransactionChain(getIdentifier());
    }

    @NonNullByDefault
    private FrontendTransaction createTransaction(final TransactionRequest<?> request, final TransactionIdentifier id) {
        return switch (request) {
            case CommitLocalTransactionRequest req -> {
                LOG.debug("{}: allocating new ready transaction {}", persistenceId, id);
                parent.dataTree.getStats().incrementReadWriteTransactionCount();
                yield FrontendReadWriteTransaction.createReady(this, id, req.getModification());
            }
            case AbstractReadTransactionRequest<?> req when req.isSnapshotOnly() -> {
                LOG.debug("{}: allocating new open snapshot {}", persistenceId, id);
                parent.dataTree.getStats().incrementReadOnlyTransactionCount();
                yield new FrontendReadOnlyTransaction(this, parent.newReadOnlyTransaction(id));
            }
            default -> {
                LOG.debug("{}: allocating new open transaction {}", persistenceId, id);
                parent.dataTree.getStats().incrementReadWriteTransactionCount();
                yield FrontendReadWriteTransaction.createOpen(this, parent.newReadWriteTransaction(id));
            }
        };
    }

    @NonNullByDefault
    final CommitCohort createFailedCohort(final TransactionIdentifier id, final DataTreeModification mod,
            final Exception failure) {
        return parent.createFailedCohort(id, mod, failure);
    }

    @NonNullByDefault
    final CommitCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod) {
        return parent.createReadyCohort(id, mod);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
            .add("identifier", getIdentifier())
            .add("persistenceId", persistenceId)
            .add("transactions", transactions)
            .toString();
    }
}
