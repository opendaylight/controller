/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Optional;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only frontend transaction state as observed by the shard leader. This class is NOT thread-safe.
 *
 * @author Robert Varga
 */
final class FrontendReadOnlyTransaction extends FrontendTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendReadOnlyTransaction.class);

    private final ReadOnlyShardDataTreeTransaction openTransaction;

    private FrontendReadOnlyTransaction(final AbstractFrontendHistory history,
            final ReadOnlyShardDataTreeTransaction transaction) {
        super(history, transaction.getIdentifier());
        this.openTransaction = requireNonNull(transaction);
    }

    static FrontendReadOnlyTransaction create(final AbstractFrontendHistory history,
            final ReadOnlyShardDataTreeTransaction transaction) {
        return new FrontendReadOnlyTransaction(history, transaction);
    }

    // Sequence has already been checked
    @Override
    TransactionSuccess<?> doHandleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        if (request instanceof ExistsTransactionRequest) {
            return handleExistsTransaction((ExistsTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            return handleReadTransaction((ReadTransactionRequest) request);
        } else if (request instanceof ModifyTransactionRequest) {
            handleModifyTransaction((ModifyTransactionRequest) request, envelope, now);
            return null;
        } else {
            LOG.warn("Rejecting unsupported request {}", request);
            throw new UnsupportedRequestException(request);
        }
    }

    @Override
    void retire() {
        // No-op
    }

    private void handleModifyTransaction(final ModifyTransactionRequest request, final RequestEnvelope envelope,
            final long now) {
        // The only valid request here is with abort protocol
        final Optional<PersistenceProtocol> optProto = request.getPersistenceProtocol();
        checkArgument(optProto.isPresent(), "Commit protocol is missing in %s", request);
        checkArgument(optProto.get() == PersistenceProtocol.ABORT, "Unsupported commit protocol in %s", request);
        openTransaction.abort(() -> recordAndSendSuccess(envelope, now,
            new ModifyTransactionSuccess(request.getTarget(), request.getSequence())));
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request) {
        final Optional<NormalizedNode> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request) {
        final Optional<NormalizedNode> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), data));
    }
}
