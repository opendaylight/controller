/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Decoded;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WriteData extends ModifyData {

    public static final Class<ShardTransactionMessages.WriteData> SERIALIZABLE_CLASS =
            ShardTransactionMessages.WriteData.class;

    public WriteData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        super(path, data);
    }

    @Override
    public Object toSerializable() {
        return new ExternalizableWriteData(getPath(), getData());

//        Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(path, data);
//        return ShardTransactionMessages.WriteData.newBuilder()
//                .setInstanceIdentifierPathArguments(encoded.getEncodedPath())
//                .setNormalizedNode(encoded.getEncodedNode().getNormalizedNode()).build();
    }

    public static WriteData fromSerializable(Object serializable) {
        if(serializable instanceof ExternalizableWriteData) {
            ExternalizableWriteData ext = (ExternalizableWriteData)serializable;
            return new WriteData(ext.getPath(), ext.getNode());
        } else {
            // From base Helium version
            ShardTransactionMessages.WriteData o = (ShardTransactionMessages.WriteData) serializable;
            Decoded decoded = new NormalizedNodeToNodeCodec(null).decode(
                    o.getInstanceIdentifierPathArguments(), o.getNormalizedNode());
            return new WriteData(decoded.getDecodedPath(), decoded.getDecodedNode());
        }
    }

    public static boolean isSerializedType(Object message) {
        return message instanceof ExternalizableWriteData ||
               message instanceof ShardTransactionMessages.WriteData;
    }
}
