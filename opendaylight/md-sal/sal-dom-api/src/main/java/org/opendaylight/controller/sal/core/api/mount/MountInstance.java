/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.mount;

import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface representing a single mount instance and represents a way for
 * clients to access underlying data, RPCs and notifications.
 */
public interface MountInstance extends //
        NotificationService, //
        DataBrokerService {

    /**
     * Invoke an RPC on the system underlying the mount instance.
     *
     * @param type RPC type
     * @param input RPC input arguments
     * @return Future representing execution of the RPC.
     */
    ListenableFuture<RpcResult<CompositeNode>> rpc(QName type, CompositeNode input);

    /**
     * Get {@link SchemaContext} of the system underlying the mount instance.
     *
     * @return A schema context.
     */
    SchemaContext getSchemaContext();
}
