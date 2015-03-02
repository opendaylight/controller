/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.rpc.impl;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public interface RpcExecutor {

    /**
     * @deprecated method will be removed in Lithium release
     */
    @Deprecated
    RpcResult<CompositeNode> invokeRpc(CompositeNode rpcRequest);

    RpcDefinition getRpcDefinition();
}