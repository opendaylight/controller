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
import com.google.common.collect.Sets;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.model.api.*;

import java.util.*;

class ContainerNodeModification implements Modification<ContainerSchemaNode, Modification.SingleNodeWrapper> {

    public static final LeafNodeModification LEAF_NODE_MODIFICATION = new LeafNodeModification();
    public static final LeafListNodeModification LEAF_LIST_NODE_MODIFICATION = new LeafListNodeModification();
    public static final ListNodeModification LIST_NODE_MODIFICATION = new ListNodeModification();

    @Override
    public Modification.SingleNodeWrapper modify(ContainerSchemaNode schema,
            Modification.SingleNodeWrapper actualNodes, Modification.SingleNodeWrapper modificationNodes,
            OperationStack operationStack) throws DataModificationException {

        return modify(schema, actualNodes, modificationNodes, operationStack, schema.getQName());
    }

    Modification.SingleNodeWrapper modify(DataNodeContainer schema, Modification.SingleNodeWrapper actualNodes,
            Modification.SingleNodeWrapper modificationNodes, OperationStack operationStack, QName nodeQName)
            throws DataModificationException {

        if (modificationNodes.isEmpty() == false) {
            operationStack.enteringNode(modificationNodes.getSingleNode());
        }

        SingleNodeWrapper modified = null;

        switch (operationStack.getCurrentOperation()) {
            case DELETE: {
                DataModificationException.DataMissingException.check(nodeQName, actualNodes);
            }
            case REMOVE: {
                modified = NodeWrappers.emptySingleNodeWrapper();
                break;
            }
            case CREATE: {
                DataModificationException.DataExistsException.check(nodeQName, actualNodes);
            }
            case REPLACE: {
                modified = modificationNodes;
                break;
            }
            case NONE:
                DataModificationException.DataMissingException.check(nodeQName, actualNodes);
            case MERGE: {
                modified = mergeContainers(schema, actualNodes, modificationNodes, operationStack);
                break;
            }
        }

        if (modificationNodes.isEmpty() == false) {
            operationStack.exitingNode(modificationNodes.getSingleNode());
        }
        return modified;
    }

    private SingleNodeWrapper mergeContainers(DataNodeContainer schema, SingleNodeWrapper actualNodes, SingleNodeWrapper modificationNodes, OperationStack operationStack) throws DataModificationException {
        if (actualNodes.isEmpty()) {
            return modificationNodes;
        }

        if (modificationNodes.isEmpty()) {
            return actualNodes;
        }

        Preconditions.checkArgument(actualNodes.getSingleNode() instanceof CompositeNode);
        CompositeNode actual = (CompositeNode) actualNodes.getSingleNode();
        Preconditions.checkArgument(modificationNodes.getSingleNode() instanceof CompositeNode);
        CompositeNode modification = (CompositeNode) modificationNodes.getSingleNode();

        Set<QName> toProcess = Sets.newLinkedHashSet(actual.keySet());
        toProcess.addAll(modification.keySet());

        List<Node<?>> merged = mergeContainersChildNodes(schema, operationStack, actual, modification, toProcess);
        return NodeWrappers.wrapNode(ImmutableCompositeNode.create(actual.getNodeType(), merged));
    }

    private List<Node<?>> mergeContainersChildNodes(DataNodeContainer schema, OperationStack operationStack,
            CompositeNode actual, CompositeNode modification, Set<QName> toProcess) throws DataModificationException {
        List<Node<?>> merged = Lists.newArrayList();

        List<QName> unknownChildNodes = Lists.newArrayList();

        for (QName childToProcessQName : toProcess) {
            DataSchemaNode schemaOfChildToProcess = schema.getDataChildByName(childToProcessQName);

            if (schemaOfChildToProcess == null) {
                unknownChildNodes.add(childToProcessQName);
                continue;
            }

            NodeWrapper modifiedValues = mergeContainersChildNode(operationStack, actual, modification,
                    childToProcessQName, schemaOfChildToProcess);

            if (modifiedValues.isEmpty() == false) {
                merged.addAll(modifiedValues.getNodes());
            }
        }

        // Try to figure out unknown child nodes by mapping them to choices/cases
        // TODO refactor, hard to read
        // TODO preserve order of xml nodes somehow
        // TODO test negative cases
        ChoiceCaseNodeMap choiceCaseNodeMap = ChoiceCaseNodeMap.fromUnknownNodes(schema, unknownChildNodes);
        Map<QName, ChoiceCaseNodeMap.ChoiceMap> mappedChoices = choiceCaseNodeMap.getMappedChoices();
        for (QName choiceQName : mappedChoices.keySet()) {
            ChoiceCaseNodeMap.ChoiceMap mappedCases = mappedChoices.get(choiceQName);

            QName caseToProcessFromModification = null;
            QName caseToProcessFromActual = null;
            for (QName nodeFromCase : mappedCases.getAllNodes()) {
                if(modification.containsKey(nodeFromCase)) {
                    if(caseToProcessFromModification!=null) {
                        // check for nodes from different cases
                        DataModificationException.IllegalChoiceValuesException.throwMultipleCasesReferenced(
                                choiceQName, modification, caseToProcessFromModification,
                                mappedCases.getCaseQNameForNodeQName(nodeFromCase));

                    }
                    caseToProcessFromModification = mappedCases.getCaseQNameForNodeQName(nodeFromCase);
                }

                if(actual.containsKey(nodeFromCase)) {
                    if(caseToProcessFromActual!=null) {
                        // check for nodes from different cases
                        DataModificationException.IllegalChoiceValuesException.throwMultipleCasesReferenced(
                                choiceQName, actual, caseToProcessFromModification,
                                mappedCases.getCaseQNameForNodeQName(nodeFromCase));
                    }
                    caseToProcessFromActual = mappedCases.getCaseQNameForNodeQName(nodeFromCase);
                }
            }

            // TODO validate when statement for found case

            QName caseToProcess = caseToProcessFromModification == null ? caseToProcessFromActual : caseToProcessFromModification;

            // Should not happen since we process case nodes referenced in actual + modification
            Preconditions.checkState(caseToProcess != null,
                    "Unable to find case statement to process, no case referenced in actual data and modification");

            for (QName childToProcessQName :  mappedCases.getChildNodesForCaseQName(caseToProcess)) {
                DataSchemaNode schemaOfChildToProcess = mappedCases.getSchemaForNode(childToProcessQName);

                NodeWrapper modifiedValues = mergeContainersChildNode(operationStack, actual, modification,
                        childToProcessQName, schemaOfChildToProcess);

                if (modifiedValues.isEmpty() == false) {
                    merged.addAll(modifiedValues.getNodes());
                }
            }

        }

        return merged;
    }

    private NodeWrapper mergeContainersChildNode(OperationStack operationStack, CompositeNode actual, CompositeNode modification, QName childToProcessQName, DataSchemaNode schemaChild) throws DataModificationException {

        List<Node<?>> storedChildren = actual.get(childToProcessQName);
        List<Node<?>> modifiedChildren = modification.get(childToProcessQName);

        return dispatchModification(schemaChild, storedChildren, modifiedChildren,
                operationStack);
    }

    private NodeWrapper dispatchModification(DataSchemaNode schemaChild, List<Node<?>> actualNodes,
            List<Node<?>> modificationNodes, OperationStack operationStack) throws DataModificationException {

        if (schemaChild instanceof LeafSchemaNode) {
            return LEAF_NODE_MODIFICATION.modify((LeafSchemaNode) schemaChild, NodeWrappers.wrapNode(actualNodes),
                    NodeWrappers.wrapNode(modificationNodes), operationStack);
        } else if (schemaChild instanceof ContainerSchemaNode) {
            return modify((ContainerSchemaNode) schemaChild, NodeWrappers.wrapNode(actualNodes),
                    NodeWrappers.wrapNode(modificationNodes), operationStack);
        } else if (schemaChild instanceof LeafListSchemaNode) {
            return LEAF_LIST_NODE_MODIFICATION.modify((LeafListSchemaNode) schemaChild,
                    NodeWrappers.wrapNodes(actualNodes), NodeWrappers.wrapNodes(modificationNodes), operationStack);
        } else if (schemaChild instanceof ListSchemaNode) {
            ListSchemaNode listSchema = (ListSchemaNode) schemaChild;
            return LIST_NODE_MODIFICATION.modify(listSchema, NodeWrappers.wrapListNodes(listSchema, actualNodes),
                    NodeWrappers.wrapListNodes(listSchema, modificationNodes), operationStack);
        }

        throw new IllegalArgumentException("Unknown schema node type " + schemaChild);
    }
}
