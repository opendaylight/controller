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
import org.eclipse.jdt.annotation.NonNull;

/**
 * Snapshot State implementation backed by a byte[].
 *
 * @author Thomas Pantelis
 */
public final class ByteState implements Snapshot.State {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final byte @NonNull[] bytes;

    private ByteState(final byte @NonNull[] bytes) {
        this.bytes = requireNonNull(bytes);
    }

    public static @NonNull ByteState of(final byte @NonNull[] bytes) {
        return new ByteState(bytes);
    }

    public static @NonNull ByteState empty() {
        return new ByteState(new byte[0]);
    }

    public byte @NonNull[] bytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof ByteState other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("length", bytes.length).toString();
    }
}
