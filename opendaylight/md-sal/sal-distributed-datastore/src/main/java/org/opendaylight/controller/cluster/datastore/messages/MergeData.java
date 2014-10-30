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
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils.Creator;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import com.google.protobuf.ByteString;

public class MergeData extends ModifyData {

    public static final Class<ShardTransactionMessages.MergeData> SERIALIZABLE_CLASS =
            ShardTransactionMessages.MergeData.class;

    private static final Creator<MergeData> CREATOR = new Creator<MergeData>() {
        @Override
        public MergeData newInstance(YangInstanceIdentifier path, NormalizedNode<?, ?> node) {
            return new MergeData(path, node);
        }
    };

    public MergeData(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        super(path, data);
    }

    @Override
    public Object toSerializable() {
        return ShardTransactionMessages.MergeData.newBuilder().setSerializedNodeAndPath(
                SerializationUtils.pathAndNodeToByteString(getPath(), getData())).build();

//        Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(getPath(), getData());
//        return ShardTransactionMessages.MergeData.newBuilder()
//            .setInstanceIdentifierPathArguments(encoded.getEncodedPath())
//            .setNormalizedNode(encoded.getEncodedNode().getNormalizedNode()).build();
    }

    public static MergeData fromSerializable(Object serializable){
        ShardTransactionMessages.MergeData o = (ShardTransactionMessages.MergeData) serializable;
        ByteString serialized = o.getSerializedNodeAndPath();
        if(serialized != null && !serialized.isEmpty()) {
            return SerializationUtils.pathAndNodeFromByteString(serialized, CREATOR);
        } else {
            // From older version
            Decoded decoded = new NormalizedNodeToNodeCodec(null).decode(
                    o.getInstanceIdentifierPathArguments(), o.getNormalizedNode());
            return new MergeData(decoded.getDecodedPath(), decoded.getDecodedNode());
        }
    }
}
