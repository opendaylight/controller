/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.protobuff.client.messages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class CompositeModificationByteStringPayload extends Payload implements
        Serializable {
    private static final long serialVersionUID = 1L;

    private ByteString byteString;
    private SoftReference<PersistentMessages.CompositeModification> modificationReference;
    private static final Logger LOG = LoggerFactory.getLogger(CompositeModificationByteStringPayload.class);

    public CompositeModificationByteStringPayload(){
        byteString = null;
    }
    public CompositeModificationByteStringPayload(Object modification){
        this(((PersistentMessages.CompositeModification) modification).toByteString());
        this.modificationReference = new SoftReference<>((PersistentMessages.CompositeModification) modification);
    }

    private CompositeModificationByteStringPayload(ByteString byteString){
        this.byteString = Preconditions.checkNotNull(byteString, "byteString should not be null");
    }


    @Override
    public Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> encode() {
        Preconditions.checkState(byteString!=null);
        Map<GeneratedMessage.GeneratedExtension, PersistentMessages.CompositeModification> map = new HashMap<>();
        map.put(org.opendaylight.controller.protobuff.messages.shard.CompositeModificationPayload.modification,
                getModificationInternal());
        return map;
    }

    @Override
    public Payload decode(
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

            return new CompositeModificationByteStringPayload(field.getLengthDelimitedList().get(0));
        }

        return new CompositeModificationByteStringPayload(modification);
    }

    public Object getModification(){
        return getModificationInternal();
    }

    private PersistentMessages.CompositeModification getModificationInternal(){
        if(this.modificationReference != null && this.modificationReference.get() != null){
            return this.modificationReference.get();
        }
        try {
            PersistentMessages.CompositeModification compositeModification = PersistentMessages.CompositeModification.parseFrom(this.byteString);
            this.modificationReference = new SoftReference<>(compositeModification);
            return compositeModification;
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Unexpected exception occurred when parsing byteString to CompositeModification", e);
        }

        return null;
    }

    @Override
    public int size(){
        return byteString.size();
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {
        byteString.writeTo(stream);
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        byteString = ByteString.readFrom(stream);
    }

    @VisibleForTesting
    public void clearModificationReference(){
        if(this.modificationReference != null) {
            this.modificationReference.clear();
        }
    }
}