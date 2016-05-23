/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Globally-unique identifier of a local history.
 *
 * @param <T> Frontend type
 *
 * @author Robert Varga
 */
public final class LocalHistoryIdentifier<T extends FrontendType> implements Identifier, WritableObject {
    private static final class Proxy<T extends FrontendType> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private ClientIdentifier<T> clientId;
        private long historyId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final ClientIdentifier<T> frontendId, final long historyId) {
            this.clientId = Preconditions.checkNotNull(frontendId);
            this.historyId = historyId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            clientId.writeTo(out);
            out.writeLong(historyId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            clientId = ClientIdentifier.readFrom(in);
            historyId = in.readLong();
        }

        private Object readResolve() {
            return new LocalHistoryIdentifier<>(clientId, historyId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final ClientIdentifier<T> clientId;
    private final long historyId;

    public LocalHistoryIdentifier(final ClientIdentifier<T> frontendId, final long historyId) {
        this.clientId = Preconditions.checkNotNull(frontendId);
        this.historyId = historyId;
    }

    public static <T extends FrontendType> LocalHistoryIdentifier<T> readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
        final ClientIdentifier<T> clientId = ClientIdentifier.readFrom(in);
        return new LocalHistoryIdentifier<>(clientId, in.readLong());
    }

    @Override
    public void writeTo(ObjectOutput out) throws IOException {
        clientId.writeTo(out);
        out.writeLong(historyId);
    }

    public ClientIdentifier<T> getClienId() {
        return clientId;
    }

    public long getHistoryId() {
        return historyId;
    }

    @Override
    public int hashCode() {
        return clientId.hashCode() * 31 + Long.hashCode(historyId);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalHistoryIdentifier)) {
            return false;
        }

        final LocalHistoryIdentifier<?> other = (LocalHistoryIdentifier<?>) o;
        return historyId == other.historyId && clientId.equals(other.clientId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalHistoryIdentifier.class).add("client", clientId)
                .add("history", Long.toUnsignedString(historyId)).toString();
    }

    private Object writeReplace() {
        return new Proxy<>(clientId, historyId);
    }
}
