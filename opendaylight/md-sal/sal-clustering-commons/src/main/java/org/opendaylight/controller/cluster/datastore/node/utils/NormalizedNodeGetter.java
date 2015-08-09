/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodeGetter implements
    NormalizedNodeVisitor {
    private final String path;
    NormalizedNode<?, ?> output;

    public NormalizedNodeGetter(String path){
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode<?, ?> normalizedNode) {
        String nodePath = parentPath + "/"+ PathUtils.toString(normalizedNode.getIdentifier());

        if(nodePath.toString().equals(path)){
            output = normalizedNode;
        }
    }

    public NormalizedNode<?, ?> getOutput(){
        return output;
    }
}
