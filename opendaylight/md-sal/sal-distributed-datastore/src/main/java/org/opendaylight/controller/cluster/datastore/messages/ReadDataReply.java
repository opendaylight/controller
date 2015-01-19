/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadDataReply implements VersionedSerializableMessage {
    public static final Class<ExternalizableReadDataReply> SERIALIZABLE_CLASS =
            ExternalizableReadDataReply.class;

    private final NormalizedNode<?, ?> normalizedNode;

    public ReadDataReply(NormalizedNode<?, ?> normalizedNode){

        this.normalizedNode = normalizedNode;
    }

    public NormalizedNode<?, ?> getNormalizedNode() {
        return normalizedNode;
    }

    @Override
    public Object toSerializable(short toVersion) {
        if(toVersion >= DataStoreVersions.LITHIUM_VERSION) {
            return new ExternalizableReadDataReply(normalizedNode, toVersion);
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
        if(serializable instanceof ExternalizableReadDataReply) {
            ExternalizableReadDataReply ext = (ExternalizableReadDataReply)serializable;
            return new ReadDataReply(ext.getNormalizedNode());
        } else {
            ShardTransactionMessages.ReadDataReply o =
                    (ShardTransactionMessages.ReadDataReply) serializable;
            return new ReadDataReply(new NormalizedNodeToNodeCodec(null).decode(
                    o.getNormalizedNode()));
        }
    }

    public static ByteString fromSerializableAsByteString(Object serializable) {
        if(serializable instanceof ExternalizableReadDataReply) {
            ExternalizableReadDataReply ext = (ExternalizableReadDataReply)serializable;
            return toSerializableReadDataReply(ext.getNormalizedNode()).toByteString();
        } else {
            ShardTransactionMessages.ReadDataReply o =
                    (ShardTransactionMessages.ReadDataReply) serializable;
            return o.getNormalizedNode().toByteString();
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof ExternalizableReadDataReply ||
               message instanceof ShardTransactionMessages.ReadDataReply;
    }
}
