/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Globally-unique identifier of a transaction.
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionIdentifier implements WritableIdentifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private LocalHistoryIdentifier historyId;
        private long transactionId;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier historyId, final long transactionId) {
            this.historyId = requireNonNull(historyId);
            this.transactionId = transactionId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            historyId.writeTo(out);
            WritableObjects.writeLong(out, transactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            historyId = LocalHistoryIdentifier.readFrom(in);
            transactionId = WritableObjects.readLong(in);
        }

        private Object readResolve() {
            return new TransactionIdentifier(historyId, transactionId);
        }
    }

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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TransactionIdentifier)) {
            return false;
        }

        final TransactionIdentifier other = (TransactionIdentifier) obj;
        return transactionId == other.transactionId && historyId.equals(other.historyId);
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

    private Object writeReplace() {
        return new Proxy(historyId, transactionId);
    }
}
