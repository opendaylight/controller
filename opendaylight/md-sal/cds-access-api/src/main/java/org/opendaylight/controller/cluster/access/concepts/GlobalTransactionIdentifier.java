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

@Beta
public final class GlobalTransactionIdentifier implements Identifier {
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
            out.writeObject(historyId);
            out.writeLong(transactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            historyId = (LocalHistoryIdentifier) in.readObject();
            transactionId = in.readLong();
        }

        @SuppressWarnings("unused")
        private GlobalTransactionIdentifier readResolve() {
            return new GlobalTransactionIdentifier(historyId, transactionId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final LocalHistoryIdentifier historyId;
    private final long transactionId;

    public GlobalTransactionIdentifier(final @Nonnull LocalHistoryIdentifier frontendId, final long transactionId) {
        this.historyId = Preconditions.checkNotNull(frontendId);
        this.transactionId = transactionId;
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
        if (!(o instanceof GlobalTransactionIdentifier)) {
            return false;
        }

        final GlobalTransactionIdentifier other = (GlobalTransactionIdentifier) o;
        return transactionId == other.transactionId && historyId.equals(other.historyId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(GlobalTransactionIdentifier.class).add("history", historyId)
                .add("transaction", transactionId).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(historyId, transactionId);
    }
}
