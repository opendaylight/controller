/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Globally-unique identifier of a transaction.
 */
public final class TransactionIdentifier implements WritableIdentifier {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull LocalHistoryIdentifier historyId;
    private final long transactionId;
    private String shortString;

    public TransactionIdentifier(final @NonNull LocalHistoryIdentifier historyId, final long transactionId) {
        this.historyId = requireNonNull(historyId);
        this.transactionId = transactionId;
    }

    public static @NonNull TransactionIdentifier readFrom(final DataInput in) throws IOException {
        final LocalHistoryIdentifier historyId = LocalHistoryIdentifier.readFrom(in);
        return new TransactionIdentifier(historyId, WritableObjects.readLong(in));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        historyId.writeTo(out);
        WritableObjects.writeLong(out, transactionId);
    }

    public @NonNull LocalHistoryIdentifier getHistoryId() {
        return historyId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    @Override
    public int hashCode() {
        return historyId.hashCode() * 31 + Long.hashCode(transactionId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof TransactionIdentifier other && transactionId == other.transactionId
            && historyId.equals(other.historyId);
    }

    public String toShortString() {
        if (shortString == null) {
            String histStr = historyId.getHistoryId() == 0 ? "" : "-chn-" + historyId.getHistoryId();
            shortString = historyId.getClientId().getFrontendId().getMemberName().getName() + "-"
                    + historyId.getClientId().getFrontendId().getClientType().getName() + "-fe-"
                    + historyId.getClientId().getGeneration() + histStr + "-txn-" + transactionId
                    + "-" + historyId.getCookie();
        }

        return shortString;
    }

    @Override
    public String toString() {
        return toShortString();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new TI(this);
    }
}
