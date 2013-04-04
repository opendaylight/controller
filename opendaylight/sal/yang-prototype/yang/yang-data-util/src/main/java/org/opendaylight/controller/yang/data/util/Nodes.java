/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;

public class Nodes {

    private Nodes() {
    }

    public static <T> SimpleNode<T> leafNode(QName name, T value) {
        return new SimpleNodeTO<T>(name, value, null);
    }

    public static CompositeNode containerNode(QName name, List<Node<?>> children) {
        return containerNode(name, children, null);
    }

    public static CompositeNode containerNode(QName name,
            List<Node<?>> children, CompositeNode parent) {
        return new ContainerNodeTO(name, parent, nodeMapFromList(children));
    }

    public static Map<QName, List<Node<?>>> nodeMapFromList(
            List<Node<?>> children) {
        Map<QName, List<Node<?>>> map = new HashMap<QName, List<Node<?>>>();
        for (Node<?> node : children) {

            QName name = node.getNodeType();
            List<Node<?>> targetList = map.get(name);
            if (targetList == null) {
                targetList = new ArrayList<Node<?>>();
                map.put(name, targetList);
            }
            targetList.add(node);
        }
        return map;
    }

    private static class ContainerNodeTO extends AbstractContainerNode {

        private final Map<QName, List<Node<?>>> nodeMap;

        public ContainerNodeTO(QName name, Map<QName, List<Node<?>>> nodeMap) {
            super(name);
            this.nodeMap = nodeMap;
        }

        public ContainerNodeTO(QName name, CompositeNode parent,
                Map<QName, List<Node<?>>> nodeMap) {
            super(name, parent);
            this.nodeMap = nodeMap;
        }

        @Override
        protected Map<QName, List<Node<?>>> getNodeMap() {

            return nodeMap;
        }
    }

    private static class SimpleNodeTO<T> extends AbstractNode<T> implements
            SimpleNode<T> {

        private final T value;

        protected SimpleNodeTO(QName name, T val, CompositeNode parent) {
            super(name, parent);
            value = val;

        }

        @Override
        public T getValue() {
            return value;
        }

    }

}
