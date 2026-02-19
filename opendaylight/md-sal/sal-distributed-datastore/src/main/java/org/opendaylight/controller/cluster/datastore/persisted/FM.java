/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/**
 * Externalizable proxy for {@link FrontendShardDataTreeSnapshotMetadata}.
 */
final class FM implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private List<FrontendClientMetadata> clients;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public FM() {
        // For Externalizable
    }

    FM(final FrontendShardDataTreeSnapshotMetadata metadata) {
        clients = metadata.getClients();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(clients.size());
        for (var c : clients) {
            c.writeTo(out);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final int size = in.readInt();
        final var builder = ImmutableList.<FrontendClientMetadata>builderWithExpectedSize(size);
        for (int i = 0; i < size ; ++i) {
            builder.add(FrontendClientMetadata.readFrom(in));
        }
        clients = builder.build();
    }

    @java.io.Serial
    private Object readResolve() {
        return new FrontendShardDataTreeSnapshotMetadata(clients);
    }
}
