/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.dom.broker.osgi;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class DOMMountPointServiceProxy extends AbstractBrokerServiceProxy<DOMMountPointService> implements DOMMountPointService{


    public DOMMountPointServiceProxy(final ServiceReference<DOMMountPointService> ref, final DOMMountPointService delegate) {
        super(ref, delegate);
    }

    @Override
    public Optional<DOMMountPoint> getMountPoint(final YangInstanceIdentifier path) {
        return getDelegate().getMountPoint(path);
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier path) {
        return getDelegate().createMountPoint(path);
    }

    public ListenerRegistration<MountProvisionListener> registerProvisionListener(final MountProvisionListener listener) {
        return getDelegate().registerProvisionListener(listener);
    }
}
