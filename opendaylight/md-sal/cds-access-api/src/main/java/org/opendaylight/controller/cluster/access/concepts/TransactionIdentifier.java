/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
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

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier historyId, final long transactionId) {
            this.historyId = Preconditions.checkNotNull(historyId);
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
    private final LocalHistoryIdentifier historyId;
    private final long transactionId;
    private transient String shortString;

    public TransactionIdentifier(final @Nonnull LocalHistoryIdentifier historyId, final long transactionId) {
        this.historyId = Preconditions.checkNotNull(historyId);
        this.transactionId = transactionId;
    }

    public static TransactionIdentifier readFrom(final DataInput in) throws IOException {
        final LocalHistoryIdentifier historyId = LocalHistoryIdentifier.readFrom(in);
        return new TransactionIdentifier(historyId, WritableObjects.readLong(in));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        historyId.writeTo(out);
        WritableObjects.writeLong(out, transactionId);
    }

    public LocalHistoryIdentifier getHistoryId() {
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransactionIdentifier)) {
            return false;
        }

        final TransactionIdentifier other = (TransactionIdentifier) o;
        return transactionId == other.transactionId && historyId.equals(other.historyId);
    }

    public String toShortString() {
        if(shortString == null) {
            String histStr = historyId.getHistoryId() == 0 ? "" : "-chn-" + historyId.getHistoryId();
            shortString = historyId.getClientId().getFrontendId().getMemberName().getName() + "-" +
                    historyId.getClientId().getFrontendId().getClientType().getName() + "-fe-" +
                    historyId.getClientId().getGeneration() + histStr + "-txn-" + transactionId;
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
