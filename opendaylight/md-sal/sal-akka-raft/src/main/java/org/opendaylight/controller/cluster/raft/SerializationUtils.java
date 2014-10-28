/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import com.google.common.collect.Lists;

public class SerializationUtils {

    public static Object fromSerializable(Object serializable){
        if(serializable instanceof AppendEntries){
            return serializable;

        } else if (serializable.getClass().equals(InstallSnapshot.SERIALIZABLE_CLASS)) {
            return InstallSnapshot.fromSerializable(serializable);
        }
        return serializable;
    }

    public static void serializeReplicatedLogEntry(ReplicatedLogEntry entry, ObjectOutput out)
            throws IOException {
        entry.writeExternal(out);
    }

    public static void serializeReplicatedLogEnties(Collection<ReplicatedLogEntry> entries,
            ObjectOutput out) throws IOException {
        out.writeInt(entries.size());
        for (ReplicatedLogEntry entry : entries) {
            serializeReplicatedLogEntry(entry, out);
        }
    }

    public static ReplicatedLogEntry deserializeReplicatedLogEntry(ObjectInput in)
            throws IOException, ClassNotFoundException {
        ReplicatedLogImplEntry entry = new ReplicatedLogImplEntry();
        entry.readExternal(in);
        return entry;
    }

    public static List<ReplicatedLogEntry> deserializeReplicatedLogEntries(ObjectInput in)
            throws IOException, ClassNotFoundException {
        int size = in.readInt();
        List<ReplicatedLogEntry> entries = Lists.newArrayListWithCapacity(size);
        for(int i = 0; i < size; i++) {
            entries.add(deserializeReplicatedLogEntry(in));
        }

        return entries;
    }
}
