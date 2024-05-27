/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Snapshot State implementation backed by a byte[].
 *
 * @author Thomas Pantelis
 */
public final class ByteState implements Snapshot.State {
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

    public byte @NonNull[] getBytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ByteState other = (ByteState) obj;
        if (!Arrays.equals(bytes, other.bytes)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ByteState [bytes=" + Arrays.toString(bytes) + "]";
    }
}
