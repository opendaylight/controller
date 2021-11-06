/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet;
import org.opendaylight.controller.cluster.datastore.utils.UnsignedLongBitmap;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class FrontendHistoryMetadata implements WritableObject {
    private final @NonNull ImmutableUnsignedLongSet purgedTransactions;
    private final @NonNull UnsignedLongBitmap closedTransactions;
    private final long historyId;
    private final long cookie;
    private final boolean closed;

    public FrontendHistoryMetadata(final long historyId, final long cookie, final boolean closed,
            final UnsignedLongBitmap closedTransactions, final ImmutableUnsignedLongSet purgedTransactions) {
        this.historyId = historyId;
        this.cookie = cookie;
        this.closed = closed;
        this.closedTransactions = requireNonNull(closedTransactions);
        this.purgedTransactions = requireNonNull(purgedTransactions);
    }

    public long getHistoryId() {
        return historyId;
    }

    public long getCookie() {
        return cookie;
    }

    public boolean isClosed() {
        return closed;
    }

    public UnsignedLongBitmap getClosedTransactions() {
        return closedTransactions;
    }

    public ImmutableUnsignedLongSet getPurgedTransactions() {
        return purgedTransactions;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        WritableObjects.writeLongs(out, historyId, cookie);
        out.writeBoolean(closed);

        final int closedSize = closedTransactions.size();
        final int purgedSize = purgedTransactions.size();
        WritableObjects.writeLongs(out, closedSize, purgedSize);
        closedTransactions.writeEntriesTo(out, closedSize);
        purgedTransactions.writeRangesTo(out, purgedSize);
    }

    public static FrontendHistoryMetadata readFrom(final DataInput in) throws IOException {
        final byte firstHdr = WritableObjects.readLongHeader(in);
        final long historyId = WritableObjects.readFirstLong(in, firstHdr);
        final long cookie = WritableObjects.readSecondLong(in, firstHdr);
        final boolean closed = in.readBoolean();

        final byte secondHdr = WritableObjects.readLongHeader(in);
        final int csize = verifySize(WritableObjects.readFirstLong(in, secondHdr));
        final int psize = verifySize(WritableObjects.readSecondLong(in, secondHdr));

        return new FrontendHistoryMetadata(historyId, cookie, closed,
            UnsignedLongBitmap.readFrom(in, csize),
            ImmutableUnsignedLongSet.readFrom(in, psize));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendHistoryMetadata.class)
            .add("historyId", historyId)
            .add("cookie", cookie)
            .add("closed", closed)
            .add("closedTransactions", closedTransactions)
            .add("purgedTransactions", purgedTransactions)
            .toString();
    }

    private static int verifySize(final long size) throws IOException {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IOException("Invalid size " + size);
        }
        return (int) size;
    }
}
