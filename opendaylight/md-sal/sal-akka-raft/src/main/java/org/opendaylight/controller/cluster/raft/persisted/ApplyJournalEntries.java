/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import akka.dispatch.ControlMessage;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * This is an internal message that is stored in the akka's persistent journal. During recovery, this
 * message is used to apply recovered journal entries to the state whose indexes range from the context's
 * current lastApplied index to "toIndex" contained in the message. This message is sent internally from a
 * behavior to the RaftActor to persist.
 *
 * @author Thomas Pantelis
 */
public sealed class ApplyJournalEntries implements Serializable, ControlMessage {
    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Legacy extends ApplyJournalEntries implements LegacySerializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        Legacy(final long toIndex) {
            super(toIndex);
        }
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private ApplyJournalEntries applyEntries = null;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(applyEntries.toIndex);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException {
            applyEntries = new Legacy(in.readLong());
        }

        @java.io.Serial
        private Object readResolve() {
            return applyEntries;
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final long toIndex;

    public ApplyJournalEntries(final long toIndex) {
        this.toIndex = toIndex;
    }

    public final long getToIndex() {
        return toIndex;
    }

    @java.io.Serial
    public final Object writeReplace() {
        return new AJE(this);
    }

    @Override
    public final String toString() {
        return "ApplyJournalEntries [toIndex=" + toIndex + "]";
    }
}
