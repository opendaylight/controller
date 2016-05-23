/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Globally-unique identifier of a transaction.
 *
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
@Beta
public final class TransactionIdentifier<T extends FrontendType> implements Identifier, WritableObject {
    private static final class Proxy<T extends FrontendType> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private LocalHistoryIdentifier<T> historyId;
        private long transactionId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final LocalHistoryIdentifier<T> historyId, final long transactionId) {
            this.historyId = Preconditions.checkNotNull(historyId);
            this.transactionId = transactionId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            historyId.writeTo(out);
            out.writeLong(transactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            historyId = LocalHistoryIdentifier.readFrom(in);
            transactionId = in.readLong();
        }

        private Object readResolve() {
            return new TransactionIdentifier<>(historyId, transactionId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final LocalHistoryIdentifier<T> historyId;
    private final long transactionId;

    public TransactionIdentifier(final @Nonnull LocalHistoryIdentifier<T> historyId, final long transactionId) {
        this.historyId = Preconditions.checkNotNull(historyId);
        this.transactionId = transactionId;
    }

    public static <T extends FrontendType> TransactionIdentifier<T> readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
        final LocalHistoryIdentifier<T> historyId = LocalHistoryIdentifier.readFrom(in);
        return new TransactionIdentifier<>(historyId, in.readLong());
    }

    @Override
    public void writeTo(ObjectOutput out) throws IOException {
        historyId.writeTo(out);
        out.writeLong(transactionId);
    }

    public LocalHistoryIdentifier<T> getHistoryId() {
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

        final TransactionIdentifier<?> other = (TransactionIdentifier<?>) o;
        return transactionId == other.transactionId && historyId.equals(other.historyId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(TransactionIdentifier.class).add("history", historyId)
                .add("transaction", transactionId).toString();
    }

    private Object writeReplace() {
        return new Proxy<>(historyId, transactionId);
    }
}
