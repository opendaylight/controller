/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Deprecated
public class MountPointManagerImpl implements MountProvisionService {

    private final ListenerRegistry<MountProvisionListener> listeners =
            ListenerRegistry.create();
    private final ConcurrentMap<YangInstanceIdentifier, MountPointImpl> mounts =
            new ConcurrentHashMap<>();
    private DataProviderService dataBroker = null;

    @Override
    public MountProvisionInstance createMountPoint(final YangInstanceIdentifier path) {
        checkState(!mounts.containsKey(path), "Mount already created");
        final MountPointImpl mount = new MountPointImpl(path);
        registerMountPoint(mount);
        mounts.put(path, mount);
        notifyMountCreated(path);
        return mount;
    }

    public void notifyMountCreated(final YangInstanceIdentifier identifier) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners
                .getListeners()) {
            listener.getInstance().onMountPointCreated(identifier);
        }
    }

    public Object registerMountPoint(final MountPointImpl impl) {
        // FIXME: Why is thie commented out? Either we need it or we don't
        // dataBroker?.registerConfigurationReader(impl.mountPath,impl.readWrapper);
        // dataBroker?.registerOperationalReader(impl.mountPath,impl.readWrapper);
        return null;
    }

    @Override
    public MountProvisionInstance createOrGetMountPoint(
            final YangInstanceIdentifier path) {
        final MountPointImpl mount = mounts.get(path);
        if (mount == null) {
            return createMountPoint(path);
        }
        return mount;
    }

    @Override
    public MountProvisionInstance getMountPoint(final YangInstanceIdentifier path) {
        return mounts.get(path);
    }

    /**
     * @return the dataBroker
     */
    public DataProviderService getDataBroker() {
        return dataBroker;
    }

    /**
     * @param dataBroker
     *            the dataBroker to set
     */
    public void setDataBroker(final DataProviderService dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(
            final MountProvisionListener listener) {
        return listeners.register(listener);
    }
}
