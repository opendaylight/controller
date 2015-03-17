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
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Intermediate proxy registration returned to the user when we cannot
 * instantiate the registration immediately. It provides a bridge to
 * a real registration which may materialize at some point in the future.
 */
final class DelayedDataTreeListenerRegistration implements ListenerRegistration<DOMDataTreeChangeListener> {
    private final RegisterDataTreeChangeListener registerTreeChangeListener;
    private volatile ListenerRegistration<DOMDataTreeChangeListener> delegate;
    @GuardedBy("this")
    private boolean closed;

    DelayedDataTreeListenerRegistration(final RegisterDataTreeChangeListener registerTreeChangeListener) {
        this.registerTreeChangeListener = Preconditions.checkNotNull(registerTreeChangeListener);
    }

    synchronized void createDelegate(final DelegateFactory<RegisterDataTreeChangeListener, ListenerRegistration<DOMDataTreeChangeListener>> factory) {
        if (!closed) {
            this.delegate = factory.createDelegate(registerTreeChangeListener);
        }
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

