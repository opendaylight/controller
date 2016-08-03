package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableRangeSet.Builder;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class FrontendClientMetadata implements Identifiable<ClientIdentifier>, WritableObject {
    private final Collection<FrontendHistoryMetadata> currentHistories;
    private final RangeSet<UnsignedLong> purgedHistories;
    private final ClientIdentifier identifier;

    public FrontendClientMetadata(final ClientIdentifier identifier, final RangeSet<UnsignedLong> purgedHistories,
            final Collection<FrontendHistoryMetadata> currentHistories) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.purgedHistories = ImmutableRangeSet.copyOf(purgedHistories);
        this.currentHistories = ImmutableList.copyOf(currentHistories);
    }

    public Collection<FrontendHistoryMetadata> getCurrentHistories() {
        return currentHistories;
    }

    public RangeSet<UnsignedLong> getPurgedHistories() {
        return purgedHistories;
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        identifier.writeTo(out);

        final Set<Range<UnsignedLong>> ranges = purgedHistories.asRanges();
        out.writeInt(ranges.size());
        for (Range<UnsignedLong> r : ranges) {
            WritableObjects.writeLongs(out, r.lowerEndpoint().longValue(), r.upperEndpoint().longValue());
        }

        out.writeInt(currentHistories.size());
        for (FrontendHistoryMetadata h : currentHistories) {
            h.writeTo(out);
        }
    }

    public static FrontendClientMetadata readFrom(final DataInput in) throws IOException, ClassNotFoundException {
        final ClientIdentifier id = ClientIdentifier.readFrom(in);

        final int purgedSize = in.readInt();
        final Builder<UnsignedLong> b = ImmutableRangeSet.builder();
        for (int i = 0; i < purgedSize; ++i) {
            final byte header = WritableObjects.readLongHeader(in);
            final UnsignedLong lower = UnsignedLong.fromLongBits(WritableObjects.readFirstLong(in, header));
            final UnsignedLong upper = UnsignedLong.fromLongBits(WritableObjects.readSecondLong(in, header));

            b.add(Range.closed(lower, upper));
        }

        final int currentSize = in.readInt();
        final Collection<FrontendHistoryMetadata> currentHistories = new ArrayList<>(currentSize);
        for (int i = 0; i < currentSize; ++i) {
            currentHistories.add(FrontendHistoryMetadata.readFrom(in));
        }

        return new FrontendClientMetadata(id, b.build(), currentHistories);
    }
}
