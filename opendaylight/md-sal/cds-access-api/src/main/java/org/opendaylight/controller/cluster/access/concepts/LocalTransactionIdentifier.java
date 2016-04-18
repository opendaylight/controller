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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public final class LocalTransactionIdentifier implements Identifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private long localHistory;
        private long transactionId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final long localHistory, final long transactionId) {
            this.localHistory = localHistory;
            this.transactionId = transactionId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(localHistory);
            out.writeLong(transactionId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            this.localHistory = in.readLong();
            this.transactionId = in.readLong();
        }

        @SuppressWarnings("unused")
        private LocalTransactionIdentifier readResolve() {
            return new LocalTransactionIdentifier(localHistory, transactionId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final long localHistory;
    private final long transactionId;

    public LocalTransactionIdentifier(final long localHistory, final long transactionId) {
        this.localHistory = localHistory;
        this.transactionId = transactionId;
    }

    public long getLocalHistory() {
        return localHistory;
    }

    public long getTransactionId() {
        return transactionId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(localHistory) * 31 + Long.hashCode(transactionId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalTransactionIdentifier)) {
            return false;
        }

        final LocalTransactionIdentifier other = (LocalTransactionIdentifier) o;
        return localHistory == other.localHistory && transactionId == other.transactionId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalTransactionIdentifier.class).add("localHistory", localHistory)
                .add("transactionId", transactionId).toString();
    }

    @SuppressWarnings("unused")
    private Proxy writeReplace() {
        return new Proxy(localHistory, transactionId);
    }
}
