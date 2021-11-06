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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ClosedTransactionException;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.DeadTransactionException;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.MutableUnsignedLongSet;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalone transactions and chained transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractFrontendHistory implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFrontendHistory.class);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final MutableUnsignedLongSet purgedTransactions;
    private final String persistenceId;
    private final ShardDataTree tree;

    /**
     * Transactions closed by the previous leader. Boolean indicates whether the transaction was committed (true) or
     * aborted (false). We only ever shrink these.
     */
    private Map<UnsignedLong, Boolean> closedTransactions;

    AbstractFrontendHistory(final String persistenceId, final ShardDataTree tree,
            final Map<UnsignedLong, Boolean> closedTransactions, final MutableUnsignedLongSet purgedTransactions) {
        this.persistenceId = requireNonNull(persistenceId);
        this.tree = requireNonNull(tree);
        this.closedTransactions = requireNonNull(closedTransactions);
        this.purgedTransactions = requireNonNull(purgedTransactions);
    }

    final String persistenceId() {
        return persistenceId;
    }

    final long readTime() {
        return tree.readTime();
    }

    final @Nullable TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        if (request instanceof TransactionPurgeRequest) {
            return handleTransactionPurgeRequest(request, envelope, now);
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
                LOG.warn("{}: no transaction state present, unexpected request {}", persistenceId(), request);
                throw new OutOfOrderRequestException(0);
            }

            tx = createTransaction(request, id);
            transactions.put(id, tx);
        } else if (!(request instanceof IncrementTransactionSequenceRequest)) {
            final Optional<TransactionSuccess<?>> maybeReplay = tx.replaySequence(request.getSequence());
            if (maybeReplay.isPresent()) {
                final TransactionSuccess<?> replay = maybeReplay.get();
                LOG.debug("{}: envelope {} replaying response {}", persistenceId(), envelope, replay);
                return replay;
            }
        }

        return tx.handleRequest(request, envelope, now);
    }

    private TransactionSuccess<?> handleTransactionPurgeRequest(final TransactionRequest<?> request,
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
            tree.purgeTransaction(id, () -> {
                closedTransactions.remove(ul);
                if (closedTransactions.isEmpty()) {
                    closedTransactions = ImmutableMap.of();
                }

                purgedTransactions.add(txidBits);
                LOG.debug("{}: finished purging inherited transaction {}", persistenceId(), id);
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

        tree.purgeTransaction(id, () -> {
            purgedTransactions.add(txidBits);
            transactions.remove(id);
            LOG.debug("{}: finished purging transaction {}", persistenceId(), id);
            envelope.sendSuccess(new TransactionPurgeResponse(id, request.getSequence()), readTime() - now);
        });

        return null;
    }

    final void destroy(final long sequence, final RequestEnvelope envelope, final long now) {
        LOG.debug("{}: closing history {}", persistenceId(), getIdentifier());
        tree.closeTransactionChain(getIdentifier(),
            () -> envelope.sendSuccess(new LocalHistorySuccess(getIdentifier(), sequence), readTime() - now));
    }

    final void purge(final long sequence, final RequestEnvelope envelope, final long now) {
        LOG.debug("{}: purging history {}", persistenceId(), getIdentifier());
        tree.purgeTransactionChain(getIdentifier(),
            () -> envelope.sendSuccess(new LocalHistorySuccess(getIdentifier(), sequence), readTime() - now));
    }

    final void retire() {
        transactions.values().forEach(FrontendTransaction::retire);
        tree.removeTransactionChain(getIdentifier());
    }

    private FrontendTransaction createTransaction(final TransactionRequest<?> request, final TransactionIdentifier id) {
        if (request instanceof CommitLocalTransactionRequest) {
            LOG.debug("{}: allocating new ready transaction {}", persistenceId(), id);
            tree.getStats().incrementReadWriteTransactionCount();
            return createReadyTransaction(id, ((CommitLocalTransactionRequest) request).getModification());
        }
        if (request instanceof AbstractReadTransactionRequest
                && ((AbstractReadTransactionRequest<?>) request).isSnapshotOnly()) {
            LOG.debug("{}: allocating new open snapshot {}", persistenceId(), id);
            tree.getStats().incrementReadOnlyTransactionCount();
            return createOpenSnapshot(id);
        }

        LOG.debug("{}: allocating new open transaction {}", persistenceId(), id);
        tree.getStats().incrementReadWriteTransactionCount();
        return createOpenTransaction(id);
    }

    abstract FrontendTransaction createOpenSnapshot(TransactionIdentifier id);

    abstract FrontendTransaction createOpenTransaction(TransactionIdentifier id);

    abstract FrontendTransaction createReadyTransaction(TransactionIdentifier id, DataTreeModification mod)
        ;

    abstract ShardDataTreeCohort createFailedCohort(TransactionIdentifier id, DataTreeModification mod,
            Exception failure);

    abstract ShardDataTreeCohort createReadyCohort(TransactionIdentifier id, DataTreeModification mod,
            Optional<SortedSet<String>> participatingShardNames);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("identifier", getIdentifier())
                .add("persistenceId", persistenceId).add("transactions", transactions).toString();
    }
}
