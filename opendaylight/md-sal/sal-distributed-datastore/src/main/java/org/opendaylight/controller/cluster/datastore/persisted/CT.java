/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload.Chunked;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload.Simple;
import org.opendaylight.raft.spi.ChunkedByteArray;

/**
 * Serialization proxy for {@link CommitTransactionPayload}.
 */
final class CT implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private CommitTransactionPayload payload;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public CT() {
        // For Externalizable
    }

    CT(final CommitTransactionPayload payload) {
        this.payload = requireNonNull(payload);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeInt(payload.size());
        payload.writeBytes(out);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final int length = in.readInt();
        if (length < 0) {
            throw new StreamCorruptedException("Invalid payload length " + length);
        } else if (length < CommitTransactionPayload.MAX_ARRAY_SIZE) {
            final byte[] serialized = new byte[length];
            in.readFully(serialized);
            payload = new Simple(serialized);
        } else {
            payload = new Chunked(ChunkedByteArray.readFrom(in, length, CommitTransactionPayload.MAX_ARRAY_SIZE));
        }
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(payload);
    }
}
