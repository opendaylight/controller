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
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
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