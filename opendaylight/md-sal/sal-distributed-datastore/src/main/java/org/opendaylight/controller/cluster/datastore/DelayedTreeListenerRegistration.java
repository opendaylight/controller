/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.messages.RegisterTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Intermediate proxy registration returned to the user when we cannot
 * instantiate the registration immediately. It provides a bridge to
 * a real registration which may materialize at some point in the future.
 */
final class DelayedTreeListenerRegistration implements ListenerRegistration<DOMDataTreeChangeListener> {
    private final RegisterTreeChangeListener registerTreeChangeListener;
    private volatile ListenerRegistration<DOMDataTreeChangeListener> delegate;
    @GuardedBy("this")
    private boolean closed;

    DelayedTreeListenerRegistration(RegisterTreeChangeListener registerTreeChangeListener) {
        this.registerTreeChangeListener = Preconditions.checkNotNull(registerTreeChangeListener);
    }

    @GuardedBy("this")
    void setDelegate(final ListenerRegistration<DOMDataTreeChangeListener> delegate) {
        // The caller (in this package) is responsible for synchronously check close
        // status and not call this.
        this.delegate = delegate;
    }

    @GuardedBy("this")
    boolean isClosed() {
        return closed;
    }

    RegisterTreeChangeListener getRegisterTreeChangeListener() {
        return registerTreeChangeListener;
    }

    @Override
    public DOMDataTreeChangeListener getInstance() {
        final ListenerRegistration<DOMDataTreeChangeListener> d = delegate;
        return d == null ? null : d.getInstance();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            if (delegate != null) {
                delegate.close();
            }
        }
    }
}

