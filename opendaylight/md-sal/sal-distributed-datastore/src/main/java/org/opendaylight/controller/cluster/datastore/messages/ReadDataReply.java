/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.protobuf.ByteString;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadDataReply implements VersionedSerializableMessage, Externalizable {
    private static final long serialVersionUID = 1L;

    public static final Class<ReadDataReply> SERIALIZABLE_CLASS = ReadDataReply.class;

    private NormalizedNode<?, ?> normalizedNode;
    private short version;

    public ReadDataReply() {
    }

    public ReadDataReply(NormalizedNode<?, ?> normalizedNode) {
        this.normalizedNode = normalizedNode;
    }

    public NormalizedNode<?, ?> getNormalizedNode() {
        return normalizedNode;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        version = in.readShort();
        normalizedNode = SerializationUtils.deserializeNormalizedNode(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(version);
        SerializationUtils.serializeNormalizedNode(normalizedNode, out);
    }

    @Override
    public Object toSerializable(short toVersion) {
        if(toVersion >= DataStoreVersions.LITHIUM_VERSION) {
            version = toVersion;
            return this;
        } else {
            return toSerializableReadDataReply(normalizedNode);
        }
    }

    private static ShardTransactionMessages.ReadDataReply toSerializableReadDataReply(
            NormalizedNode<?, ?> normalizedNode) {
        if(normalizedNode != null) {
            return ShardTransactionMessages.ReadDataReply.newBuilder()
                    .setNormalizedNode(new NormalizedNodeToNodeCodec(null)
                    .encode(normalizedNode).getNormalizedNode()).build();
        } else {
            return ShardTransactionMessages.ReadDataReply.newBuilder().build();

        }
    }

    public static ReadDataReply fromSerializable(Object serializable) {
        if(serializable instanceof ReadDataReply) {
            return (ReadDataReply) serializable;
        } else {
            ShardTransactionMessages.ReadDataReply o =
                    (ShardTransactionMessages.ReadDataReply) serializable;
            return new ReadDataReply(new NormalizedNodeToNodeCodec(null).decode(o.getNormalizedNode()));
        }
    }

    public static ByteString fromSerializableAsByteString(Object serializable) {
        if(serializable instanceof ReadDataReply) {
            ReadDataReply r = (ReadDataReply)serializable;
            return toSerializableReadDataReply(r.getNormalizedNode()).toByteString();
        } else {
            ShardTransactionMessages.ReadDataReply o =
                    (ShardTransactionMessages.ReadDataReply) serializable;
            return o.getNormalizedNode().toByteString();
        }
    }

    public static boolean isSerializedType(Object message) {
        return SERIALIZABLE_CLASS.isAssignableFrom(message.getClass()) ||
               message instanceof ShardTransactionMessages.ReadDataReply;
    }
}
