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
import com.google.common.collect.ImmutableList;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ImmutableUnsignedLongSet;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.WritableObject;

public final class FrontendClientMetadata implements Identifiable<ClientIdentifier>, WritableObject {
    private final @NonNull ImmutableList<FrontendHistoryMetadata> currentHistories;
    private final @NonNull ImmutableUnsignedLongSet purgedHistories;
    private final @NonNull ClientIdentifier identifier;

    public FrontendClientMetadata(final ClientIdentifier identifier, final ImmutableUnsignedLongSet purgedHistories,
            final Collection<FrontendHistoryMetadata> currentHistories) {
        this.identifier = requireNonNull(identifier);
        this.purgedHistories = requireNonNull(purgedHistories);
        this.currentHistories = ImmutableList.copyOf(currentHistories);
    }

    public Collection<FrontendHistoryMetadata> getCurrentHistories() {
        return currentHistories;
    }

    public ImmutableUnsignedLongSet getPurgedHistories() {
        return purgedHistories;
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        identifier.writeTo(out);
        purgedHistories.writeTo(out);

        out.writeInt(currentHistories.size());
        for (final FrontendHistoryMetadata h : currentHistories) {
            h.writeTo(out);
        }
    }

    public static FrontendClientMetadata readFrom(final DataInput in) throws IOException {
        final ClientIdentifier id = ClientIdentifier.readFrom(in);
        final var purgedHistories = ImmutableUnsignedLongSet.readFrom(in);

        final int currentSize = in.readInt();
        // FIXME: ImmutableList.builder()
        final var currentHistories = new ArrayList<FrontendHistoryMetadata>(currentSize);
        for (int i = 0; i < currentSize; ++i) {
            currentHistories.add(FrontendHistoryMetadata.readFrom(in));
        }

        return new FrontendClientMetadata(id, purgedHistories, currentHistories);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendClientMetadata.class).add("identifer", identifier)
                .add("current", currentHistories).add("purged", purgedHistories).toString();
    }
}
