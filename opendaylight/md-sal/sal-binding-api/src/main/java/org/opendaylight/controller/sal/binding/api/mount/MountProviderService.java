/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.mount;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provider MountProviderService, this version allows access to MD-SAL services
 * specific for this mountpoint and registration / provision of interfaces for
 * mount point.
 *
 * @author ttkacik
 *
 */
public interface MountProviderService extends MountService {

    @Override
    MountProviderInstance getMountPoint(InstanceIdentifier<?> path);

    MountProviderInstance createMountPoint(InstanceIdentifier<?> path);

    MountProviderInstance createOrGetMountPoint(InstanceIdentifier<?> path);

    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);

    interface MountProvisionListener extends EventListener {

        void onMountPointCreated(InstanceIdentifier<?> path);

        void onMountPointRemoved(InstanceIdentifier<?> path);

    }
}
