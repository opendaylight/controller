/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import com.google.common.base.Optional;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree.DataChangeListenerRegistrationImpl;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.StoreTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a single node within the listener tree. Note that the data returned from
 * and instance of this class is guaranteed to have any relevance or consistency
 * only as long as the {@link ListenerWalker} instance through which it is reached remains
 * unclosed.
 *
 * @author Robert Varga
 */
public class ListenerNode implements StoreTreeNode<ListenerNode>, Identifiable<PathArgument> {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerNode.class);

    private final Collection<DataChangeListenerRegistration<?>> listeners = new ArrayList<>();
    private final Map<PathArgument, ListenerNode> children = new HashMap<>();
    private final PathArgument identifier;
    private final Reference<ListenerNode> parent;

    ListenerNode(final ListenerNode parent, final PathArgument identifier) {
        this.parent = new WeakReference<>(parent);
        this.identifier = identifier;
    }

    @Override
    public PathArgument getIdentifier() {
        return identifier;
    }

    @Override
    public Optional<ListenerNode> getChild(final PathArgument child) {
        return Optional.fromNullable(children.get(child));
    }

    /**
     * Return the list of current listeners. This collection is guaranteed
     * to be immutable only while the walker, through which this node is
     * reachable remains unclosed.
     *
     * @return the list of current listeners
     */
    public Collection<DataChangeListenerRegistration<?>> getListeners() {
        return listeners;
    }

    ListenerNode ensureChild(final PathArgument child) {
        ListenerNode potential = children.get(child);
        if (potential == null) {
            potential = new ListenerNode(this, child);
            children.put(child, potential);
        }
        return potential;
    }

    void addListener(final DataChangeListenerRegistration<?> listener) {
        listeners.add(listener);
        LOG.debug("Listener {} registered", listener);
    }

    void removeListener(final DataChangeListenerRegistrationImpl<?> listener) {
        listeners.remove(listener);
        LOG.debug("Listener {} unregistered", listener);

        // We have been called with the write-lock held, so we can perform some cleanup.
        removeThisIfUnused();
    }

    private void removeThisIfUnused() {
        final ListenerNode p = parent.get();
        if (p != null && listeners.isEmpty() && children.isEmpty()) {
            p.removeChild(identifier);
        }
    }

    private void removeChild(final PathArgument arg) {
        children.remove(arg);
        removeThisIfUnused();
    }

    @Override
    public String toString() {
        return "Node [identifier=" + identifier + ", listeners=" + listeners.size() + ", children=" + children.size() + "]";
    }
}
