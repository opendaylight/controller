/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;

/**
 * Snapshot State implementation backed by a byte[].
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class ByteState implements Snapshot.State {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    public static final StateSnapshot.Support<ByteState> SUPPORT = new Support<>() {
        @Override
        public Class<ByteState> snapshotType() {
            return ByteState.class;
        }

        @Override
        public Reader<ByteState> reader() {
            return source -> {
                try (var in = source.openStream()) {
                    return ByteState.of(in.readAllBytes());
                }
            };
        }

        @Override
        public Writer<ByteState> writer() {
            return (snapshot, out) -> out.write(snapshot.bytes());
        }
    };

    private final byte[] bytes;

    private ByteState(final byte[] bytes) {
        this.bytes = requireNonNull(bytes);
    }

    public static ByteState of(final byte[] bytes) {
        return new ByteState(bytes);
    }

    public static ByteState empty() {
        return new ByteState(new byte[0]);
    }

    public byte[] bytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof ByteState other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("length", bytes.length).toString();
    }
}
