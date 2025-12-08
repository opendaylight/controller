/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only frontend transaction state as observed by the shard leader. This class is NOT thread-safe.
 */
final class FrontendReadOnlyTransaction extends FrontendTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendReadOnlyTransaction.class);

    private final ReadOnlyShardDataTreeTransaction openTransaction;

    FrontendReadOnlyTransaction(final AbstractFrontendHistory history,
            final ReadOnlyShardDataTreeTransaction transaction) {
        super(history, transaction.getIdentifier());
        openTransaction = requireNonNull(transaction);
    }

    // Sequence has already been checked
    @Override
    TransactionSuccess<?> doHandleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        return switch (request) {
            case ExistsTransactionRequest req -> handleExistsTransaction(req);
            case ReadTransactionRequest req -> handleReadTransaction(req);
            case ModifyTransactionRequest req -> handleModifyTransaction(req, envelope, now);
            default -> {
                LOG.warn("Rejecting unsupported request {}", request);
                throw new UnsupportedRequestException(request);
            }
        };
    }

    @Override
    void retire() {
        // No-op
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request,
            final RequestEnvelope envelope, final long now) {
        // The only valid request here is with abort protocol
        final var proto = request.getPersistenceProtocol()
            .orElseThrow(() -> new IllegalArgumentException("Commit protocol is missing in " + request));
        if (proto != PersistenceProtocol.ABORT) {
            throw new IllegalArgumentException("Unsupported commit protocol in " + proto);
        }

        openTransaction.abort(() -> recordAndSendSuccess(envelope, now,
            new ModifyTransactionSuccess(request.getTarget(), request.getSequence())));
        return null;
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request) {
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), openTransaction.getSnapshot().readNode(request.getPath()).isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request) {
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), openTransaction.getSnapshot().readNode(request.getPath())));
    }
}
