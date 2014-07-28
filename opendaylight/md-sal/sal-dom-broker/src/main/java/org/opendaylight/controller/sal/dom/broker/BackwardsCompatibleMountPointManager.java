/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.sal.dom.broker;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class BackwardsCompatibleMountPointManager implements MountProvisionService, MountProvisionListener {

    private final ListenerRegistry<MountProvisionListener> listeners = ListenerRegistry.create();
    private final ConcurrentMap<InstanceIdentifier, MountProvisionInstance> mounts = new ConcurrentHashMap<>();

    private final DOMMountPointService domMountPointService;

    public BackwardsCompatibleMountPointManager(final DOMMountPointService domMountPointService) {
        this.domMountPointService = domMountPointService;
    }

    @Override
    public MountProvisionInstance createMountPoint(final InstanceIdentifier path) {
        checkState(!mounts.containsKey(path), "Mount already created");
        // Create mount point instance, wrap instance of new API with BackwardsCompatibleMountPoint to preserve backwards comatibility
        final BackwardsCompatibleMountPoint mount = new BackwardsCompatibleMountPoint(path, domMountPointService.createMountPoint(path));
        mounts.put(path, mount);
        return mount;
    }

    public void notifyMountCreated(final InstanceIdentifier identifier) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners.getListeners()) {
            listener.getInstance().onMountPointCreated(identifier);
        }
    }

    public void notifyMountRemoved(final InstanceIdentifier identifier) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners.getListeners()) {
            listener.getInstance().onMountPointRemoved(identifier);
        }
    }

    @Override
    public MountProvisionInstance createOrGetMountPoint(
            final InstanceIdentifier path) {
        final MountProvisionInstance mount = getMountPoint(path);
        if (mount == null) {
            return createMountPoint(path);
        }
        return mount;
    }

    @Override
    public MountProvisionInstance getMountPoint(final InstanceIdentifier path) {
        // If the mount point was created here, return directly
        if(mounts.containsKey(path)) {
            return mounts.get(path);
        }

        // If mount was created in underlying DOMMountService, wrap as MountProvisionInstance
        final Optional<DOMMountPoint> mount = domMountPointService.getMountPoint(path);
        if(mount.isPresent()) {
            return new BackwardsCompatibleMountPoint(path, mount.get());
        } else {
            return null;
        }
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(
            final MountProvisionListener listener) {
        return domMountPointService.registerProvisionListener(listener);
    }

    @Override
    public void onMountPointCreated(final InstanceIdentifier path) {
        notifyMountCreated(path);
    }

    @Override
    public void onMountPointRemoved(final InstanceIdentifier path) {
            notifyMountRemoved(path);
    }
}
