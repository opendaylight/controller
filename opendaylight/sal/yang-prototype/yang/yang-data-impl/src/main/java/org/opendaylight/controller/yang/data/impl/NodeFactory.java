/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.MutableSimpleNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.NodeModification;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 * 
 */
public abstract class NodeFactory {

    /**
     * @param qName
     * @param parent
     * @param value
     * @return simple node modification, based on given qname, value and parent
     */
    public static <T> SimpleNode<T> createImmutableSimpleNode(QName qName,
            CompositeNode parent, T value) {
        return createImmutableSimpleNode(qName, parent, value, null);
    }
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @param modifyAction 
     * @param original originating node, if available
     * @return simple node modification, based on given qname, value and parent
     */
    public static <T> MutableSimpleNode<T> createMutableSimpleNode(QName qName,
            CompositeNode parent, Object value, ModifyAction modifyAction, SimpleNode<T> original) {
        @SuppressWarnings("unchecked")
        MutableSimpleNodeTOImpl<T> simpleNodeTOImpl = 
                new MutableSimpleNodeTOImpl<T>(qName, parent, (T) value, modifyAction);
        simpleNodeTOImpl.setOriginal(original);
        return simpleNodeTOImpl;
    }
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @return composite node modification, based on given qname, value (children), parent and modifyAction
     */
    public static CompositeNode createImmutableCompositeNode(QName qName,
            CompositeNode parent, List<Node<?>> value) {
        return createImmutableCompositeNode(qName, parent, value, null);
    }
    
    /**
     * @param qName
     * @param parent
     * @param valueArg 
     * @param modifyAction 
     * @param original originating node, if available
     * @return composite node modification, based on given qName, value (children), parent and modifyAction
     */
    public static MutableCompositeNode createMutableCompositeNode(QName qName,
            CompositeNode parent, List<Node<?>> valueArg, ModifyAction modifyAction, CompositeNode original) {
        List<Node<?>> value = valueArg;
        if (value == null) {
            value = new ArrayList<>();
        }
        MutableCompositeNodeTOImpl compositeNodeTOImpl = 
                new MutableCompositeNodeTOImpl(qName, parent, value, modifyAction);
        compositeNodeTOImpl.setOriginal(original);
        return compositeNodeTOImpl;
    }
    
    
    /**
     * @param qName
     * @param parent
     * @param value
     * @param modifyAction
     * @return simple node modification, based on given qname, value, parent and modifyAction
     */
    public static <T> SimpleNode<T> createImmutableSimpleNode(QName qName,
            CompositeNode parent, T value, ModifyAction modifyAction) {
        SimpleNodeTOImpl<T> simpleNodeModTOImpl = 
                new SimpleNodeTOImpl<T>(qName, parent, value, modifyAction);
        return simpleNodeModTOImpl;
    }

    /**
     * @param qName
     * @param parent
     * @param value
     * @param modifyAction 
     * @return composite node modification, based on given qname, value (children), parent and modifyAction
     */
    public static CompositeNode createImmutableCompositeNode(QName qName,
            CompositeNode parent, List<Node<?>> value, ModifyAction modifyAction) {
        CompositeNodeTOImpl compositeNodeModTOImpl = 
                new CompositeNodeTOImpl(qName, parent, value, modifyAction);
        return compositeNodeModTOImpl;
    }

    /**
     * @param node
     * @return copy of given node, parent and value are the same, but parent 
     * has no reference to this copy 
     */
    public static <T> SimpleNode<T> copyNode(SimpleNode<T> node) {
        SimpleNode<T> twinNode = createImmutableSimpleNode(
                    node.getNodeType(), node.getParent(), node.getValue());
        return twinNode;
    }
    
    /**
     * @param node
     * @return copy of given node, parent and value are the same, but parent 
     * has no reference to this copy 
     */
    public static <T> MutableSimpleNode<T> copyNodeAsMutable(SimpleNode<T> node) {
        MutableSimpleNode<T> twinNode = createMutableSimpleNode(
                    node.getNodeType(), node.getParent(), node.getValue(), 
                    node.getModificationAction(), null);
        return twinNode;
    }
    
    /**
     * @param node
     * @param children 
     * @return copy of given node, parent and children are the same, but parent and children 
     * have no reference to this copy
     */
    public static CompositeNode copyNode(CompositeNode node, Node<?>... children) {
        CompositeNode twinNode = createImmutableCompositeNode(
                node.getNodeType(), node.getParent(), Arrays.asList(children), node.getModificationAction());
        return twinNode;
    }
    
    /**
     * @param node
     * @return copy of given node, parent and children are the same, but parent and children 
     * have no reference to this copy
     */
    public static CompositeNode copyNode(CompositeNode node) {
       return copyNode(node, node.getChildren().toArray(new Node<?>[0]));
    }
    
    /**
     * @param node root of original tree
     * @param originalToCopyArg (optional) empty map, where binding between original and copy 
     * will be stored
     * @return copy of given node and all subnodes recursively
     */
    public static MutableCompositeNode copyDeepAsMutable(CompositeNode node, 
            Map<Node<?>, Node<?>> originalToCopyArg) {
        
        Map<Node<?>, Node<?>> originalToCopy = originalToCopyArg;
        if (originalToCopy == null) {
            originalToCopy = new HashMap<>();
        }

        MutableCompositeNode mutableRoot = createMutableCompositeNode(node.getNodeType(), null, null, 
                node.getModificationAction(), null);
        Stack<SimpleEntry<CompositeNode, MutableCompositeNode>> jobQueue = new Stack<>();
        jobQueue.push(new SimpleEntry<CompositeNode, MutableCompositeNode>(node, mutableRoot));
        originalToCopy.put(node, mutableRoot);

        while (!jobQueue.isEmpty()) {
            SimpleEntry<CompositeNode, MutableCompositeNode> job = jobQueue.pop();
            CompositeNode originalNode = job.getKey();
            MutableCompositeNode mutableNode = job.getValue();
            mutableNode.setValue(new ArrayList<Node<?>>());

            for (Node<?> child : originalNode.getChildren()) {
                Node<?> mutableAscendant = null;
                if (child instanceof CompositeNode) {
                    MutableCompositeNode newMutable = 
                            createMutableCompositeNode(child.getNodeType(), mutableNode, null, 
                                    ((NodeModification) child).getModificationAction(), null);
                    jobQueue.push(new SimpleEntry<CompositeNode, MutableCompositeNode>(
                            (CompositeNode) child, newMutable));
                    mutableAscendant = newMutable;
                } else if (child instanceof SimpleNode<?>) {
                    mutableAscendant = 
                            createMutableSimpleNode(child.getNodeType(), mutableNode, 
                                    child.getValue(), 
                                    ((NodeModification) child).getModificationAction(), null);
                } else {
                    throw new IllegalStateException("Node class deep copy not supported: "
                            +child.getClass().getName());
                }

                mutableNode.getChildren().add(mutableAscendant);
                originalToCopy.put(child, mutableAscendant);
            }
            mutableNode.init();
        }

        return mutableRoot;
    }
    
    /**
     * @param node root of original tree
     * @param originalToCopyArg (optional) empty map, where binding between original and copy 
     * will be stored
     * @return copy of given node and all subnodes recursively
     */
    public static CompositeNode copyDeepAsImmutable(CompositeNode node, 
            Map<Node<?>, Node<?>> originalToCopyArg) {
        Stack<CompositeNode> jobQueue = new Stack<>();
        jobQueue.push(node);
        
        Map<Node<?>, Node<?>> originalToCopy = originalToCopyArg;
        if (originalToCopy == null) {
            originalToCopy = new HashMap<>();
        }
        
        while (!jobQueue.isEmpty()) {
            CompositeNode jobNode = jobQueue.peek();
            if (!originalToCopy.isEmpty() 
                    && originalToCopy.keySet().containsAll(jobNode.getChildren())) {
                jobQueue.pop();
                List<Node<?>> newChildren = NodeUtils.collectMapValues(jobNode.getChildren(), originalToCopy);
                CompositeNode nodeCopy = createImmutableCompositeNode(jobNode.getNodeType(), null, 
                        newChildren, jobNode.getModificationAction());
                NodeUtils.fixChildrenRelation(nodeCopy);
                originalToCopy.put(jobNode, nodeCopy);
            } else {
                for (Node<?> child : jobNode.getChildren()) {
                    if (child instanceof SimpleNode<?>) {
                        originalToCopy.put(child, createImmutableSimpleNode(
                                child.getNodeType(), null, child.getValue(), 
                                ((NodeModification) child).getModificationAction()));
                    } else if (child instanceof CompositeNode) {
                        jobQueue.push((CompositeNode) child);
                    }
                }
            }
        }
       
        return (CompositeNode) originalToCopy.get(node);
    }
    
}
