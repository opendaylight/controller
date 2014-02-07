/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface MountProvisionInstance extends //
        MountInstance,//
        NotificationPublishService, //
        RpcProvisionRegistry,//
        DataProviderService {

    void setSchemaContext(SchemaContext optional);

}
