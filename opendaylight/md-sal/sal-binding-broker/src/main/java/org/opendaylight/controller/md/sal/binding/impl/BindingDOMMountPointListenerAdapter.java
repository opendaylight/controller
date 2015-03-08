package org.opendaylight.controller.md.sal.binding.impl;

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

    public BindingDOMMountPointListenerAdapter(T listener, BindingToNormalizedNodeCodec codec, DOMMountPointService mountService) {
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
    public void onMountPointCreated(YangInstanceIdentifier path) {
        try {
            InstanceIdentifier<? extends DataObject> bindingPath = codec.toBinding(path).get();
            listener.onMountPointCreated(bindingPath);
        } catch (DeserializationException e) {
            BindingDOMMountPointServiceAdapter.LOG.error("Unable to translate mountPoint path {}. Ommiting event.",path,e);
        }

    }

    @Override
    public void onMountPointRemoved(YangInstanceIdentifier path) {
        try {
            InstanceIdentifier<? extends DataObject> bindingPath = codec.toBinding(path).get();
            listener.onMountPointRemoved(bindingPath);
        } catch (DeserializationException e) {
            BindingDOMMountPointServiceAdapter.LOG.error("Unable to translate mountPoint path {}. Ommiting event.",path,e);
        }
    }
}