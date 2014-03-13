/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * Client-level interface for interacting with mount points. It provides access
 * to {@link MountInstance} instances based on their path.
 */
public interface MountService extends BrokerService {
    /**
     * Obtain access to a mount instance registered at the specified path.
     *
     * @param path Path at which the instance is registered
     * @return Reference to the instance, or null if no such instance exists.
     */
    MountInstance getMountPoint(InstanceIdentifier path);
}
