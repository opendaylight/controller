/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.protobuff.client.messages;

import com.google.common.base.Preconditions;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;

@Deprecated
public class CompositeModificationPayload extends Payload implements
    Serializable {

    private final PersistentMessages.CompositeModification modification;

    public CompositeModificationPayload(){
        modification = null;
    }
    public CompositeModificationPayload(Object modification){
        this.modification = (PersistentMessages.CompositeModification) Preconditions.checkNotNull(modification, "modification should not be null");
    }

    @Override public Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> encode() {
        Preconditions.checkState(modification!=null);
        Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> map = new HashMap<>();
        map.put(
            org.opendaylight.controller.protobuff.messages.shard.CompositeModificationPayload.modification, this.modification);
        return map;
    }

    @Override public Payload decode(
        AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payload) {
        PersistentMessages.CompositeModification modification = payload
            .getExtension(
                org.opendaylight.controller.protobuff.messages.shard.CompositeModificationPayload.modification);



        // The extension was put in the unknown field.
        // This is because extensions need to be registered
        // see org.opendaylight.controller.mdsal.CompositeModificationPayload.registerAllExtensions
        // also see https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/ExtensionRegistry
        // If that is not done then on the other end the extension shows up as an unknown field
        // Need to figure out a better way to do this
        if(payload.getUnknownFields().hasField(2)){
            UnknownFieldSet.Field field =
                payload.getUnknownFields().getField(2);

            try {
                modification =
                    PersistentMessages.CompositeModification
                        .parseFrom(field.getLengthDelimitedList().get(0));
            } catch (InvalidProtocolBufferException e) {

            }
        }

        return new CompositeModificationPayload(modification);
    }

    public Object getModification(){
        return this.modification;
    }

    @Override
    public int size(){
        return this.modification.getSerializedSize();
    }
}
