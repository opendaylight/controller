/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;


public interface DOMMountPointService extends BrokerService {

    Optional<DOMMountPoint> getMountPoint(YangInstanceIdentifier path);

    DOMMountPointBuilder createMountPoint(YangInstanceIdentifier path);

    ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener);

    interface DOMMountPointBuilder {

        <T extends DOMService> DOMMountPointBuilder addService(Class<T> type,T impl);

        DOMMountPointBuilder addInitialSchemaContext(SchemaContext ctx);

        ObjectRegistration<DOMMountPoint> register();
    }
}
