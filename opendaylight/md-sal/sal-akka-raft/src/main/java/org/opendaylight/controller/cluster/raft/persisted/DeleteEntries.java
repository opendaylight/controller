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
public class DeleteEntries implements Serializable, MigratedSerializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private DeleteEntries deleteEntries;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final DeleteEntries deleteEntries) {
            this.deleteEntries = deleteEntries;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(deleteEntries.fromIndex);
         }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            deleteEntries = new DeleteEntries(in.readLong());
        }

        private Object readResolve() {
            return deleteEntries;
        }
    }

    private static final long serialVersionUID = 1L;

    private final long fromIndex;
    private final boolean migrated;

    private DeleteEntries(final long fromIndex, final boolean migrated) {
        this.fromIndex = fromIndex;
        this.migrated = migrated;
    }

    public DeleteEntries(final long fromIndex) {
        this(fromIndex, false);
    }

    public long getFromIndex() {
        return fromIndex;
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
    public static DeleteEntries createMigrated(final long fromIndex) {
        return new DeleteEntries(fromIndex, true);
    }

    @Override
    public String toString() {
        return "DeleteEntries [fromIndex=" + fromIndex + "]";
    }
}
