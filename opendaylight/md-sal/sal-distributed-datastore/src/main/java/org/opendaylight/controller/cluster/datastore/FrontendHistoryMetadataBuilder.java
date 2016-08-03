/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;

final class FrontendHistoryMetadataBuilder implements Builder<FrontendHistoryMetadata>, Identifiable<LocalHistoryIdentifier> {
    private final LocalHistoryIdentifier identifier;

    private long nextTransaction;
    private boolean closed;

    FrontendHistoryMetadataBuilder(final LocalHistoryIdentifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    FrontendHistoryMetadataBuilder(final ClientIdentifier clientId, final FrontendHistoryMetadata meta) {
        identifier = new LocalHistoryIdentifier(clientId, meta.getHistoryId(), meta.getCookie());
        nextTransaction = meta.getNextTransaction();
        closed = meta.isClosed();
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public FrontendHistoryMetadata build() {
        return new FrontendHistoryMetadata(identifier.getHistoryId(), identifier.getCookie(), nextTransaction, closed);
    }

    void onHistoryClosed() {
        closed = true;
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        nextTransaction = txId.getTransactionId() + 1;
    }
}
