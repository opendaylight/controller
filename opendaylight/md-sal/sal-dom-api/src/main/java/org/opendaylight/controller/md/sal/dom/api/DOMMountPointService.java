/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Optional;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Deprecated.
 *
 * @deprecated Use {@link org.opendaylight.mdsal.dom.api.DOMMountPointService} instead
 */
@Deprecated
public interface DOMMountPointService extends DOMService {

    Optional<DOMMountPoint> getMountPoint(YangInstanceIdentifier path);

    DOMMountPointBuilder createMountPoint(YangInstanceIdentifier path);

    ListenerRegistration<DOMMountPointListener> registerProvisionListener(DOMMountPointListener listener);

    interface DOMMountPointBuilder {

        <T extends DOMService> DOMMountPointBuilder addService(Class<T> type,T impl);

        DOMMountPointBuilder addInitialSchemaContext(SchemaContext ctx);

        ObjectRegistration<DOMMountPoint> register();
    }
}
