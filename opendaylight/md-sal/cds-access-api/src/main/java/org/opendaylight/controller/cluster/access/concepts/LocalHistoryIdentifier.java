/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
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
 * Globally-unique identifier of a local history. This identifier is assigned on the frontend and is composed of
 * - a {@link ClientIdentifier}, which uniquely identifies a single instantiation of a particular frontend
 * - an unsigned long, which uniquely identifies the history on the backend
 * - an unsigned long cookie, assigned by the client and meaningless on the backend, which just reflects it back
 *
 * @author Robert Varga
 */
public final class LocalHistoryIdentifier implements WritableIdentifier {
    /*
     * Implementation note: cookie is currently required only for module-based sharding, which is implemented as part
     *                      of normal DataBroker interfaces. For DOMDataTreeProducer cookie will always be zero, hence
     *                      we may end up not needing cookie at all.
     *
     *                      We use WritableObjects.writeLongs() to output historyId and cookie (in that order). If we
     *                      end up not needing the cookie at all, we can switch to writeLong() and use zero flags for
     *                      compatibility.
     */
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;
        private ClientIdentifier clientId;
        private long historyId;
        private long cookie;

        // checkstyle flags the public modifier as redundant however it is explicitly needed for Java serialization to
        // be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ClientIdentifier frontendId, final long historyId, final long cookie) {
            clientId = requireNonNull(frontendId);
            this.historyId = historyId;
            this.cookie = cookie;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            clientId.writeTo(out);
            WritableObjects.writeLongs(out, historyId, cookie);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            clientId = ClientIdentifier.readFrom(in);

            final byte header = WritableObjects.readLongHeader(in);
            historyId = WritableObjects.readFirstLong(in, header);
            cookie = WritableObjects.readSecondLong(in, header);
        }

        private Object readResolve() {
            return new LocalHistoryIdentifier(clientId, historyId, cookie);
        }
    }

    private static final long serialVersionUID = 1L;
    private final @NonNull ClientIdentifier clientId;
    private final long historyId;
    private final long cookie;

    public LocalHistoryIdentifier(final ClientIdentifier frontendId, final long historyId) {
        this(frontendId, historyId, 0);
    }

    public LocalHistoryIdentifier(final ClientIdentifier frontendId, final long historyId, final long cookie) {
        clientId = requireNonNull(frontendId);
        this.historyId = historyId;
        this.cookie = cookie;
    }

    public static @NonNull LocalHistoryIdentifier readFrom(final DataInput in) throws IOException {
        final ClientIdentifier clientId = ClientIdentifier.readFrom(in);

        final byte header = WritableObjects.readLongHeader(in);
        return new LocalHistoryIdentifier(clientId, WritableObjects.readFirstLong(in, header),
            WritableObjects.readSecondLong(in, header));
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        clientId.writeTo(out);
        WritableObjects.writeLongs(out, historyId, cookie);
    }

    public @NonNull ClientIdentifier getClientId() {
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
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalHistoryIdentifier)) {
            return false;
        }

        final LocalHistoryIdentifier other = (LocalHistoryIdentifier) obj;
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
