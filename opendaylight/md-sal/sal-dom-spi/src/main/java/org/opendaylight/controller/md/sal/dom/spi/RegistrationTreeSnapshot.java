/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.locks.Lock;

/**
 * A stable read-only snapshot of a {@link AbstractRegistrationTree}.
 *
 * @author Robert Varga
 */
public final class RegistrationTreeSnapshot<T> implements AutoCloseable {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<RegistrationTreeSnapshot> CLOSED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(RegistrationTreeSnapshot.class, "closed");
    private final RegistrationTreeNode<T> node;
    private final Lock lock;

    // Used via CLOSED_UPDATER
    @SuppressWarnings("unused")
    private volatile int closed = 0;

    RegistrationTreeSnapshot(final Lock lock, final RegistrationTreeNode<T> node) {
        this.lock = Preconditions.checkNotNull(lock);
        this.node = Preconditions.checkNotNull(node);
    }

    public RegistrationTreeNode<T> getRootNode() {
        return node;
    }

    @Override
    public void close() {
        if (CLOSED_UPDATER.compareAndSet(this, 0, 1)) {
            lock.unlock();
        }
    }
}
