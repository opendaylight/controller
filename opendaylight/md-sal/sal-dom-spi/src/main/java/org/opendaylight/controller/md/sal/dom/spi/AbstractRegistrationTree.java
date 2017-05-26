/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * An abstract tree of registrations. Allows a read-only snapshot to be taken.
 *
 * @param <T> Type of registered object
 */
public abstract class AbstractRegistrationTree<T> {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final RegistrationTreeNode<T> rootNode = new RegistrationTreeNode<>(null, null);

    protected AbstractRegistrationTree() {

    }

    /**
     * Acquire the read-write lock. This should be done before invoking {@link #findNodeFor(Iterable)}.
     */
    protected final void takeLock() {
        rwLock.writeLock().lock();
    }

    /**
     * Release the read-write lock. This should be done after invocation of {@link #findNodeFor(Iterable)}
     * and modification of the returned node. Note that callers should do so in a finally block.
     */
    protected final void releaseLock() {
        rwLock.writeLock().unlock();
    }

    /**
     * Find an existing, or allocate a fresh, node for a particular path. Must be called with the
     * read-write lock held.
     *
     * @param path Path to find a node for
     * @return A registration node for the specified path
     */
    @Nonnull protected final RegistrationTreeNode<T> findNodeFor(@Nonnull final Iterable<PathArgument> path) {
        RegistrationTreeNode<T> walkNode = rootNode;
        for (final PathArgument arg : path) {
            walkNode = walkNode.ensureChild(arg);
        }

        return walkNode;
    }

    /**
     * Add a registration to a particular node. The node must have been returned via {@link #findNodeFor(Iterable)}
     * and the lock must still be held.
     *
     * @param node Tree node
     * @param registration Registration instance
     */
    protected final void addRegistration(@Nonnull final RegistrationTreeNode<T> node, @Nonnull final T registration) {
        node.addRegistration(registration);
    }

    /**
     * Remove a registration from a particular node. This method must not be called while the read-write lock
     * is held.
     *
     * @param node Tree node
     * @param registration Registration instance
     */
    protected final void removeRegistration(@Nonnull final RegistrationTreeNode<T> node, @Nonnull final T registration) {
        // Take the write lock
        rwLock.writeLock().lock();
        try {
            node.removeRegistration(registration);
        } finally {
            // Always release the lock
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Obtain a tree snapshot. This snapshot ensures a consistent view of
     * registrations. The snapshot should be closed as soon as it is not required,
     * because each unclosed instance blocks modification of this tree.
     *
     * @return A snapshot instance.
     */
    @Nonnull public final RegistrationTreeSnapshot<T> takeSnapshot() {
        final RegistrationTreeSnapshot<T> ret = new RegistrationTreeSnapshot<>(rwLock.readLock(), rootNode);
        rwLock.readLock().lock();
        return ret;
    }
}
