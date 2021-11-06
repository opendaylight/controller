/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class FrontendHistoryMetadata implements WritableObject {
    private final RangeSet<UnsignedLong> purgedTransactions;
    private final Map<UnsignedLong, Boolean> closedTransactions;
    private final long historyId;
    private final long cookie;
    private final boolean closed;

    public FrontendHistoryMetadata(final long historyId, final long cookie, final boolean closed,
            final Map<UnsignedLong, Boolean> closedTransactions, final RangeSet<UnsignedLong> purgedTransactions) {
        this.historyId = historyId;
        this.cookie = cookie;
        this.closed = closed;
        this.closedTransactions = ImmutableMap.copyOf(closedTransactions);
        this.purgedTransactions = ImmutableRangeSet.copyOf(purgedTransactions);
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

    public Map<UnsignedLong, Boolean> getClosedTransactions() {
        return closedTransactions;
    }

    public RangeSet<UnsignedLong> getPurgedTransactions() {
        return purgedTransactions;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        WritableObjects.writeLongs(out, historyId, cookie);
        out.writeBoolean(closed);

        final Set<Range<UnsignedLong>> purgedRanges = purgedTransactions.asRanges();
        WritableObjects.writeLongs(out, closedTransactions.size(), purgedRanges.size());
        for (Entry<UnsignedLong, Boolean> e : closedTransactions.entrySet()) {
            WritableObjects.writeLong(out, e.getKey().longValue());
            out.writeBoolean(e.getValue().booleanValue());
        }
        for (Range<UnsignedLong> r : purgedRanges) {
            WritableObjects.writeLongs(out, r.lowerEndpoint().longValue(), r.upperEndpoint().longValue());
        }
    }

    public static FrontendHistoryMetadata readFrom(final DataInput in) throws IOException {
        byte header = WritableObjects.readLongHeader(in);
        final long historyId = WritableObjects.readFirstLong(in, header);
        final long cookie = WritableObjects.readSecondLong(in, header);
        final boolean closed = in.readBoolean();

        header = WritableObjects.readLongHeader(in);
        long ls = WritableObjects.readFirstLong(in, header);
        Verify.verify(ls >= 0 && ls <= Integer.MAX_VALUE);
        final int csize = (int) ls;

        ls = WritableObjects.readSecondLong(in, header);
        Verify.verify(ls >= 0 && ls <= Integer.MAX_VALUE);
        final int psize = (int) ls;

        final Map<UnsignedLong, Boolean> closedTransactions = new HashMap<>(csize);
        for (int i = 0; i < csize; ++i) {
            final UnsignedLong key = UnsignedLong.fromLongBits(WritableObjects.readLong(in));
            final Boolean value = Boolean.valueOf(in.readBoolean());
            closedTransactions.put(key, value);
        }
        final RangeSet<UnsignedLong> purgedTransactions = TreeRangeSet.create();
        for (int i = 0; i < psize; ++i) {
            final byte h = WritableObjects.readLongHeader(in);
            final UnsignedLong l = UnsignedLong.fromLongBits(WritableObjects.readFirstLong(in, h));
            final UnsignedLong u = UnsignedLong.fromLongBits(WritableObjects.readSecondLong(in, h));
            purgedTransactions.add(Range.closedOpen(l, u));
        }

        return new FrontendHistoryMetadata(historyId, cookie, closed, closedTransactions, purgedTransactions);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendHistoryMetadata.class).add("historyId", historyId)
                .add("cookie", cookie).add("closed", closed).add("closedTransactions", closedTransactions)
                .add("purgedTransactions", purgedTransactions).toString();
    }
}
