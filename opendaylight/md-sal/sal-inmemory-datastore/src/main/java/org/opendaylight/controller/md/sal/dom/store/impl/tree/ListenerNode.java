/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeNode;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.StoreTreeNode;

/**
 * This is a single node within the listener tree. Note that the data returned from
 * and instance of this class is guaranteed to have any relevance or consistency
 * only as long as the {@link ListenerWalker} instance through which it is reached remains
 * unclosed.
 *
 * @author Robert Varga
 */
public class ListenerNode implements StoreTreeNode<ListenerNode>, Identifiable<PathArgument> {
    final RegistrationTreeNode<DataChangeListenerRegistration<?>> delegate;

    ListenerNode(final RegistrationTreeNode<DataChangeListenerRegistration<?>> delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public PathArgument getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public Optional<ListenerNode> getChild(final PathArgument child) {
        final RegistrationTreeNode<DataChangeListenerRegistration<?>> c = delegate.getExactChild(child);
        if (c == null) {
            return Optional.absent();
        }

        return Optional.of(new ListenerNode(c));
    }

    /**
     * Return the list of current listeners. This collection is guaranteed
     * to be immutable only while the walker, through which this node is
     * reachable remains unclosed.
     *
     * @return the list of current listeners
     */
    public Collection<DataChangeListenerRegistration<?>> getListeners() {
        return delegate.getRegistrations();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
