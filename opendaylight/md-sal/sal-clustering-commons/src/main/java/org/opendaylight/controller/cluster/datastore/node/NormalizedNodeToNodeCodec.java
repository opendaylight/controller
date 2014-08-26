/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node;

import org.opendaylight.controller.cluster.datastore.node.utils.PathUtils;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodeToNodeCodec {
    private final SchemaContext ctx;
    private static final Logger logger = LoggerFactory.getLogger(NormalizedNodeToNodeCodec.class);

    public NormalizedNodeToNodeCodec(final SchemaContext ctx){
        this.ctx = ctx;

    }

    public NormalizedNodeMessages.Container encode(YangInstanceIdentifier id, NormalizedNode node){

        NormalizedNodeMessages.Container.Builder builder = NormalizedNodeMessages.Container.newBuilder();
        String parentPath = "";

        if(id != null){
            parentPath = PathUtils.getParentPath(PathUtils.toString(id));
        }

        builder.setParentPath(parentPath);
        if(node != null) {
            builder.setNormalizedNode(NormalizedNodeSerializer.serialize(node));
        }

        return builder.build();
    }

    public NormalizedNode<?,?> decode(YangInstanceIdentifier id, NormalizedNodeMessages.Node node){
        if(node.getIntType() < 0 || node.getSerializedSize() == 0){
            return null;
        }
        return NormalizedNodeSerializer.deSerialize(node);
    }


}
