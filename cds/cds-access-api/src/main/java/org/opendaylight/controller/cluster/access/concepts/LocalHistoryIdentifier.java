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
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Globally-unique identifier of a local history. This identifier is assigned on the frontend and is composed of
 * - a {@link ClientIdentifier}, which uniquely identifies a single instantiation of a particular frontend
 * - an unsigned long, which uniquely identifies the history on the backend
 * - an unsigned long cookie, assigned by the client and meaningless on the backend, which just reflects it back
 */
public final class LocalHistoryIdentifier implements WritableIdentifier {
    @java.io.Serial
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
        if (!(obj instanceof LocalHistoryIdentifier other)) {
            return false;
        }

        return historyId == other.historyId && cookie == other.cookie && clientId.equals(other.clientId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LocalHistoryIdentifier.class).add("client", clientId)
                .add("history", Long.toUnsignedString(historyId, 16))
                .add("cookie", Long.toUnsignedString(cookie, 16)).toString();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new HI(this);
    }
}
