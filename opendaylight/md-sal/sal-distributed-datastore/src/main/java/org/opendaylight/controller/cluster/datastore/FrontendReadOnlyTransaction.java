/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Read-only frontend transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendReadOnlyTransaction extends FrontendTransaction {
    private final ReadOnlyShardDataTreeTransaction openTransaction;

    private FrontendReadOnlyTransaction(final AbstractFrontendHistory history,
            final ReadOnlyShardDataTreeTransaction transaction) {
        super(history, transaction.getIdentifier());
        this.openTransaction = Preconditions.checkNotNull(transaction);
    }

    static FrontendReadOnlyTransaction create(final AbstractFrontendHistory history,
            final ReadOnlyShardDataTreeTransaction transaction) {
        return new FrontendReadOnlyTransaction(history, transaction);
    }

    // Sequence has already been checked
    @Override
    @Nullable TransactionSuccess<?> handleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        if (request instanceof ExistsTransactionRequest) {
            return handleExistsTransaction((ExistsTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            return handleReadTransaction((ReadTransactionRequest) request);
        } else if (request instanceof TransactionAbortRequest) {
            handleTransactionAbort((TransactionAbortRequest) request, envelope, now);
            return null;
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    private void handleTransactionAbort(final TransactionAbortRequest request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        openTransaction.abort(() -> recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(request.getTarget(),
            request.getSequence())));
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(openTransaction.getIdentifier(),
            request.getSequence(), data));
    }
}
