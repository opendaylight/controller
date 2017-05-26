/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.base.Optional;
import java.util.EventListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface MountPointService extends BindingService {

    Optional<MountPoint> getMountPoint(InstanceIdentifier<?> mountPoint);

    <T extends MountPointListener> ListenerRegistration<T> registerListener(InstanceIdentifier<?> path, T listener);


    interface MountPointListener extends EventListener {

        void onMountPointCreated(InstanceIdentifier<?> path);

        void onMountPointRemoved(InstanceIdentifier<?> path);

    }

}
