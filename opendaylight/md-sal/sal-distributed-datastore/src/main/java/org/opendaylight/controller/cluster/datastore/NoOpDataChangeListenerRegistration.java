/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * When a consumer registers a data change listener and no local shard is
 * available to register that listener with then we return an instance of
 * NoOpDataChangeListenerRegistration
 *
 * <p>
 *
 * The NoOpDataChangeListenerRegistration as it's name suggests does
 * nothing when an operation is invoked on it
 */
public class NoOpDataChangeListenerRegistration
    implements ListenerRegistration {

    private final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>
        listener;

    public <L extends AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>>> NoOpDataChangeListenerRegistration(
        AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> listener) {

        this.listener = listener;
    }

    @Override
    public AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> getInstance() {
        return listener;
    }

    @Override public void close() {

    }
}
