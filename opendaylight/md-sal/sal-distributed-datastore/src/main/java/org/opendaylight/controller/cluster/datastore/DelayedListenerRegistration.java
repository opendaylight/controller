/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import java.util.EventListener;
import java.util.Map.Entry;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

abstract class DelayedListenerRegistration<L extends EventListener, R> implements ListenerRegistration<L> {
    private final R registrationMessage;
    private volatile ListenerRegistration<L> delegate;

    @GuardedBy("this")
    private boolean closed;

    protected DelayedListenerRegistration(R registrationMessage) {
        this.registrationMessage = registrationMessage;
    }

    R getRegistrationMessage() {
        return registrationMessage;
    }

    ListenerRegistration<L> getDelegate() {
        return delegate;
    }

    synchronized <LR extends ListenerRegistration<L>> void createDelegate(
            final LeaderLocalDelegateFactory<R, LR, Optional<DataTreeCandidate>> factory) {
        if (!closed) {
            final Entry<LR, Optional<DataTreeCandidate>> res = factory.createDelegate(registrationMessage);
            this.delegate = res.getKey();
        }
    }

    @Override
    public L getInstance() {
        final ListenerRegistration<L> d = delegate;
        return d == null ? null : (L)d.getInstance();
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
