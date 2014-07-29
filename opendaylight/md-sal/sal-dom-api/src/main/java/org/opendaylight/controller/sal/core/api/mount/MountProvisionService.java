/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * @deprecated Use org.opendaylight.controller.md.sal.dom.api.DOMMountPointService instead
 */
@Deprecated
public interface MountProvisionService extends MountService {

    @Override
    public MountProvisionInstance getMountPoint(YangInstanceIdentifier path);

    MountProvisionInstance createMountPoint(YangInstanceIdentifier path);

    MountProvisionInstance createOrGetMountPoint(YangInstanceIdentifier path);

    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);

}
