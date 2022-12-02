/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * Internal message that is stored in the akka's persistent journal to delete journal entries.
 *
 * @author Thomas Pantelis
 */
public sealed class DeleteEntries implements Serializable {
    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Legacy extends DeleteEntries implements LegacySerializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        Legacy(final long fromIndex) {
            super(fromIndex);
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private DeleteEntries deleteEntries = null;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(deleteEntries.fromIndex);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            deleteEntries = new Legacy(in.readLong());
        }

        @java.io.Serial
        private Object readResolve() {
            return deleteEntries;
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long fromIndex;

    public DeleteEntries(final long fromIndex) {
        this.fromIndex = fromIndex;
    }

    public final long getFromIndex() {
        return fromIndex;
    }

    @java.io.Serial
    public final Object writeReplace() {
        return new DE(this);
    }

    @Override
    public final String toString() {
        return "DeleteEntries [fromIndex=" + fromIndex + "]";
    }
}
