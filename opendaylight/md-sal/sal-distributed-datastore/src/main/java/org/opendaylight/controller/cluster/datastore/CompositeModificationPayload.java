/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.example.protobuff.messages.KeyValueMessages;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.messages.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CompositeModificationPayload extends Payload implements
    Serializable {

    private final PersistentMessages.CompositeModification modification;

    public CompositeModificationPayload(){
        modification = null;
    }
    public CompositeModificationPayload(Object modification){
        this.modification = (PersistentMessages.CompositeModification) modification;
    }

    @Override public Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> encode() {
        Preconditions.checkState(modification!=null);
        Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> map = new HashMap<>();
        map.put(org.opendaylight.controller.mdsal.CompositeModificationPayload.modification, this.modification);
        return map;
    }

    @Override public Payload decode(
        AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payload) {
        PersistentMessages.CompositeModification modification = payload
            .getExtension(
                org.opendaylight.controller.mdsal.CompositeModificationPayload.modification);
        payload.getExtension(KeyValueMessages.value);
        return new CompositeModificationPayload(modification);
    }

    public Object getModification(){
        return this.modification;
    }
}
