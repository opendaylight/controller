/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

import java.util.List;

class LeafListNodeModification implements Modification<LeafListSchemaNode, Modification.NodeWrapper> {

    @Override
    public NodeWrapper modify(LeafListSchemaNode schemaNode, NodeWrapper actualNodes, NodeWrapper modificationNodes,
            OperationStack operationStack) throws DataModificationException {

        // Merge or None operation on parent, leaving actual if modification not present
        if(modificationNodes.isEmpty())
            return actualNodes;

        List<Node<?>> resultNodes = Lists.newArrayList(actualNodes.getNodes());

        // TODO implement ordering

        for (Node<?> leafListModification : modificationNodes.getNodes()) {
            operationStack.enteringNode(leafListModification);

            switch (operationStack.getCurrentOperation()) {
            case MERGE:
            case CREATE: {
                DataModificationException.DataExistsException.check(schemaNode.getQName(), actualNodes, leafListModification);
            }
            case REPLACE: {
                if (actualNodes.contains(leafListModification) == false) {
                    resultNodes.add(leafListModification);
                }
                break;
            }
            case DELETE: {
                DataModificationException.DataMissingException.check(schemaNode.getQName(), actualNodes, leafListModification);
            }
            case REMOVE: {
                if (resultNodes.contains(leafListModification)) {
                    resultNodes.remove(leafListModification);
                }
                break;
            }
            case NONE: {
                break;
            }
            }

            operationStack.exitingNode(leafListModification);
        }
        return NodeWrappers.wrapNodes(resultNodes);
    }
}
