/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.SerializationUtils;

public class ApplyState implements Externalizable {
    private static final long serialVersionUID = 1L;

    private transient ActorRef clientActor;
    private transient String identifier;
    private transient ReplicatedLogEntry replicatedLogEntry;

    public ApplyState() {
    }

    public ApplyState(ActorRef clientActor, String identifier,
        ReplicatedLogEntry replicatedLogEntry) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.replicatedLogEntry = replicatedLogEntry;
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ReplicatedLogEntry getReplicatedLogEntry() {
        return replicatedLogEntry;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        clientActor = (ActorRef) in.readObject();

        if(in.readBoolean()) {
            identifier = in.readUTF();
        }

        replicatedLogEntry = SerializationUtils.deserializeReplicatedLogEntry(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(clientActor);

        if(identifier != null) {
            out.writeBoolean(true);
            out.writeUTF(identifier);
        }
        else {
            out.writeBoolean(false);
        }

        SerializationUtils.serializeReplicatedLogEntry(replicatedLogEntry, out);
    }
}
