/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Decoded;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Encoded;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WriteData extends ModifyData implements VersionedSerializableMessage {
    private static final long serialVersionUID = 1L;

    public static final Class<WriteData> SERIALIZABLE_CLASS = WriteData.class;

    public WriteData() {
    }

    public WriteData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        super(path, data);
    }

    @Override
    public Object toSerializable(short toVersion) {
        if(toVersion >= DataStoreVersions.LITHIUM_VERSION) {
            setVersion(toVersion);
            return this;
        } else {
            // To base or R1 Helium version
            Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(getPath(), getData());
            return ShardTransactionMessages.WriteData.newBuilder()
                    .setInstanceIdentifierPathArguments(encoded.getEncodedPath())
                    .setNormalizedNode(encoded.getEncodedNode().getNormalizedNode()).build();
        }
    }

    public static WriteData fromSerializable(Object serializable) {
        if(serializable instanceof WriteData) {
            return (WriteData) serializable;
        } else {
            // From base or R1 Helium version
            ShardTransactionMessages.WriteData o = (ShardTransactionMessages.WriteData) serializable;
            Decoded decoded = new NormalizedNodeToNodeCodec(null).decode(
                    o.getInstanceIdentifierPathArguments(), o.getNormalizedNode());
            return new WriteData(decoded.getDecodedPath(), decoded.getDecodedNode());
        }
    }

    public static boolean isSerializedType(Object message) {
        return SERIALIZABLE_CLASS.isInstance(message) ||
               message instanceof ShardTransactionMessages.WriteData;
    }
}
