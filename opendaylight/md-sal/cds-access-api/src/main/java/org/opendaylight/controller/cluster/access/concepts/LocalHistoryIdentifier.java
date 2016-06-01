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
 * Globally-unique identifier of a local history. This identifier is assigned on the frontend and is coposed of
 * - a {@link ClientIdentifier}, which uniquely identifies a single instantiation of a particular frontend
 * - an unsigned long, which uniquely identifies the history on the backend
 * - an unsigned long cookie, assigned by the client and meaningless on the backend, which just reflects it back
 *
 * @author Robert Varga
 */
public final class LocalHistoryIdentifier implements WritableIdentifier {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private ClientIdentifier clientId;
        private long historyId;
        private long cookie;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final ClientIdentifier frontendId, final long historyId, final long cookie) {
            this.clientId = Preconditions.checkNotNull(frontendId);
            this.historyId = historyId;
            this.cookie = cookie;
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
            return new LocalHistoryIdentifier(clientId, historyId, cookie);
        }
    }

    private static final long serialVersionUID = 1L;
    private final ClientIdentifier clientId;
    private final long historyId;
    private final long cookie;

    public LocalHistoryIdentifier(final ClientIdentifier frontendId, final long historyId) {
        this(frontendId, historyId, 0);
    }

    public LocalHistoryIdentifier(final ClientIdentifier frontendId, final long historyId, final long cookie) {
        this.clientId = Preconditions.checkNotNull(frontendId);
        this.historyId = historyId;
        this.cookie = cookie;
    }

    public static LocalHistoryIdentifier readFrom(final DataInput in) throws IOException {
        final ClientIdentifier clientId = ClientIdentifier.readFrom(in);
        final long historyId = WritableObjects.readLong(in);
        return new LocalHistoryIdentifier(clientId, historyId, WritableObjects.readLong(in));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        clientId.writeTo(out);
        WritableObjects.writeLong(out, historyId);
        WritableObjects.writeLong(out, cookie);
    }

    public ClientIdentifier getClientId() {
        return clientId;
    }

    public long getHistoryId() {
        return historyId;
    }

    public long getCookie() {
        return cookie;
    }

    @Override
    public int hashCode() {
        int ret = clientId.hashCode();
        ret = 31 * ret + Long.hashCode(historyId);
        ret = 31 * ret + Long.hashCode(cookie);
        return ret;
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
        return historyId == other.historyId && cookie == other.cookie && clientId.equals(other.clientId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalHistoryIdentifier.class).add("client", clientId)
                .add("history", Long.toUnsignedString(historyId, 16))
                .add("cookie", Long.toUnsignedString(cookie, 16)).toString();
    }

    private Object writeReplace() {
        return new Proxy(clientId, historyId, cookie);
    }
}
