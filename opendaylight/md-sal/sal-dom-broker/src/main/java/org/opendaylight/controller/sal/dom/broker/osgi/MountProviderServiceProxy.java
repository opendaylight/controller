/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class MountProviderServiceProxy extends AbstractBrokerServiceProxy<MountProvisionService> implements MountProvisionService{


    public MountProviderServiceProxy(ServiceReference<MountProvisionService> ref, MountProvisionService delegate) {
        super(ref, delegate);
    }

    @Override
    public MountProvisionInstance getMountPoint(InstanceIdentifier path) {
        return getDelegate().getMountPoint(path);
    }

    @Override
    public MountProvisionInstance createMountPoint(InstanceIdentifier path) {
        return getDelegate().createMountPoint(path);
    }

    @Override
    public MountProvisionInstance createOrGetMountPoint(InstanceIdentifier path) {
        return getDelegate().createOrGetMountPoint(path);
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener) {
        return getDelegate().registerProvisionListener(listener);
    }
}
