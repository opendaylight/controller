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

import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload persisted when a local history is instructed some transaction identifiers, i.e. the frontend has used them
 * for other purposes. It contains a {@link LocalHistoryIdentifier} and a list of transaction identifiers within that
 * local history.
 */
public final class SkipTransactionsPayload extends AbstractIdentifiablePayload<LocalHistoryIdentifier> {
    private static final class Proxy extends AbstractProxy<LocalHistoryIdentifier> {
        private static final long serialVersionUID = 1L;

        private ImmutableUnsignedLongSet transactionIds;

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
            transactionIds = ImmutableUnsignedLongSet.readFrom(in);
            return id;
        }

        @Override
        protected SkipTransactionsPayload createObject(final LocalHistoryIdentifier identifier,
                final byte[] serialized) {
            return new SkipTransactionsPayload(identifier, serialized, verifyNotNull(transactionIds));
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SkipTransactionsPayload.class);
    private static final long serialVersionUID = 1L;
    private static final int PROXY_SIZE = externalizableProxySize(Proxy::new);

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via externalizable proxy")
    private final @NonNull ImmutableUnsignedLongSet transactionIds;

    private SkipTransactionsPayload(final @NonNull LocalHistoryIdentifier historyId,
            final byte @NonNull [] serialized, final ImmutableUnsignedLongSet transactionIds) {
        super(historyId, serialized);
        this.transactionIds = requireNonNull(transactionIds);
    }

    public static @NonNull SkipTransactionsPayload create(final LocalHistoryIdentifier historyId,
            final ImmutableUnsignedLongSet transactionIds, final int initialSerializedBufferCapacity) {
        final var out = ByteStreams.newDataOutput(initialSerializedBufferCapacity);
        try {
            historyId.writeTo(out);
            transactionIds.writeTo(out);
        } catch (IOException e) {
            // This should never happen
            LOG.error("Failed to serialize {} ids {}", historyId, transactionIds, e);
            throw new IllegalStateException("Failed to serialize " + historyId + " ids " + transactionIds, e);
        }

        return new SkipTransactionsPayload(historyId, out.toByteArray(), transactionIds);
    }

    public @NonNull ImmutableUnsignedLongSet getTransactionIds() {
        return transactionIds;
    }

    @Override
    protected Proxy externalizableProxy(final byte[] serialized) {
        return new Proxy(serialized);
    }

    @Override
    protected int externalizableProxySize() {
        return PROXY_SIZE;
    }
}
