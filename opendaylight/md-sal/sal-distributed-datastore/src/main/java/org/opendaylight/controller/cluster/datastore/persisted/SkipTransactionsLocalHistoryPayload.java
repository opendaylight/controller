/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedLong;
import java.io.DataInput;
import java.io.IOException;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a local history is instructed some transaction identifiers, i.e. the frontend has used them
 * for other purposes. It contains a {@link LocalHistoryIdentifier} and a list of transaction identifiers within that
 * local history.
 */
@Beta
public final class SkipTransactionsLocalHistoryPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
    private static final class Proxy extends AbstractProxy<LocalHistoryIdentifier> {
        private static final long serialVersionUID = 1L;

        private List<UnsignedLong> transactionIds;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final byte[] serialized) {
            super(serialized);
        }

        @Override
        protected LocalHistoryIdentifier readIdentifier(final DataInput in) throws IOException {
            final var id = LocalHistoryIdentifier.readFrom(in);

            final int size = in.readInt();
            final var builder = ImmutableList.<UnsignedLong>builderWithExpectedSize(size);

            int idx;
            if (size % 2 != 0) {
                builder.add(UnsignedLong.fromLongBits(WritableObjects.readLong(in)));
                idx = 1;
            } else {
                idx = 0;
            }

            for (; idx < size; idx += 2) {
                final byte hdr = WritableObjects.readLongHeader(in);
                builder.add(UnsignedLong.fromLongBits(WritableObjects.readFirstLong(in, hdr)));
                builder.add(UnsignedLong.fromLongBits(WritableObjects.readSecondLong(in, hdr)));
            }

            transactionIds = builder.build();
            return id;
        }

        @Override
        protected SkipTransactionsLocalHistoryPayload createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new SkipTransactionsLocalHistoryPayload(identifier, serialized, verifyNotNull(transactionIds));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SkipTransactionsLocalHistoryPayload.class);
    private static final long serialVersionUID = 1L;

    private final @NonNull List<UnsignedLong> transactionIds;

    private SkipTransactionsLocalHistoryPayload(final LocalHistoryIdentifier historyId, final byte[] serialized,
            final List<UnsignedLong> transactionIds) {
        super(historyId, serialized);
        this.transactionIds = requireNonNull(transactionIds);
    }

    public static @NonNull SkipTransactionsLocalHistoryPayload create(final LocalHistoryIdentifier historyId,
            final List<UnsignedLong> transactionIds, final int initialSerializedBufferCapacity) {
        final var out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            historyId.writeTo(out);

            final int size = transactionIds.size();
            out.writeInt(size);

            int idx;
            if (size % 2 != 0) {
                WritableObjects.writeLong(out, transactionIds.get(0).longValue());
                idx = 1;
            } else {
                idx = 0;
            }

            for (; idx < size; idx += 2) {
                WritableObjects.writeLongs(out, transactionIds.get(idx).longValue(),
                    transactionIds.get(idx + 1).longValue());
            }
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {} ids {}", historyId, transactionIds, e);
            throw new RuntimeException("Failed to serialize " + historyId + " ids " + transactionIds, e);
        }

        return new SkipTransactionsLocalHistoryPayload(historyId, out.toByteArray(), transactionIds);
    }

    public @NonNull List<UnsignedLong> getTransactionIds() {
        return transactionIds;
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }
}
