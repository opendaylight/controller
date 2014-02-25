/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that wraps List of Nodes and adds utility methods
 */
final class NodeWrappers {

    public static Modification.SingleNodeWrapper wrapNode(final List<Node<?>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return NodeWrappers.EMPTY_SINLGLE_WRAPPER;
        }

        Preconditions.checkArgument(nodes.size()==1, "Multiple nodes present when only 1 expected in %s", nodes);
        return wrapNode(nodes.get(0));
    }

    public static Modification.SingleNodeWrapper wrapNode(final Node<?> node) {
        if (node == null) {
            return NodeWrappers.EMPTY_SINLGLE_WRAPPER;
        }

        return new Modification.SingleNodeWrapper() {

            @Override
            public List<Node<?>> getNodes() {
                return Lists.<Node<?>>newArrayList(node);
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Node<?> getSingleNode() {
                return node;
            }

            @Override
            public boolean contains(Node<?> otherNode) {
                return node.equals(otherNode);
            }
        };
    }

    public static Modification.NodeWrapper wrapNodes(final List<Node<?>> nodes) {

        if (nodes == null || nodes.isEmpty())
            return NodeWrappers.EMPTY_SINLGLE_WRAPPER;

        final Set<Node<?>> uniqueNodes = Sets.newLinkedHashSet(nodes);
        Preconditions.checkArgument(nodes.size() == uniqueNodes.size(), "Duplicate node detected in %s", nodes);

        return new Modification.NodeWrapper() {

            public List<Node<?>> getNodes() {
                return nodes;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Node<?> leafListModification) {
                return uniqueNodes.contains(leafListModification);
            }
        };

    }

    public static Modification.NodeWrapper emptyWrapper() {
        return EMPTY_SINLGLE_WRAPPER;
    }

    public static Modification.SingleNodeWrapper emptySingleNodeWrapper() {
        return EMPTY_SINLGLE_WRAPPER;
    }


    private static final Modification.SingleNodeWrapper EMPTY_SINLGLE_WRAPPER = new Modification.SingleNodeWrapper() {

        @Override
        public List<Node<?>> getNodes() {
            throw new UnsupportedOperationException("empty nodes");
        }

        @Override
        public boolean contains(Node<?> leafListModification) {
            throw new UnsupportedOperationException("empty nodes");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Node<?> getSingleNode() {
            throw new UnsupportedOperationException("empty nodes");
        }
    };

    private static final Modification.ListNodeWrapper EMPTY_LIST_WRAPPER = new Modification.ListNodeWrapper() {


        @Override
        public boolean contains(Modification.ListNodeKey key) {
            return EMPTY_SINLGLE_WRAPPER.contains(null);
        }

        @Override
        public Node<?> getSingleNode(Modification.ListNodeKey key) {
            return EMPTY_SINLGLE_WRAPPER.getSingleNode();
        }

        @Override
        public Node<?> getSingleNode(Node<?> key) {
            return EMPTY_SINLGLE_WRAPPER.getSingleNode();
        }

        @Override
        public Map<Modification.ListNodeKey, Node<?>> getMappedNodes() {
            throw new UnsupportedOperationException("empty nodes");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Node<?> leafListModification) {
            return EMPTY_SINLGLE_WRAPPER.contains(null);
        }

        @Override
        public List<Node<?>> getNodes() {
            return EMPTY_SINLGLE_WRAPPER.getNodes();
        }
    };

    public static Modification.ListNodeWrapper wrapListNodes(final ListSchemaNode schemaNode, List<Node<?>> nodes) {
        if (nodes == null || nodes.isEmpty())
            return NodeWrappers.EMPTY_LIST_WRAPPER;

        final Map<Modification.ListNodeKey, Node<?>> mappedListNodes = mapListNodes(schemaNode, nodes);

        return new Modification.ListNodeWrapper() {
            @Override
            public boolean contains(Modification.ListNodeKey key) {
                return mappedListNodes.containsKey(key);
            }

            @Override
            public Node<?> getSingleNode(Modification.ListNodeKey key) {
                return Preconditions.checkNotNull(mappedListNodes.get(key), "No value mapped to key %s", key);
            }

            @Override
            public Node<?> getSingleNode(Node<?> node) {
                return getSingleNode(getKeyForListNode(schemaNode, node));
            }

            @Override
            public Map<Modification.ListNodeKey, Node<?>> getMappedNodes() {
                return ImmutableMap.copyOf(mappedListNodes);
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Node<?> leafListModification) {
                return contains(getKeyForListNode(schemaNode, leafListModification));
            }

            @Override
            public List<Node<?>> getNodes() {
                return Lists.newArrayList(mappedListNodes.values());
            }
        };
    }

    public static Map<Modification.ListNodeKey, Node<?>> mapListNodes(ListSchemaNode schemaNode, List<Node<?>> nodes) {
        final Map<Modification.ListNodeKey, Node<?>> mappedListNodes = Maps.newLinkedHashMap();
        for (Node<?> node : nodes) {
            mappedListNodes.put(getKeyForListNode(schemaNode, node), node);
        }
        Preconditions.checkArgument(nodes.size() == mappedListNodes.size(), "Duplicate nodes detected in list %s", nodes);
        return mappedListNodes;
    }

    public static Modification.ListNodeKey getKeyForListNode(ListSchemaNode schemaNode, Node<?> listModification) {
        Preconditions.checkArgument(schemaNode.getQName().equals(listModification.getNodeType()),
                "Incompatible node: %s : %s", schemaNode, listModification);

        Preconditions.checkArgument(listModification instanceof CompositeNode);
        CompositeNode composite = (CompositeNode) listModification;

        List<SimpleNode<?>> keys = Lists.newArrayList();

        if(schemaNode.getKeyDefinition().isEmpty()) {
            return ListNodeKeyImpl.from(listModification);
        }

        for (QName qName : schemaNode.getKeyDefinition()) {
            List<SimpleNode<?>> keyLeaves = composite.getSimpleNodesByName(qName);
            // TODO transfor to element checked missing exception
            Preconditions.checkState(keyLeaves != null && keyLeaves.size() == 1, "Element missing for %s in %s", qName, schemaNode.getQName());
            keys.add(keyLeaves.get(0));
        }

        return ListNodeKeyImpl.from(keys);
    }

    /**
     * Simple wrapper over List key. Key for list can be List of SimpleNodes or
     * the node itself if key statement is not present
     */
    private static final class ListNodeKeyImpl implements Modification.ListNodeKey {
        private final Object innerKey;

        private ListNodeKeyImpl(Object innerKey) {
            this.innerKey = Preconditions.checkNotNull(innerKey);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ListNodeKeyImpl)) return false;

            ListNodeKeyImpl that = (ListNodeKeyImpl) o;

            if (!innerKey.equals(that.innerKey)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return innerKey.hashCode();
        }

        public static Modification.ListNodeKey from(Object innerKey) {
            return new ListNodeKeyImpl(innerKey);
        }
    }
}
