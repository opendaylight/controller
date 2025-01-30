/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.raft.spi.ImmutableUnsignedLongSet;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObject;

public record FrontendClientMetadata(
        @NonNull ClientIdentifier clientId,
        @NonNull ImmutableUnsignedLongSet purgedHistories,
        @NonNull ImmutableList<FrontendHistoryMetadata> currentHistories) implements Immutable, WritableObject {
    public FrontendClientMetadata {
        requireNonNull(clientId);
        requireNonNull(purgedHistories);
        requireNonNull(currentHistories);
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        clientId.writeTo(out);
        purgedHistories.writeTo(out);

        out.writeInt(currentHistories.size());
        for (final FrontendHistoryMetadata h : currentHistories) {
            h.writeTo(out);
        }
    }

    public static FrontendClientMetadata readFrom(final DataInput in) throws IOException {
        final var clientId = ClientIdentifier.readFrom(in);
        final var purgedHistories = ImmutableUnsignedLongSet.readFrom(in);

        final int currentSize = in.readInt();
        final var currentBuilder = ImmutableList.<FrontendHistoryMetadata>builderWithExpectedSize(currentSize);
        for (int i = 0; i < currentSize; ++i) {
            currentBuilder.add(FrontendHistoryMetadata.readFrom(in));
        }

        return new FrontendClientMetadata(clientId, purgedHistories, currentBuilder.build());
    }
}
