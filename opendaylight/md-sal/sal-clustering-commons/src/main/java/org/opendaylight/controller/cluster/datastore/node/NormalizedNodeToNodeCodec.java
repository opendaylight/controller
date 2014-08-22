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

import com.google.common.base.Optional;
import org.opendaylight.controller.cluster.datastore.node.utils.PathUtils;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
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

        NormalizedNodeMessages.Container.Builder containerBuilder =
            NormalizedNodeMessages.Container.newBuilder();

        NormalizedNodeMessages.Container container;

        if (node != null) {

            NormalizedNodeMessages.Node compositeNodeCompatible =
                CompositeNodeCompatibility.toNode(node);

            container =
                containerBuilder.setParentPath(parentPath).setNormalizedNode(
                    compositeNodeCompatible).build();
        } else {
            //this can happen when an attempt was made to read from datastore and normalized node was null.
            container = containerBuilder.setParentPath(parentPath).build();
        }

        return container;


    }

    public NormalizedNode<?,?> decode(YangInstanceIdentifier id, NormalizedNodeMessages.Node node){


        DataNormalizer normalizer = new DataNormalizer(ctx);

        try {

            // Note : To get to the schemaNode it appears that you need a new
            // YangInstanceIdentifier instead of the legacy one. This may be
            // a bug in DataNormalizationOperation

            Optional<DataSchemaNode> optionalSchemaNode =
                normalizer.getOperation(id).getDataSchemaNode();

            DataSchemaNode schemaNode = null;

            if(optionalSchemaNode.isPresent()) {
                schemaNode = optionalSchemaNode.get();
            }

            if(node.getChildCount() == 0){
                return CompositeNodeCompatibility.toSimpleNormalizedNode(node, schemaNode);
            }

            CompositeNode compositeNode = CompositeNodeCompatibility
                .toComposite(node, schemaNode);

            YangInstanceIdentifier idLegacy = normalizer.toLegacy(id);

            return normalizer.toNormalized(idLegacy, compositeNode).getValue();
        } catch (DataNormalizationException e) {
            logger.warn("Exception occurred when decoding node "
                + String.format("id = [%s], node = [%s], originalError = [%s]",
                        id.toString(), node.toString(), e.getMessage()));
            return null;
        }


    }


}
