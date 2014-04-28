/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.mount;

import java.util.EventListener;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface MountProvisionService extends MountService {

    @Override
    public MountProvisionInstance getMountPoint(InstanceIdentifier path);

    MountProvisionInstance createMountPoint(InstanceIdentifier path);

    MountProvisionInstance createOrGetMountPoint(InstanceIdentifier path);

    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);

    public interface MountProvisionListener extends EventListener {

        void onMountPointCreated(InstanceIdentifier path);

        void onMountPointRemoved(InstanceIdentifier path);

    }
}
