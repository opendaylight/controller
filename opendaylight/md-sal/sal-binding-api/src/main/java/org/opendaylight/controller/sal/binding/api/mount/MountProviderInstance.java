/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.mount;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;

/**
 * Provider's version of Mount Point, this version allows access to MD-SAL
 * services specific for this mountpoint and registration / provision of
 * interfaces for mount point.
 *
 * @author ttkacik
 *
 */
public interface MountProviderInstance //
        extends //
        MountInstance, //
        DataProviderService, //
        RpcProviderRegistry, //
        NotificationProviderService {

}
