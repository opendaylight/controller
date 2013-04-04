/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;

public abstract class AbstractContainerNode extends AbstractNode<List<Node<?>>>
        implements CompositeNode {

    public SimpleNode<?> getFirstSimpleByName(QName leaf) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leaf);
        if (list.size() == 0)
            return null;
        return list.get(0);
    }

    protected AbstractContainerNode(QName name, CompositeNode parent) {
        super(name, parent);
    }

    public AbstractContainerNode(QName name) {
        super(name, null);
    }

    public List<Node<?>> getChildren() {
        return getValue();
    }

    public List<Node<?>> getValue() {
        Map<QName, List<Node<?>>> map = getNodeMap();
        if (map == null)
            throw new IllegalStateException("nodeMap should not be null");
        List<Node<?>> ret = new ArrayList<Node<?>>();
        Collection<List<Node<?>>> values = map.values();
        for (List<Node<?>> list : values) {
            ret.addAll(list);
        }
        return ret;
    }

    protected abstract Map<QName, List<Node<?>>> getNodeMap();

    public List<CompositeNode> getCompositesByName(QName children) {
        Map<QName, List<Node<?>>> map = getNodeMap();
        if (map == null)
            throw new IllegalStateException("nodeMap should not be null");
        List<Node<?>> toFilter = map.get(children);
        List<CompositeNode> list = new ArrayList<CompositeNode>();
        for (Node<?> node : toFilter) {
            if (node instanceof CompositeNode)
                list.add((CompositeNode) node);
        }
        return list;
    }

    public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
        Map<QName, List<Node<?>>> map = getNodeMap();
        if (map == null)
            throw new IllegalStateException("nodeMap should not be null");
        List<Node<?>> toFilter = map.get(children);
        List<SimpleNode<?>> list = new ArrayList<SimpleNode<?>>();

        for (Node<?> node : toFilter) {
            if (node instanceof SimpleNode<?>)
                list.add((SimpleNode<?>) node);
        }
        return list;
    }

    public CompositeNode getFirstCompositeByName(QName container) {
        List<CompositeNode> list = getCompositesByName(container);
        if (list.size() == 0)
            return null;
        return list.get(0);
    }

    public SimpleNode<?> getFirstLeafByName(QName leaf) {
        List<SimpleNode<?>> list = getSimpleNodesByName(leaf);
        if (list.size() == 0)
            return null;
        return list.get(0);
    }

    public List<CompositeNode> getCompositesByName(String children) {
        return getCompositesByName(localQName(children));
    }

    public List<SimpleNode<?>> getSimpleNodesByName(String children) {
        return getSimpleNodesByName(localQName(children));
    }

    private QName localQName(String str) {
        return new QName(getNodeType(), str);
    }
}
