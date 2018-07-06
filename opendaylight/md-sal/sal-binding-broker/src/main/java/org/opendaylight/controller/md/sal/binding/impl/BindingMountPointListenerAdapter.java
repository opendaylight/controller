/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.MountPointService.MountPointListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BindingMountPointListenerAdapter<T extends MountPointListener>
        implements ListenerRegistration<T>, org.opendaylight.mdsal.binding.api.MountPointService.MountPointListener {

    private final T listener;
    private final ListenerRegistration<?> registration;

    BindingMountPointListenerAdapter(final InstanceIdentifier<?> path, final T listener,
            final org.opendaylight.mdsal.binding.api.MountPointService delegate) {
        this.listener = listener;
        this.registration = delegate.registerListener(path, this);
    }

    @Override
    public T getInstance() {
        return listener;
    }

    @Override
    public void close() {
        registration.close();
    }

    @Override
    public void onMountPointCreated(InstanceIdentifier<?> path) {
        listener.onMountPointCreated(path);
    }

    @Override
    public void onMountPointRemoved(InstanceIdentifier<?> path) {
        listener.onMountPointRemoved(path);
    }
}
