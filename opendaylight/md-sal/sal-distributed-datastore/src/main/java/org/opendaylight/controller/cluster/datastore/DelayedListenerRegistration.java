/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class DelayedListenerRegistration implements
    ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> {

    private volatile boolean closed;

    private final RegisterChangeListener registerChangeListener;

    private volatile ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                                         NormalizedNode<?, ?>>> delegate;

    DelayedListenerRegistration(final RegisterChangeListener registerChangeListener) {
        this.registerChangeListener = registerChangeListener;
    }

    void setDelegate( final ListenerRegistration<AsyncDataChangeListener<YangInstanceIdentifier,
                                        NormalizedNode<?, ?>>> registration) {
        this.delegate = registration;
    }

    boolean isClosed() {
        return closed;
    }

    RegisterChangeListener getRegisterChangeListener() {
        return registerChangeListener;
    }

    @Override
    public AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> getInstance() {
        return delegate != null ? delegate.getInstance() : null;
    }

    @Override
    public void close() {
        closed = true;
        if(delegate != null) {
            delegate.close();
        }
    }
}