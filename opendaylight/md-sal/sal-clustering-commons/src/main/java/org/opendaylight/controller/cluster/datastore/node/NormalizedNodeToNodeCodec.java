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

import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.cluster.datastore.node.utils.PathUtils;
import org.opendaylight.yangtools.yang.common.QName;
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
        String parentPath = "";

        if(id != null){
            parentPath = PathUtils.getParentPath(id.toString());
        }


        NormalizedNodeToProtocolBufferNode encoder = new NormalizedNodeToProtocolBufferNode();
        encoder.encode(parentPath, node);

        return encoder.getContainer();


    }

    public NormalizedNode<?,?> decode(YangInstanceIdentifier id, NormalizedNodeMessages.Node node){
            NodeToNormalizedNodeBuilder currentOp = NodeToNormalizedNodeBuilder.from(ctx);

            for(YangInstanceIdentifier.PathArgument pathArgument : id.getPathArguments()){
                currentOp = currentOp.getChild(pathArgument);
            }

            QName nodeType = null;

            if(id.getPath().size() < 1){
                nodeType = null;
            } else {
                final YangInstanceIdentifier.PathArgument pathArgument = id.getPath().get(id.getPath().size() - 1);
                if(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier){
                    nodeType = null;
                } else {
                    nodeType = pathArgument.getNodeType();
                }
            }
            if((node != null)&& (!node.getType().isEmpty())){
               return currentOp.normalize(nodeType, node);
            } else{
              return null;
            }
    }


}
