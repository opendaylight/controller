/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages.AppendEntries.ReplicatedLogEntry;

/**
 * Payload implementation for MutableCompositeModification used for persistence and replication.
 *
 * @author Thomas Pantelis
 */
public class ModificationPayload extends Payload implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient byte[] serializedPayload;

    public ModificationPayload() {
    }

    public ModificationPayload(Modification from) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(from);
        out.close();
        serializedPayload = bos.toByteArray();
    }

    public Modification getModification() throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedPayload));
        Modification to = (Modification) in.readObject();
        in.close();
        return to;
    }

    @Override
    public int size() {
        return serializedPayload.length;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        serializedPayload = new byte[size];
        in.readFully(serializedPayload);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(serializedPayload.length);
        out.write(serializedPayload);
    }

    @SuppressWarnings("rawtypes")
    @Override
    @Deprecated
    public <T> Map<GeneratedExtension, T> encode() {
        return null;
    }

    @Override
    @Deprecated
    public Payload decode(ReplicatedLogEntry.Payload payload) {
        return null;
    }
}
