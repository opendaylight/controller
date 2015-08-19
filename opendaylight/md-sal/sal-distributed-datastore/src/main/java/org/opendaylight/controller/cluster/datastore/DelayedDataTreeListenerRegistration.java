/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Map.Entry;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

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

    synchronized void createDelegate(final LeaderLocalDelegateFactory<RegisterDataTreeChangeListener, ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> factory) {
        if (!closed) {
            final Entry<ListenerRegistration<DOMDataTreeChangeListener>, Optional<DataTreeCandidate>> res =
                    factory.createDelegate(registerTreeChangeListener);
            this.delegate = res.getKey();
            factory.getShard().getDataStore().notifyOfInitialData(registerTreeChangeListener.getPath(),
                    this.delegate.getInstance(), res.getValue());
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

