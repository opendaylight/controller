/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModificationBuilder;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaContext;

/**
 * @author michal.rehak
 *
 */
public class NodeModificationBuilderImpl implements NodeModificationBuilder {
    
    private SchemaContext context;
    
    private Set<MutableNode<?>> changeLog;
    private LazyNodeToNodeMap originalToMutable;

    /**
     * @param originalTreeRootNode 
     * @param context
     */
    public NodeModificationBuilderImpl(CompositeNode originalTreeRootNode, SchemaContext context) {
        this.context = context;
        originalToMutable = new LazyNodeToNodeMap();
        changeLog = new HashSet<>();
    }

    /**
     * @param modNode
     * @param action 
     */
    private void addModificationToLog(MutableNode<?> modNode, ModifyAction action) {
        modNode.setModifyAction(action);
        changeLog.add(modNode);
    }

    @Override
    public void addNode(MutableSimpleNode<?> newNode) {
        NodeUtils.fixParentRelation(newNode);
        addModificationToLog(newNode, ModifyAction.CREATE);
    }
    
    @Override
    public void addNode(MutableCompositeNode newNode) {
        NodeUtils.fixParentRelation(newNode);
        addModificationToLog(newNode, ModifyAction.CREATE);
    }
    
    @Override
    public void replaceNode(MutableSimpleNode<?> replacementNode) {
        addModificationToLog(replacementNode, ModifyAction.REPLACE);
    }
    
    @Override
    public void replaceNode(MutableCompositeNode replacementNode) {
        addModificationToLog(replacementNode, ModifyAction.REPLACE);
    }

    @Override
    public void deleteNode(MutableCompositeNode deadNode) {
        addModificationToLog(deadNode, ModifyAction.DELETE);
    }
    
    @Override
    public void deleteNode(MutableSimpleNode<?> deadNode) {
        addModificationToLog(deadNode, ModifyAction.DELETE);
    }

    @Override
    public void removeNode(MutableSimpleNode<?> deadNode) {
        addModificationToLog(deadNode, ModifyAction.REMOVE);
    }
    
    @Override
    public void removeNode(MutableCompositeNode deadNode) {
        addModificationToLog(deadNode, ModifyAction.REMOVE);
    }
    
    @Override
    public void mergeNode(MutableCompositeNode alteredNode) {
        addModificationToLog(alteredNode, ModifyAction.MERGE);
    }

    /**
     * @return minimalistic tree containing diffs only
     */
    @Override
    public CompositeNode buildDiffTree() {
        Set<Node<?>> wanted = new HashSet<>();
        
        // walk changeLog, collect all required nodes
        for (MutableNode<?> mutant : changeLog) {
            wanted.addAll(collectSelfAndAllParents(mutant));
        }
        
        // walk wanted and add relevant keys
        Map<String, ListSchemaNode>  mapOfLists = NodeUtils.buildMapOfListNodes(context);
        for (Node<?> outlaw : wanted) {
            if (outlaw instanceof CompositeNode) {
                String path = NodeUtils.buildPath(outlaw);
                if (mapOfLists.containsKey(path)) {
                    ListSchemaNode listSchema = mapOfLists.get(path);
                    if (listSchema.getQName().equals(outlaw.getNodeType())) {
                        // try to add key subnode to wanted list
                        List<QName> supportedKeys = listSchema.getKeyDefinition();
                        CompositeNode outlawOriginal = ((MutableCompositeNode) outlaw).getOriginal();
                        for (Node<?> outlawOriginalChild : outlawOriginal.getChildren()) {
                            if (supportedKeys.contains(outlawOriginalChild.getNodeType())) {
                                originalToMutable.getMutableEquivalent(outlawOriginalChild);
                            }
                        }
                    }
                }
            }
        }
        
        return originalToMutable.getMutableRoot();
    }

    /**
     * @param focusedDescendant
     * @return set of parents and focusedAncestor itself
     */
    private static Set<Node<?>> collectSelfAndAllParents(Node<?> focusedDescendant) {
        Set<Node<?>> family = new HashSet<>();
        Node<?> tmpNode = focusedDescendant;
        while (tmpNode != null) {
            family.add(tmpNode);
            tmpNode = tmpNode.getParent();
        }
        return family;
    }

    /**
     * @param originalNode
     * @return mutable version of given node
     */
    @Override
    public Node<?> getMutableEquivalent(Node<?> originalNode) {
        return originalToMutable.getMutableEquivalent(originalNode);
    }

}
