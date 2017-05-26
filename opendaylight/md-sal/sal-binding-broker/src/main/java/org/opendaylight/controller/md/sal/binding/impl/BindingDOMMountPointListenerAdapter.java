/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.MountPointService.MountPointListener;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;

final class BindingDOMMountPointListenerAdapter<T extends MountPointListener> implements ListenerRegistration<T>, MountProvisionListener {

    private final T listener;
    private final ListenerRegistration<MountProvisionListener> registration;
    private final BindingToNormalizedNodeCodec codec;

    public BindingDOMMountPointListenerAdapter(final T listener, final BindingToNormalizedNodeCodec codec, final DOMMountPointService mountService) {
        this.listener = listener;
        this.codec = codec;
        this.registration = mountService.registerProvisionListener(this);
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
    public void onMountPointCreated(final YangInstanceIdentifier path) {
        try {
            final InstanceIdentifier<? extends DataObject> bindingPath = toBinding(path);
            listener.onMountPointCreated(bindingPath);
        } catch (final DeserializationException e) {
            BindingDOMMountPointServiceAdapter.LOG.error("Unable to translate mountPoint path {}. Omitting event.",path,e);
        }

    }

    private InstanceIdentifier<? extends DataObject> toBinding(final YangInstanceIdentifier path) throws DeserializationException {
        final Optional<InstanceIdentifier<? extends DataObject>> instanceIdentifierOptional = codec.toBinding(path);
        if(instanceIdentifierOptional.isPresent()) {
            return instanceIdentifierOptional.get();
        } else {
            throw new DeserializationException("Deserialization unsuccessful, " + instanceIdentifierOptional);
        }
    }

    @Override
    public void onMountPointRemoved(final YangInstanceIdentifier path) {
        try {
            final InstanceIdentifier<? extends DataObject> bindingPath = toBinding(path);
            listener.onMountPointRemoved(bindingPath);
        } catch (final DeserializationException e) {
            BindingDOMMountPointServiceAdapter.LOG.error("Unable to translate mountPoint path {}. Omitting event.",path,e);
        }
    }
}