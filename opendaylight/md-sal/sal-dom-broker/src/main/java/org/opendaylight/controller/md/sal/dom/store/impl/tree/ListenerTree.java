/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public final class ListenerTree {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerTree.class);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final Node rootNode = new Node(null, null);

    private ListenerTree() {

    }

    public static ListenerTree create() {
        return new ListenerTree();
    }

    /**
     * Registers listener on this node.
     *
     * @param path Full path on which listener is registered.
     * @param listener Listener
     * @param scope Scope of triggering event.
     * @return Listener registration
     */
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> registerDataChangeListener(final InstanceIdentifier path,
            final L listener, final DataChangeScope scope) {

        // Take the write lock
        rwLock.writeLock().lock();

        try {
            Node walkNode = rootNode;
            for (final PathArgument arg : path.getPath()) {
                walkNode = walkNode.ensureChild(arg);
            }

            final Node node = walkNode;
            DataChangeListenerRegistration<L> reg = new DataChangeListenerRegistrationImpl<L>(listener) {
                @Override
                public DataChangeScope getScope() {
                    return scope;
                }

                @Override
                public InstanceIdentifier getPath() {
                    return path;
                }

                @Override
                protected void removeRegistration() {
                    /*
                     * TODO: Here's an interesting problem. The way the datastore works, it
                     *       enqueues requests towards the listener, so the listener will be
                     *       notified at some point in the future. Now if the registration is
                     *       closed, we will prevent any new events from being delivered, but
                     *       we have no way to purge that queue.
                     *
                     *       While this does not directly violate the ListenerRegistration
                     *       contract, it is probably not going to be liked by the users.
                     */

                    // Take the write lock
                    ListenerTree.this.rwLock.writeLock().lock();
                    try {
                        node.removeListener(this);
                    } finally {
                        // Always release the lock
                        ListenerTree.this.rwLock.writeLock().unlock();
                    }
                }
            };

            node.addListener(reg);
            return reg;
        } finally {
            // Always release the lock
            rwLock.writeLock().unlock();
        }
    }

    public Walker getWalker() {
        /*
         * TODO: The only current user of this method is local to the datastore.
         *       Since this class represents a read-lock, losing a reference to
         *       it is a _major_ problem, as the registration process will get
         *       wedged, eventually grinding the system to a halt. Should an
         *       external user exist, make the Walker a phantom reference, which
         *       will cleanup the lock if not told to do so.
         */
        final Walker ret = new Walker(rwLock.readLock(), rootNode);
        rwLock.readLock().lock();
        return ret;
    }

    public static final class Walker implements AutoCloseable {
        private final Lock lock;
        private final Node node;

        @GuardedBy("this")
        private boolean valid = true;

        private Walker(final Lock lock, final Node node) {
            this.lock = Preconditions.checkNotNull(lock);
            this.node = Preconditions.checkNotNull(node);
        }

        public Node getRootNode() {
            return node;
        }

        @Override
        public synchronized void close() {
            if (valid) {
                lock.unlock();
                valid = false;
            }
        }
    }

    /**
     * This is a single node within the listener tree. Note that the data returned from
     * and instance of this class is guaranteed to have any relevance or consistency
     * only as long as the {@link Walker} instance through which it is reached remains
     * unclosed.
     */
    public static final class Node implements StoreTreeNode<Node>, Identifiable<PathArgument> {
        private final Collection<DataChangeListenerRegistration<?>> listeners = new ArrayList<>();
        private final Map<PathArgument, Node> children = new HashMap<>();
        private final PathArgument identifier;
        private final Reference<Node> parent;

        private Node(final Node parent, final PathArgument identifier) {
            this.parent = new WeakReference<>(parent);
            this.identifier = identifier;
        }

        @Override
        public PathArgument getIdentifier() {
            return identifier;
        }

        @Override
        public Optional<Node> getChild(final PathArgument child) {
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

        private Node ensureChild(final PathArgument child) {
            Node potential = children.get(child);
            if (potential == null) {
                potential = new Node(this, child);
                children.put(child, potential);
            }
            return potential;
        }

        private void addListener(final DataChangeListenerRegistration<?> listener) {
            listeners.add(listener);
            LOG.debug("Listener {} registered", listener);
        }

        private void removeListener(final DataChangeListenerRegistrationImpl<?> listener) {
            listeners.remove(listener);
            LOG.debug("Listener {} unregistered", listener);

            // We have been called with the write-lock held, so we can perform some cleanup.
            removeThisIfUnused();
        }

        private void removeThisIfUnused() {
            final Node p = parent.get();
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

    private abstract static class DataChangeListenerRegistrationImpl<T extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> extends AbstractListenerRegistration<T> //
    implements DataChangeListenerRegistration<T> {
        public DataChangeListenerRegistrationImpl(final T listener) {
            super(listener);
        }
    }
}
