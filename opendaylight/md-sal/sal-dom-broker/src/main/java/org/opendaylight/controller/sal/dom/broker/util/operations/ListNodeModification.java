/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import java.util.Map;

public class ListNodeModification implements Modification<ListSchemaNode, Modification.ListNodeWrapper> {

    public static final ContainerNodeModification CONTAINER_NODE_MODIFICATION = new ContainerNodeModification();

    @Override
    public ListNodeWrapper modify(ListSchemaNode schemaNode, ListNodeWrapper actualNodes,
            ListNodeWrapper modificationNodes, OperationStack operationStack) throws DataModificationException {

        // Merge or None operation on parent, leaving actual if modification not present
        if (modificationNodes.isEmpty())
            return actualNodes;

        Map<Modification.ListNodeKey, Node<?>> resultNodes = Maps.newLinkedHashMap();
        if(actualNodes.isEmpty() == false)
            resultNodes.putAll(actualNodes.getMappedNodes());

        for (Node<?> listModification : modificationNodes.getNodes()) {
            operationStack.enteringNode(listModification);

            Modification.ListNodeKey modificationNodeKey = NodeWrappers.getKeyForListNode(schemaNode, listModification);

            switch (operationStack.getCurrentOperation()) {
            case NONE:
                DataModificationException.DataMissingException.check(schemaNode.getQName(), actualNodes, listModification);
            case MERGE: {
                Node<?> mergedListNode;
                if (actualNodes.contains(listModification)) {
                    SingleNodeWrapper actualListNode = NodeWrappers.wrapNode(actualNodes
                            .getSingleNode(listModification));
                    SingleNodeWrapper modification = CONTAINER_NODE_MODIFICATION.modify(schemaNode, actualListNode,
                            NodeWrappers.wrapNode(listModification), operationStack, schemaNode.getQName());
                    mergedListNode = modification.getSingleNode();
                } else {
                    mergedListNode = listModification;
                }

                resultNodes.put(NodeWrappers.getKeyForListNode(schemaNode, mergedListNode), mergedListNode);
                break;
            }
            case CREATE: {
                DataModificationException.DataExistsException.check(schemaNode.getQName(), actualNodes, listModification);
            }
            case REPLACE: {
                resultNodes.put(modificationNodeKey, listModification);
                break;
            }
            case DELETE: {
                DataModificationException.DataMissingException.check(schemaNode.getQName(), actualNodes, listModification);
            }
            case REMOVE: {
                if (resultNodes.containsKey(modificationNodeKey)) {
                    resultNodes.remove(modificationNodeKey);
                }
                break;
            }
            default:
                throw new IllegalStateException("Unable to perform operation " + operationStack.getCurrentOperation());
            }

            operationStack.exitingNode(listModification);
        }
        return NodeWrappers.wrapListNodes(schemaNode, Lists.newArrayList(resultNodes.values()));
    }

}
