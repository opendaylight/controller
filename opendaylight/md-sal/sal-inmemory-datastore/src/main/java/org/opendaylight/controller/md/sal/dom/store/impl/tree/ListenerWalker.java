/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.Lock;

/**
 * A walking context, pretty much equivalent to an iterator, but it
 * exposes the underlying tree structure.
 *
 * @author Robert Varga
 */
public class ListenerWalker implements AutoCloseable {
    private static final AtomicIntegerFieldUpdater<ListenerWalker> CLOSED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ListenerWalker.class, "closed");
    private final Lock lock;
    private final ListenerNode node;

    // Used via CLOSED_UPDATER
    @SuppressWarnings("unused")
    private volatile int closed = 0;

    ListenerWalker(final Lock lock, final ListenerNode node) {
        this.lock = Preconditions.checkNotNull(lock);
        this.node = Preconditions.checkNotNull(node);
    }

    public ListenerNode getRootNode() {
        return node;
    }

    @Override
    public void close() {
        if (CLOSED_UPDATER.compareAndSet(this, 0, 1)) {
            lock.unlock();
        }
    }
}