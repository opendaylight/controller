/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;

/**
 * @author michal.rehak
 * 
 */
public class CompositeNodeTOImpl extends AbstractNodeTO<List<Node<?>>>
        implements CompositeNode {

    private Map<QName, List<Node<?>>> nodeMap;

    /**
     * @param qname
     * @param parent use null to create top composite node (without parent)
     * @param value
     */
    public CompositeNodeTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value) {
        super(qname, parent, value);
        if (value != null) {
            nodeMap = NodeUtils.buildNodeMap(getValue());
        }
        init();
    }

    /**
     * @param qname
     * @param parent use null to create top composite node (without parent)
     * @param value
     * @param modifyAction 
     */
    public CompositeNodeTOImpl(QName qname, CompositeNode parent,
            List<Node<?>> value, ModifyAction modifyAction) {
        super(qname, parent, value, modifyAction);
        init();
    }
    

    /**
     * @return the nodeMap
     */
    protected Map<QName, List<Node<?>>> getNodeMap() {
        return nodeMap;
    }
    
    @Override
    public List<Node<?>> getChildren() {
        return getValue();
    }

    @Override
    public SimpleNode<?> getFirstSimpleByName(QName leafQName) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leafQName);
        if (list.isEmpty())
            return null;
        return list.get(0);
    }

    @Override
    public List<CompositeNode> getCompositesByName(QName children) {
        List<Node<?>> toFilter = getNodeMap().get(children);
        List<CompositeNode> list = new ArrayList<CompositeNode>();
        for (Node<?> node : toFilter) {
            if (node instanceof CompositeNode)
                list.add((CompositeNode) node);
        }
        return list;
    }

    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
        List<Node<?>> toFilter = getNodeMap().get(children);
        List<SimpleNode<?>> list = new ArrayList<SimpleNode<?>>();

        for (Node<?> node : toFilter) {
            if (node instanceof SimpleNode<?>)
                list.add((SimpleNode<?>) node);
        }
        return list;
    }

    @Override
    public CompositeNode getFirstCompositeByName(QName container) {
        List<CompositeNode> list = getCompositesByName(container);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * @param leaf
     * @return TODO:: do we need this method?
     */
    public SimpleNode<?> getFirstLeafByName(QName leaf) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leaf);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<CompositeNode> getCompositesByName(String children) {
        return getCompositesByName(new QName(getNodeType(), children));
    }
    
    @Override
    public List<SimpleNode<?>> getSimpleNodesByName(String children) {
        return getSimpleNodesByName(new QName(getNodeType(), children));
    }

    /**
     * @param value
     */
    protected void init() {
        if (getValue() != null) {
            nodeMap = NodeUtils.buildNodeMap(getValue());
        }
    }
    
    @Override
    public MutableCompositeNode asMutable() {
        throw new IllegalAccessError("cast to mutable is not supported - "+getClass().getSimpleName());
    }
    
    @Override
    public String toString() {
        return super.toString() + ", children.size = " 
                + (getChildren() != null ? getChildren().size() : "n/a");
    }
    
    

}
