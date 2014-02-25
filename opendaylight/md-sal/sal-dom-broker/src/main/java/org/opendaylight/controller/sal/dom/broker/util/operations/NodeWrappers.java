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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that wraps Nodes or collection of nodes and adds utility methods
 */
final class NodeWrappers {

    public static Modification.SingleNodeWrapper wrapNode(final List<Node<?>> nodes) {
        if(nodes == null) {
            return emptySingleNodeWrapper();
        }

        Preconditions.checkArgument(nodes.size()==1, "Multiple nodes present when only 1 expected in %s", nodes);
        return wrapNode(nodes.get(0));
    }

    public static Modification.SingleNodeWrapper wrapNode(final Node<?> node) {
        return new SingleNodeWrapperImpl(node);
    }

    public static Modification.LeafListNodeWrapper wrapNodes(final List<Node<?>> nodes) {
        if (nodes == null) {
            return new LeafListNodeWrapperImpl(Collections.<Node<?>>emptyList(), Collections.<Node<?>>emptySet());
        }

        final Set<Node<?>> uniqueNodes = Sets.newLinkedHashSet(nodes);
        Preconditions.checkArgument(nodes.size() == uniqueNodes.size(), "Duplicate node detected in %s", nodes);

        return new LeafListNodeWrapperImpl(nodes, uniqueNodes);
    }

    public static Modification.ListNodeWrapper wrapListNodes(final ListSchemaNode schemaNode, List<Node<?>> nodes) throws DataModificationException.MissingElementException {
        if (nodes == null) {
            return new ListNodeWrapperImpl(Collections.<Modification.ListNodeKey, Node<?>>emptyMap(), null);
        }

        final Map<Modification.ListNodeKey, Node<?>> mappedListNodes = mapListNodes(schemaNode, nodes);

        return new ListNodeWrapperImpl(mappedListNodes, schemaNode);
    }

    public static Map<Modification.ListNodeKey, Node<?>> mapListNodes(ListSchemaNode schemaNode, List<Node<?>> nodes) throws DataModificationException.MissingElementException {
        final Map<Modification.ListNodeKey, Node<?>> mappedListNodes = Maps.newLinkedHashMap();
        for (Node<?> node : nodes) {
            mappedListNodes.put(getKeyForListNode(schemaNode, node), node);
        }
        Preconditions.checkArgument(nodes.size() == mappedListNodes.size(), "Duplicate nodes detected in list %s", nodes);
        return mappedListNodes;
    }

    public static Modification.ListNodeKey getKeyForListNode(ListSchemaNode schemaNode, Node<?> listNode) throws DataModificationException.MissingElementException {
        Preconditions.checkArgument(schemaNode.getQName().equals(listNode.getNodeType()),
                "Incompatible node: %s : %s", schemaNode, listNode);

        Preconditions.checkArgument(listNode instanceof CompositeNode);
        CompositeNode composite = (CompositeNode) listNode;

        List<SimpleNode<?>> keys = Lists.newArrayList();

        if(schemaNode.getKeyDefinition().isEmpty()) {
            return ListNodeKeyImpl.from(listNode);
        }

        for (QName qName : schemaNode.getKeyDefinition()) {
            List<SimpleNode<?>> keyLeaves = composite.getSimpleNodesByName(qName);
            DataModificationException.MissingElementException.check(keyLeaves, schemaNode.getQName(), qName, listNode);
            keys.add(keyLeaves.get(0));
        }

        return ListNodeKeyImpl.from(keys);
    }

    public static Modification.SingleNodeWrapper emptySingleNodeWrapper() {
        return wrapNode((Node<?>)null);
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

    private static abstract class AbstractNodeWrapper implements Modification.NodeWrapper {

        @Override
        public List<Node<?>> getNodes() {
            checkNotEmpty();
            return getNonEmptyNodes();
        }

        protected void checkNotEmpty() {
            Preconditions.checkState(isEmpty() == false, "Empty wrapper");
        }

        public abstract List<Node<?>> getNonEmptyNodes();
    }

    private static class ListNodeWrapperImpl extends AbstractNodeWrapper implements Modification.ListNodeWrapper {
        private final Map<Modification.ListNodeKey, Node<?>> mappedListNodes;
        private final ListSchemaNode schemaNode;

        public ListNodeWrapperImpl(Map<Modification.ListNodeKey, Node<?>> mappedListNodes, ListSchemaNode schemaNode) {
            this.mappedListNodes = mappedListNodes;
            this.schemaNode = schemaNode;
        }

        @Override
        public boolean contains(Modification.ListNodeKey key) {
            if(isEmpty()) {
                return false;
            }
            return mappedListNodes.containsKey(key);
        }

        @Override
        public boolean contains(Node<?> node) throws DataModificationException.MissingElementException {
            if(isEmpty()) {
                return false;
            }
            return contains(getKeyForListNode(schemaNode, node));
        }

        @Override
        public Node<?> getSingleNode(Modification.ListNodeKey key) {
            checkNotEmpty();
            return Preconditions.checkNotNull(mappedListNodes.get(key), "No value mapped to key %s", key);
        }

        @Override
        public Node<?> getSingleNode(Node<?> node) throws DataModificationException.MissingElementException {
            checkNotEmpty();
            return getSingleNode(getKeyForListNode(schemaNode, node));
        }

        @Override
        public Map<Modification.ListNodeKey, Node<?>> getMappedNodes() {
            checkNotEmpty();
            return ImmutableMap.copyOf(mappedListNodes);
        }

        @Override
        public boolean isEmpty() {
            return mappedListNodes.isEmpty();
        }

        @Override
        public List<Node<?>> getNonEmptyNodes() {
            return Lists.newArrayList(mappedListNodes.values());
        }
    }

    private static class SingleNodeWrapperImpl extends AbstractNodeWrapper implements Modification.SingleNodeWrapper {

        private final Node<?> node;

        public SingleNodeWrapperImpl(Node<?> node) {
            this.node = node;
        }

        @Override
        public boolean isEmpty() {
            return node == null;
        }

        @Override
        public Node<?> getSingleNode() {
            checkNotEmpty();
            return node;
        }

        @Override
        public List<Node<?>> getNonEmptyNodes() {
            return Lists.<Node<?>>newArrayList(node);
        }
    }

    private static class LeafListNodeWrapperImpl extends AbstractNodeWrapper implements Modification.LeafListNodeWrapper {

        private final List<Node<?>> nodes;
        private final Set<Node<?>> uniqueNodes;

        public LeafListNodeWrapperImpl(List<Node<?>> nodes, Set<Node<?>> uniqueNodes) {
            this.nodes = nodes;
            this.uniqueNodes = uniqueNodes;
        }

        @Override
        public List<Node<?>> getNonEmptyNodes() {
            return nodes;
        }

        @Override
        public boolean isEmpty() {
            return nodes == null || nodes.isEmpty();
        }

        @Override
        public boolean contains(Node<?> leafListModification) {
            if(isEmpty()) {
                return false;
            }
            return uniqueNodes.contains(leafListModification);
        }
    }
}