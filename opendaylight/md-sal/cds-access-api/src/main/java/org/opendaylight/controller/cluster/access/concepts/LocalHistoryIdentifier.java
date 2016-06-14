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
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Globally-unique identifier of a local history.
 *
 * @author Robert Varga
 */
public final class LocalHistoryIdentifier implements WritableIdentifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private ClientIdentifier clientId;
        private long historyId;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final ClientIdentifier frontendId, final long historyId) {
            this.clientId = Preconditions.checkNotNull(frontendId);
            this.historyId = historyId;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            clientId.writeTo(out);
            WritableObjects.writeLong(out, historyId);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            clientId = ClientIdentifier.readFrom(in);
            historyId = WritableObjects.readLong(in);
        }

        private Object readResolve() {
            return new LocalHistoryIdentifier(clientId, historyId);
        }
    }

    private static final long serialVersionUID = 1L;
    private final ClientIdentifier clientId;
    private final long historyId;

    public LocalHistoryIdentifier(final ClientIdentifier frontendId, final long historyId) {
        this.clientId = Preconditions.checkNotNull(frontendId);
        this.historyId = historyId;
    }

    public static LocalHistoryIdentifier readFrom(final DataInput in) throws IOException {
        final ClientIdentifier clientId = ClientIdentifier.readFrom(in);
        return new LocalHistoryIdentifier(clientId, WritableObjects.readLong(in));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        clientId.writeTo(out);
        WritableObjects.writeLong(out, historyId);
    }

    public ClientIdentifier getClientId() {
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

        final LocalHistoryIdentifier other = (LocalHistoryIdentifier) o;
        return historyId == other.historyId && clientId.equals(other.clientId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalHistoryIdentifier.class).add("client", clientId)
                .add("history", Long.toUnsignedString(historyId)).toString();
    }

    private Object writeReplace() {
        return new Proxy(clientId, historyId);
    }
}
