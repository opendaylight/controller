/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.DataChangeListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of listeners organized as a tree by node to which they listen. This class
 * allows for efficient lookup of listeners when we walk the DataTreeCandidate.
 *
 * @author Robert Varga
 */
public final class ListenerTree  {
    private static final Logger LOG = LoggerFactory.getLogger(ListenerTree.class);
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final ListenerNode rootNode = new ListenerNode(null, null);

    private ListenerTree() {
        // Private to disallow direct instantiation
    }

    /**
     * Create a new empty instance of the listener tree.
     *
     * @return An empty instance.
     */
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
    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> DataChangeListenerRegistration<L> registerDataChangeListener(final YangInstanceIdentifier path,
            final L listener, final DataChangeScope scope) {

        // Take the write lock
        rwLock.writeLock().lock();

        try {
            ListenerNode walkNode = rootNode;
            for (final PathArgument arg : path.getPathArguments()) {
                walkNode = walkNode.ensureChild(arg);
            }

            final ListenerNode node = walkNode;
            DataChangeListenerRegistration<L> reg = new DataChangeListenerRegistrationImpl<L>(listener) {
                @Override
                public DataChangeScope getScope() {
                    return scope;
                }

                @Override
                public YangInstanceIdentifier getPath() {
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

    /**
     * Obtain a tree walking context. This context ensures a consistent view of
     * the listener registrations. The context should be closed as soon as it
     * is not required, because each unclosed instance blocks modification of
     * the listener tree.
     *
     * @return A walker instance.
     */
    public ListenerWalker getWalker() {
        /*
         * TODO: The only current user of this method is local to the datastore.
         *       Since this class represents a read-lock, losing a reference to
         *       it is a _major_ problem, as the registration process will get
         *       wedged, eventually grinding the system to a halt. Should an
         *       external user exist, make the Walker a phantom reference, which
         *       will cleanup the lock if not told to do so.
         */
        final ListenerWalker ret = new ListenerWalker(rwLock.readLock(), rootNode);
        rwLock.readLock().lock();
        return ret;
    }

    abstract static class DataChangeListenerRegistrationImpl<T extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> extends AbstractListenerRegistration<T> //
    implements DataChangeListenerRegistration<T> {
        public DataChangeListenerRegistrationImpl(final T listener) {
            super(listener);
        }
    }
}
