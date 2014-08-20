/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursion state used in {@link ResolveDataChangeEventsTask}. Instances of this
 * method track which listeners are affected by a particular change node. It takes
 * care of properly inheriting SUB/ONE listeners and also provides a means to
 * understand when actual processing need not occur.
 */
final class ResolveDataChangeState {
    private static final Logger LOG = LoggerFactory.getLogger(ResolveDataChangeState.class);
    /**
     * Inherited from all parents
     */
    private final Iterable<Builder> inheritedSub;
    /**
     * Inherited from immediate parent
     */
    private final Iterable<Builder> inheritedOne;
    private final YangInstanceIdentifier nodeId;
    private final Collection<Node> nodes;

    private final Map<DataChangeListenerRegistration<?>, Builder> subBuilders = new HashMap<>();
    private final Map<DataChangeListenerRegistration<?>, Builder> oneBuilders = new HashMap<>();
    private final Map<DataChangeListenerRegistration<?>, Builder> baseBuilders = new HashMap<>();

    private ResolveDataChangeState(final YangInstanceIdentifier nodeId,
            final Iterable<Builder> inheritedSub, final Iterable<Builder> inheritedOne,
            final Collection<Node> nodes) {
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.nodes = Preconditions.checkNotNull(nodes);
        this.inheritedSub = Preconditions.checkNotNull(inheritedSub);
        this.inheritedOne = Preconditions.checkNotNull(inheritedOne);

        /*
         * Collect the nodes which need to be propagated from us to the child.
         */
        for (Node n : nodes) {
            for (DataChangeListenerRegistration<?> l : n.getListeners()) {
                final Builder b = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE);
                switch (l.getScope()) {
                case BASE:
                    baseBuilders.put(l, b);
                    break;
                case ONE:
                    oneBuilders.put(l, b);
                    break;
                case SUBTREE:
                    subBuilders.put(l, b);
                    break;
                }
            }
        }
    }

    /**
     * Create an initial state handle at a particular root node.
     *
     * @param rootId root instance identifier
     * @param root root node
     * @return
     */
    public static ResolveDataChangeState initial(final YangInstanceIdentifier rootId, final Node root) {
        return new ResolveDataChangeState(rootId, Collections.<Builder>emptyList(),
            Collections.<Builder>emptyList(), Collections.singletonList(root));
    }

    /**
     * Create a state handle for iterating over a particular child.
     *
     * @param childId ID of the child
     * @return State handle
     */
    public ResolveDataChangeState child(final PathArgument childId) {
        return new ResolveDataChangeState(nodeId.node(childId),
            Iterables.concat(inheritedSub, subBuilders.values()),
            oneBuilders.values(), getListenerChildrenWildcarded(nodes, childId));
    }

    /**
     * Get the current path
     *
     * @return Current path.
     */
    public YangInstanceIdentifier getPath() {
        return nodeId;
    }

    /**
     * Check if this child needs processing.
     *
     * @return True if processing needs to occur, false otherwise.
     */
    public boolean needsProcessing() {
        // May have underlying listeners, so we need to process
        if (!nodes.isEmpty()) {
            return true;
        }
        // Have SUBTREE listeners
        if (!Iterables.isEmpty(inheritedSub)) {
            return true;
        }
        // Have ONE listeners
        if (!Iterables.isEmpty(inheritedOne)) {
            return true;
        }

        return false;
    }

    /**
     * Add an event to all current listeners.
     *
     * @param event
     */
    public void addEvent(final DOMImmutableDataChangeEvent event) {
        // Subtree builders get always notified
        for (Builder b : subBuilders.values()) {
            b.merge(event);
        }
        for (Builder b : inheritedSub) {
            b.merge(event);
        }

        if (event.getScope() == DataChangeScope.ONE || event.getScope() == DataChangeScope.BASE) {
            for (Builder b : oneBuilders.values()) {
                b.merge(event);
            }
        }

        if (event.getScope() == DataChangeScope.BASE) {
            for (Builder b : inheritedOne) {
                b.merge(event);
            }
            for (Builder b : baseBuilders.values()) {
                b.merge(event);
            }
        }
    }

    /**
     * Gather all non-empty events into the provided map.
     *
     * @param before before-image
     * @param after after-image
     * @param map target map
     */
    public void collectEvents(final NormalizedNode<?, ?> before, final NormalizedNode<?, ?> after,
            final Multimap<DataChangeListenerRegistration<?>, DOMImmutableDataChangeEvent> map) {
        for (Entry<DataChangeListenerRegistration<?>, Builder> e : baseBuilders.entrySet()) {
            final Builder b = e.getValue();
            if (!b.isEmpty()) {
                map.put(e.getKey(), b.setBefore(before).setAfter(after).build());
            }
        }
        for (Entry<DataChangeListenerRegistration<?>, Builder> e : oneBuilders.entrySet()) {
            final Builder b = e.getValue();
            if (!b.isEmpty()) {
                map.put(e.getKey(), b.setBefore(before).setAfter(after).build());
            }
        }
        for (Entry<DataChangeListenerRegistration<?>, Builder> e : subBuilders.entrySet()) {
            final Builder b = e.getValue();
            if (!b.isEmpty()) {
                map.put(e.getKey(), b.setBefore(before).setAfter(after).build());
            }
        }

        LOG.trace("Collected events {}", map);
    }

    private static Collection<Node> getListenerChildrenWildcarded(final Collection<Node> parentNodes,
            final PathArgument child) {
        if (parentNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Node> result = new ArrayList<>();
        if (child instanceof NodeWithValue || child instanceof NodeIdentifierWithPredicates) {
            NodeIdentifier wildcardedIdentifier = new NodeIdentifier(child.getNodeType());
            addChildNodes(result, parentNodes, wildcardedIdentifier);
        }
        addChildNodes(result, parentNodes, child);
        return result;
    }

    private static void addChildNodes(final List<Node> result, final Collection<Node> parentNodes, final PathArgument childIdentifier) {
        for (Node node : parentNodes) {
            Optional<Node> child = node.getChild(childIdentifier);
            if (child.isPresent()) {
                result.add(child.get());
            }
        }
    }
}
