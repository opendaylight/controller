/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NormalizedNodePrinter implements NormalizedNodeVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodePrinter.class);

    private static String spaces(int n){
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<n;i++){
            builder.append(' ');
        }
        return builder.toString();
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode<?, ?> normalizedNode) {
        LOG.info(spaces(level * 4) + normalizedNode.getClass().toString() + ":" + normalizedNode.getIdentifier());
        if(normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode){
            LOG.info(spaces(level * 4) + " parentPath = " + parentPath);
            LOG.info(spaces(level * 4) + " key = " + normalizedNode.getClass().toString() + ":" + normalizedNode.getIdentifier());
            LOG.info(spaces(level * 4) + " value = " + normalizedNode.getValue());
        }
    }
}
