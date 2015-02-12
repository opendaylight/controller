/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent.Builder;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

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
    private final Collection<Builder> inheritedOne;
    private final YangInstanceIdentifier nodeId;
    private final Collection<ListenerNode> nodes;

    private final Map<DataChangeListenerRegistration<?>, Builder> subBuilders;
    private final Map<DataChangeListenerRegistration<?>, Builder> oneBuilders;
    private final Map<DataChangeListenerRegistration<?>, Builder> baseBuilders;

    private ResolveDataChangeState(final YangInstanceIdentifier nodeId,
            final Iterable<Builder> inheritedSub, final Collection<Builder> inheritedOne,
            final Collection<ListenerNode> nodes) {
        this.nodeId = Preconditions.checkNotNull(nodeId);
        this.nodes = Preconditions.checkNotNull(nodes);
        this.inheritedSub = Preconditions.checkNotNull(inheritedSub);
        this.inheritedOne = Preconditions.checkNotNull(inheritedOne);

        /*
         * Collect the nodes which need to be propagated from us to the child.
         */
        final Map<DataChangeListenerRegistration<?>, Builder> sub = new HashMap<>();
        final Map<DataChangeListenerRegistration<?>, Builder> one = new HashMap<>();
        final Map<DataChangeListenerRegistration<?>, Builder> base = new HashMap<>();
        for (ListenerNode n : nodes) {
            for (DataChangeListenerRegistration<?> l : n.getListeners()) {
                final Builder b = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE);
                switch (l.getScope()) {
                case BASE:
                    base.put(l, b);
                    break;
                case ONE:
                    one.put(l, b);
                    break;
                case SUBTREE:
                    sub.put(l, b);
                    break;
                }
            }
        }

        baseBuilders = maybeEmpty(base);
        oneBuilders = maybeEmpty(one);
        subBuilders = maybeEmpty(sub);
    }

    private static <K, V> Map<K, V> maybeEmpty(final Map<K, V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return map;
    }

    /**
     * Create an initial state handle at a particular root node.
     *
     * @param rootId root instance identifier
     * @param root root node
     * @return
     */
    public static ResolveDataChangeState initial(final YangInstanceIdentifier rootId, final ListenerNode root) {
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
        /*
         * We instantiate a concatenation only when needed:
         *
         * 1) If our collection is empty, we reuse the parent's. This is typically the case
         *    for intermediate node, which should be the vast majority.
         * 2) If the parent's iterable is a Collection and it is empty, reuse our collection.
         *    This is the case for the first node which defines a subtree listener in a
         *    particular subtree.
         * 3) Concatenate the two collections. This happens when we already have some
         *    subtree listeners and we encounter a node which adds a few more.
         *
         * This allows us to lower number of objects allocated and also
         * speeds up Iterables.isEmpty() in needsProcessing().
         *
         * Note that the check for Collection in 2) relies on precisely this logic, which
         * ensures that we simply cannot see an empty concatenation, but rather start off with
         * an empty collection, then switch to a non-empty collection and finally switch to
         * a concatenation. This saves us from instantiating iterators, which a trivial
         * Iterables.isEmpty() would do as soon as we cross case 3).
         */
        final Iterable<Builder> sb;
        if (!subBuilders.isEmpty()) {
            if (inheritedSub instanceof Collection && ((Collection<?>) inheritedSub).isEmpty()) {
                sb = subBuilders.values();
            } else {
                sb = Iterables.concat(inheritedSub, subBuilders.values());
            }
        } else {
            sb = inheritedSub;
        }

        return new ResolveDataChangeState(nodeId.node(childId), sb,
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
        // Have ONE listeners
        if (!inheritedOne.isEmpty()) {
            return true;
        }

        /*
         * Have SUBTREE listeners
         *
         * This is slightly magical replacement for !Iterables.isEmpty(inheritedSub).
         * It relies on the logic in child(), which gives us the guarantee that when
         * inheritedSub is not a Collection, it is guaranteed to be non-empty (which
         * means we need to process). If it is a collection, we still need to check
         * it for emptiness.
         *
         * Unlike Iterables.isEmpty() this code does not instantiate any temporary
         * objects and is thus more efficient.
         */
        if (inheritedSub instanceof Collection) {
            return !((Collection<?>) inheritedSub).isEmpty();
        }

        // Non-Collection => non-empty => have to process
        return true;
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

    private static Collection<ListenerNode> getListenerChildrenWildcarded(final Collection<ListenerNode> parentNodes,
            final PathArgument child) {
        if (parentNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<ListenerNode> result = new ArrayList<>();
        if (child instanceof NodeWithValue || child instanceof NodeIdentifierWithPredicates) {
            NodeIdentifier wildcardedIdentifier = new NodeIdentifier(child.getNodeType());
            addChildNodes(result, parentNodes, wildcardedIdentifier);
        }
        addChildNodes(result, parentNodes, child);
        return result;
    }

    private static void addChildNodes(final List<ListenerNode> result, final Collection<ListenerNode> parentNodes, final PathArgument childIdentifier) {
        for (ListenerNode node : parentNodes) {
            Optional<ListenerNode> child = node.getChild(childIdentifier);
            if (child.isPresent()) {
                result.add(child.get());
            }
        }
    }
}
