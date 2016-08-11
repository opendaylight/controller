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
 * This is an internal message that is stored in the akka's persistent journal. During recovery, this
 * message is used to apply recovered journal entries to the state whose indexes range from the context's
 * current lastApplied index to "toIndex" contained in the message. This message is sent internally from a
 * behavior to the RaftActor to persist.
 *
 * @author Thomas Pantelis
 */
public class ApplyJournalEntries implements Serializable, MigratedSerializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private ApplyJournalEntries applyEntries;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final ApplyJournalEntries applyEntries) {
            this.applyEntries = applyEntries;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(applyEntries.toIndex);
         }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            applyEntries = new ApplyJournalEntries(in.readLong());
        }

        private Object readResolve() {
            return applyEntries;
        }
    }

    private static final long serialVersionUID = 1L;

    private final long toIndex;
    private final boolean migrated;

    private ApplyJournalEntries(final long toIndex, final boolean migrated) {
        this.toIndex = toIndex;
        this.migrated = migrated;
    }

    public ApplyJournalEntries(final long toIndex) {
        this(toIndex, false);
    }

    public long getToIndex() {
        return toIndex;
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    @Override
    public Object writeReplace() {
        return new Proxy(this);
    }

    @Deprecated
    public static ApplyJournalEntries createMigrated(final long fromIndex) {
        return new ApplyJournalEntries(fromIndex, true);
    }

    @Override
    public String toString() {
        return "ApplyJournalEntries [toIndex=" + toIndex + "]";
    }
}
