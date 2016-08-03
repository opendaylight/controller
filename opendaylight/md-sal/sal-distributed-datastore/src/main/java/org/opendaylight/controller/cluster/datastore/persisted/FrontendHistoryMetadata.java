/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.MoreObjects;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class FrontendHistoryMetadata implements WritableObject {
    private final long historyId;
    private final long cookie;
    private final long nextTransaction;
    private final boolean closed;

    public FrontendHistoryMetadata(final long historyId, final long cookie, final long nextTransaction,
            final boolean closed) {
        this.historyId = historyId;
        this.cookie = cookie;
        this.nextTransaction = nextTransaction;
        this.closed = closed;
    }

    public long getHistoryId() {
        return historyId;
    }

    public long getCookie() {
        return cookie;
    }

    public long getNextTransaction() {
        return nextTransaction;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        WritableObjects.writeLongs(out, historyId, cookie);
        WritableObjects.writeLong(out, nextTransaction);
        out.writeBoolean(closed);
    }

    public static FrontendHistoryMetadata readFrom(final DataInput in) throws IOException {
        final byte header = WritableObjects.readLongHeader(in);
        final long historyId = WritableObjects.readFirstLong(in, header);
        final long cookie = WritableObjects.readSecondLong(in, header);
        final long nextTransaction = WritableObjects.readLong(in);
        final boolean closed = in.readBoolean();

        return new FrontendHistoryMetadata(historyId, cookie, nextTransaction, closed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyId, cookie, nextTransaction, closed);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FrontendHistoryMetadata)) {
            return false;
        }

        final FrontendHistoryMetadata other = (FrontendHistoryMetadata) o;
        return historyId == other.historyId && cookie == other.cookie && nextTransaction == other.nextTransaction
                && closed == other.closed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendHistoryMetadata.class).add("historiId", historyId)
                .add("cookie", cookie).add("nextTransaction", nextTransaction).add("closed", closed).toString();
    }
}
